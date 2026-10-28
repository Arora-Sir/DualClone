package com.mohitarora.dualclone.domain.backup

import com.mohitarora.dualclone.domain.model.ProfileKind
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupProfile(val userId: Int, val kind: ProfileKind, val clones: List<String>)

data class BackupPreset(val kind: ProfileKind, val userId: Int, val packageName: String, val op: String, val mode: String)

data class BackupTheme(val preset: String, val pureBlack: Boolean, val customColorHex: Long, val mode: String)

data class BackupModel(
    val createdAtMillis: Long,
    val appVersion: String = "",
    val profiles: List<BackupProfile> = emptyList(),
    val appOpsPresets: List<BackupPreset> = emptyList(),
    val theme: BackupTheme? = null,
    val updateSettings: BackupUpdateSettings? = null,
    /** Sections present in the file. Files written before selective export read as everything. */
    val parts: BackupParts = BackupParts.EVERYTHING,
    val schemaVersion: Int = BackupCodec.SCHEMA_VERSION
)

sealed interface BackupReadResult {
    data class Ok(val model: BackupModel) : BackupReadResult
    data object NeedsPassword : BackupReadResult
    data object WrongPasswordOrCorrupt : BackupReadResult
    data class Invalid(val reason: String) : BackupReadResult
}

/**
 * Reads and writes the dualclone-backup-v1 file. Plain files are JSON.
 * Password protected files are a JSON envelope holding AES-256-GCM ciphertext, with the key derived by
 * PBKDF2WithHmacSHA256 at 310000 iterations. The format name is bound into the GCM tag as associated data.
 */
object BackupCodec {
    const val FORMAT = "dualclone-backup-v1"
    const val SCHEMA_VERSION = 1
    const val PBKDF2_ITERATIONS = 310_000
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val MAX_INPUT_BYTES = 5 * 1024 * 1024

