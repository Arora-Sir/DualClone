package com.mohitarora.dualclone.util

object PackageValidator {
    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val APPOP_REGEX = Regex("^[A-Z][A-Z0-9_]*$")

    fun isValidPackageName(packageName: String): Boolean {
        return PACKAGE_REGEX.matches(packageName.trim())
    }

    fun isValidAppOp(appOp: String): Boolean {
        return APPOP_REGEX.matches(appOp.trim())
    }
}
