package br.dev.celularremoto.updates

/** Regras determinísticas usadas antes de qualquer download. */
object UpdateRules {
    const val REPOSITORY = "drilemoz/Celular-Remoto"
    const val PACKAGE = "br.dev.celularremoto"
    const val MANIFEST_NAME = "celular-remoto-update.json"
    const val MAX_APK_BYTES = 250L * 1024 * 1024
    val VALID_VERSION = Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:-(?:alpha|beta|rc)\\.[1-9][0-9]*)?")
    private val SHA256 = Regex("[0-9a-fA-F]{64}")

    fun apkName(version: String): String? = if (VALID_VERSION.matches(version))
        "CelularRemoto-v$version.apk" else null

    fun isValidRelease(version: String, code: Int, currentCode: Int, sha256: String,
        packageName: String, stableSigned: Boolean, tag: String, assetName: String): Boolean =
        code > currentCode && code > 0 && code <= 2_100_000_000 &&
            VALID_VERSION.matches(version) &&
            (!version.contains("-alpha.") || version.substringAfterLast('.').toIntOrNull() == code) &&
            sha256.matches(SHA256) && packageName == PACKAGE && stableSigned &&
            tag == "v$version" && assetName == apkName(version)

    /** Bloqueia links arbitrários e domínios parecidos com GitHub. */
    fun allowedAssetUrl(url: String, tag: String, assetName: String): Boolean =
        url == "https://github.com/$REPOSITORY/releases/download/$tag/$assetName"
}
