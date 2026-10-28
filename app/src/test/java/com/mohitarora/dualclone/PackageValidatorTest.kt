package com.mohitarora.dualclone

import com.mohitarora.dualclone.util.PackageValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageValidatorTest {

    @Test
    fun acceptsNormalPackageNames() {
        assertTrue(PackageValidator.isValidPackageName("com.mohitarora.dualclone"))
        assertTrue(PackageValidator.isValidPackageName("com.popularapp.thirtydayfitnesschallenge"))
        assertTrue(PackageValidator.isValidPackageName("com.example.app_2"))
    }

    @Test
    fun rejectsShellInjectionAndMalformedNames() {
        assertFalse(PackageValidator.isValidPackageName("com.example; rm -rf /"))
        assertFalse(PackageValidator.isValidPackageName("com.example app"))
        assertFalse(PackageValidator.isValidPackageName("single"))
        assertFalse(PackageValidator.isValidPackageName("1com.example"))
        assertFalse(PackageValidator.isValidPackageName(""))
    }

    @Test
    fun validatesAppOpNames() {
        assertTrue(PackageValidator.isValidAppOp("MANAGE_EXTERNAL_STORAGE"))
        assertFalse(PackageValidator.isValidAppOp("manage_external_storage"))
        assertFalse(PackageValidator.isValidAppOp("OP; id"))
    }
}
