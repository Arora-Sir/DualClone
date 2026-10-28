package com.mohitarora.dualclone.arch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cheap source level layering checks. They fail the build when a rule is broken.
 */
class ArchitectureTest {

    private val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }

    private fun kotlinFiles(subPath: String): List<File> =
        File(root, "com/mohitarora/dualclone/$subPath").takeIf { it.exists() }
            ?.walkTopDown()?.filter { it.extension == "kt" }?.toList().orEmpty()

    private fun imports(file: File): List<String> =
        file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() }

    @Test
    fun uiNeverImportsTheShizukuTransportDirectly() {
        // NOTE: Screens may use ShizukuManager for status and permission and ShizukuDiagnostics for display. Nothing else.
        val allowed = setOf(
            "com.mohitarora.dualclone.shizuku.ShizukuManager",
            "com.mohitarora.dualclone.shizuku.ShizukuDiagnostics"
        )
        val violations = kotlinFiles("ui").flatMap { f ->
            imports(f).filter { it.startsWith("com.mohitarora.dualclone.shizuku.") && it !in allowed }.map { "${f.name}: $it" }
        }
        assertTrue("UI code must go through repositories, found: $violations", violations.isEmpty())
    }

    @Test
    fun uiNeverImportsRikkaShizukuApi() {
        val violations = kotlinFiles("ui").filter { f -> imports(f).any { it.startsWith("rikka.shizuku") } }.map { it.name }
        assertTrue("UI code must not call the Shizuku API: $violations", violations.isEmpty())
    }

    @Test
    fun pureDomainPackagesHaveNoAndroidImports() {
        val violations = listOf("domain/pm", "domain/policy", "domain/backup", "domain/model", "domain/repo").flatMap { dir ->
            kotlinFiles(dir).flatMap { f ->
                imports(f).filter { it.startsWith("android.") || it.startsWith("androidx.") }.map { "${f.name}: $it" }
            }
        }
        assertTrue("Domain code must stay free of Android types: $violations", violations.isEmpty())
    }

    @Test
    fun domainNeverDependsOnDataOrUi() {
        val violations = kotlinFiles("domain").flatMap { f ->
            imports(f).filter {
                it.startsWith("com.mohitarora.dualclone.data.") || it.startsWith("com.mohitarora.dualclone.ui.") ||
                    it.startsWith("com.mohitarora.dualclone.shizuku.")
            }.map { "${f.name}: $it" }
        }
        assertTrue("Domain must not import outer layers: $violations", violations.isEmpty())
    }

    @Test
    fun sourcesKeepThePunctuationInvariant() {
        val banned = listOf(0x2014.toChar().toString(), 0x2013.toChar().toString())
        // NOTE: Scoped to the data layer packages. The UI packages have their own owner and checks.
        val owned = listOf("data", "domain", "shizuku", "work", "receiver", "di", "ui/viewmodel")
        val violations = owned.flatMap { kotlinFiles(it) }.filter { f -> banned.any { f.readText().contains(it) } }.map { it.name }
        assertTrue("Em or en dash found in: $violations", violations.isEmpty())
    }

    @Test
    fun onlyTheUpdatePackageTouchesTheNetwork() {
        // NOTE: Network containment. One package owns all network code, so the privacy claim stays checkable.
        val banned = listOf("java.net.", "javax.net.", "okhttp3", "android.net.http", "org.apache.http")
        val violations = kotlinFiles("").filter { !it.invariantSeparatorsPath.contains("/dualclone/update/") }.flatMap { f ->
            f.readLines().filter { line ->
                val t = line.trim()
                banned.any { b -> t.startsWith("import $b") || t.startsWith("import static $b") }
            }.map { "${f.name}: ${it.trim()}" }
        }
        assertTrue("Only com.mohitarora.dualclone.update may use network APIs: $violations", violations.isEmpty())
    }
}
