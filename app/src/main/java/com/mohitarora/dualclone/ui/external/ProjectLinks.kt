package com.mohitarora.dualclone.ui.external

import android.net.Uri
import org.json.JSONObject

/** Central URL registry. Screens and sheets never hardcode a URL or the UPI ID. */
object ProjectLinks {
    const val APP_NAME = "DualClone"
    const val REPO_NAME = "DualClone"
    const val APPLICATION_ID = "com.mohitarora.dualclone"

    const val WEBSITE = "https://mohitarora.me"
    const val GITHUB_PROFILE = "https://github.com/Arora-Sir"
    const val LINKEDIN = "https://www.linkedin.com/in/arora-sir"
    const val X = "https://x.com/MohitAr0rA"

    const val PAYPAL = "https://paypal.me/arorasir"
    const val SPONSORS = "https://github.com/sponsors/Arora-Sir"
    const val UPI_ID = "mohit1998arora@yescred"

    const val REPOSITORY = "https://github.com/Arora-Sir/$REPO_NAME"
    const val RELEASES_LATEST = "$REPOSITORY/releases/latest"
    const val NEW_ISSUE = "$REPOSITORY/issues/new/choose"
    const val PRIVACY = "$REPOSITORY/blob/main/PRIVACY.md"
    const val SHIZUKU_FORK = "https://github.com/thedjchi/Shizuku"
    const val SHIZUKU_RELEASES = "https://github.com/RikkaApps/Shizuku/releases/latest"

    val upiPayment: String
        get() = "upi://pay?pa=$UPI_ID&pn=Mohit%20Arora&cu=INR&tn=" + Uri.encode("$APP_NAME Support")

    /** Obtainium one tap subscription. The universal APK needs no ABI filter. */
    val obtainium: String
        get() {
            val settings = "{\"versionExtractionRegEx\":\"v(.+)\",\"matchGroupToUse\":\"\$1\"}"
            val json = "{\"id\":\"$APPLICATION_ID\",\"url\":\"$REPOSITORY\",\"author\":\"Arora-Sir\"," +
                "\"name\":\"$APP_NAME\",\"additionalSettings\":${JSONObject.quote(settings)}}"
            return "https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/" + Uri.encode(json)
        }
}