    fun write(
        model: BackupModel,
        out: OutputStream,
        password: CharArray? = null,
        random: SecureRandom = SecureRandom(),
        iterations: Int = PBKDF2_ITERATIONS
    ) {
        val plain = MiniJson.write(toMap(model)).toByteArray(Charsets.UTF_8)
        val bytes = if (password == null || password.isEmpty()) {
            plain
        } else {
            val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
            val iv = ByteArray(IV_BYTES).also(random::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(FORMAT.toByteArray(Charsets.UTF_8))
            val enc = Base64.getEncoder()
            MiniJson.write(
                linkedMapOf(
                    "format" to FORMAT,
                    "encrypted" to true,
                    "kdf" to KDF,
                    "iterations" to iterations,
                    "salt" to enc.encodeToString(salt),
                    "iv" to enc.encodeToString(iv),
                    "ciphertext" to enc.encodeToString(cipher.doFinal(plain))
                )
            ).toByteArray(Charsets.UTF_8)
        }
        out.write(bytes)
        out.flush()
    }

    fun read(input: InputStream, password: CharArray? = null): BackupReadResult {
        val raw = input.readBounded() ?: return BackupReadResult.Invalid("File is larger than ${MAX_INPUT_BYTES / 1024} KB")
        val root = try {
            MiniJson.parse(String(raw, Charsets.UTF_8))
        } catch (e: MiniJson.JsonException) {
            return BackupReadResult.Invalid("Not a valid backup file")
        }
        val map = root as? Map<*, *> ?: return BackupReadResult.Invalid("Not a backup file")
        if (map["format"] != FORMAT) return BackupReadResult.Invalid("Unsupported backup format")
        val decoded: Map<*, *> = if (map["encrypted"] == true) {
            if (password == null || password.isEmpty()) return BackupReadResult.NeedsPassword
            val plain = try {
                decrypt(map, password)
            } catch (e: GeneralSecurityException) {
                return BackupReadResult.WrongPasswordOrCorrupt
            } catch (e: IllegalArgumentException) {
                return BackupReadResult.Invalid("Corrupt encrypted backup")
            }
            try {
                MiniJson.parse(String(plain, Charsets.UTF_8)) as? Map<*, *>
            } catch (e: MiniJson.JsonException) {
                null
            } ?: return BackupReadResult.WrongPasswordOrCorrupt
        } else {
            map
        }
        return try {
            BackupReadResult.Ok(fromMap(decoded))
        } catch (e: IllegalArgumentException) {
            BackupReadResult.Invalid(e.message ?: "Malformed backup")
        }
    }

    fun isEncrypted(bytes: ByteArray): Boolean = try {
        (MiniJson.parse(String(bytes, Charsets.UTF_8)) as? Map<*, *>)?.get("encrypted") == true
    } catch (e: MiniJson.JsonException) {
        false
    }

    private fun decrypt(envelope: Map<*, *>, password: CharArray): ByteArray {
        val dec = Base64.getDecoder()
        val iterations = (envelope["iterations"] as? Long)?.toInt() ?: throw IllegalArgumentException("Missing iterations")
        require(iterations in 1..2_000_000) { "Unreasonable iteration count" }
        val salt = dec.decode(envelope["salt"] as? String ?: throw IllegalArgumentException("Missing salt"))
        val iv = dec.decode(envelope["iv"] as? String ?: throw IllegalArgumentException("Missing iv"))
        val ct = dec.decode(envelope["ciphertext"] as? String ?: throw IllegalArgumentException("Missing ciphertext"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(FORMAT.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun InputStream.readBounded(): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val n = read(chunk)
            if (n < 0) break
            buffer.write(chunk, 0, n)
            if (buffer.size() > MAX_INPUT_BYTES) return null
        }
        return buffer.toByteArray()
    }

    private fun toMap(m: BackupModel): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>(
            "format" to FORMAT,
            "schemaVersion" to m.schemaVersion,
            "parts" to m.parts.names(),
            "createdAtMillis" to m.createdAtMillis,
            "appVersion" to m.appVersion
        )
        // NOTE: Sections that were not selected are omitted from the file, not written empty.
        if (m.parts.cloneList) {
            map["profiles"] = m.profiles.map {
                linkedMapOf("userId" to it.userId, "kind" to it.kind.name, "clones" to it.clones)
            }
        }
        if (m.parts.appOpsPresets) {
            map["appOpsPresets"] = m.appOpsPresets.map {
                linkedMapOf(
                    "kind" to it.kind.name, "userId" to it.userId,
                    "packageName" to it.packageName, "op" to it.op, "mode" to it.mode
                )
            }
        }
        if (m.parts.settings) {
            m.theme?.let {
                map["theme"] = linkedMapOf(
                    "preset" to it.preset, "pureBlack" to it.pureBlack,
                    "customColorHex" to it.customColorHex, "mode" to it.mode
                )
            }
            m.updateSettings?.let { map["updateSettings"] = linkedMapOf("dailyCheck" to it.dailyCheck) }
        }
        return map
    }

    private fun fromMap(map: Map<*, *>): BackupModel {
        val version = (map["schemaVersion"] as? Long)?.toInt() ?: throw IllegalArgumentException("Missing schemaVersion")
        require(version in 1..SCHEMA_VERSION) { "Backup was written by a newer version of DualClone" }
        fun kindOf(v: Any?): ProfileKind =
            ProfileKind.entries.firstOrNull { it.name == v } ?: throw IllegalArgumentException("Unknown profile kind")
        fun str(m: Map<*, *>, k: String) = m[k] as? String ?: throw IllegalArgumentException("Missing $k")
        fun int(m: Map<*, *>, k: String) = (m[k] as? Long)?.toInt() ?: throw IllegalArgumentException("Missing $k")
        // NOTE: A file without a parts header is a full backup from before selective export.
        val parts = (map["parts"] as? List<*>)?.let { BackupParts.fromNames(it) } ?: BackupParts.EVERYTHING
        if (!parts.isValid) throw IllegalArgumentException("Backup lists no sections")
        val profiles = (map["profiles"] as? List<*> ?: emptyList<Any?>()).map {
            val p = it as? Map<*, *> ?: throw IllegalArgumentException("Bad profile entry")
            BackupProfile(int(p, "userId"), kindOf(p["kind"]), (p["clones"] as? List<*> ?: emptyList<Any?>()).map { c ->
                c as? String ?: throw IllegalArgumentException("Bad package entry")
            })
        }
        val presets = (map["appOpsPresets"] as? List<*> ?: emptyList<Any?>()).map {
            val p = it as? Map<*, *> ?: throw IllegalArgumentException("Bad preset entry")
            BackupPreset(kindOf(p["kind"]), int(p, "userId"), str(p, "packageName"), str(p, "op"), str(p, "mode"))
        }
        val theme = (map["theme"] as? Map<*, *>)?.let {
            BackupTheme(
                preset = str(it, "preset"),
                pureBlack = it["pureBlack"] as? Boolean ?: false,
                customColorHex = it["customColorHex"] as? Long ?: 0L,
                mode = it["mode"] as? String ?: "SYSTEM"
            )
        }
        return BackupModel(
            createdAtMillis = map["createdAtMillis"] as? Long ?: 0L,
            appVersion = map["appVersion"] as? String ?: "",
            profiles = profiles,
            appOpsPresets = presets,
            theme = theme,
            updateSettings = (map["updateSettings"] as? Map<*, *>)?.let { BackupUpdateSettings(it["dailyCheck"] as? Boolean ?: false) },
            parts = parts,
            schemaVersion = version
        )
    }
}
