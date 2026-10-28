package com.mohitarora.dualclone.update

import java.net.URI

/**
 * The only network destinations DualClone may contact.
 * The API host serves the release list. GitHub serves asset downloads and redirects them to its asset hosts.
 */
object UrlPolicy {
    const val REPO = "Arora-Sir/DualClone"
    const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO/releases/latest"

    private const val API_HOST = "api.github.com"
    private val ASSET_ENTRY_HOSTS = setOf("github.com", API_HOST)
    private val REDIRECT_HOSTS = setOf(
        "github.com",
        API_HOST,
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )
    private val ASSET_PATH_PREFIXES = listOf("/$REPO/releases/download/", "/repos/$REPO/releases/assets/")

    /** True for https URLs on an allowed host, without credentials in the URL. Used for every hop. */
    fun isAllowedHop(url: String): Boolean {
        val uri = parse(url) ?: return false
        return uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
            uri.host?.lowercase() in REDIRECT_HOSTS
    }

    /** Stricter check for the asset URLs named in the release JSON: host and repository path. */
    fun isAllowedAssetEntry(url: String): Boolean {
        val uri = parse(url) ?: return false
        return isAllowedHop(url) && uri.host.lowercase() in ASSET_ENTRY_HOSTS &&
            ASSET_PATH_PREFIXES.any { (uri.rawPath ?: "").startsWith(it) }
    }

    fun resolve(base: String, location: String): String? = try {
        URI(base).resolve(location).toString()
    } catch (_: Exception) {
        null
    }

    private fun parse(url: String): URI? = try {
        URI(url).takeIf { it.host != null }
    } catch (_: Exception) {
        null
    }
}
