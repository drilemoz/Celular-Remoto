package br.dev.celularremoto.ui

/** Políticas puras de aparência. Não fazem alterações de permissões ou acesso remoto. */
internal object AppearancePolicy {
    fun isDark(mode: String, systemDark: Boolean): Boolean = when (mode) {
        "dark", "amoled" -> true
        "system" -> systemDark
        else -> false
    }

    fun isAmoled(mode: String) = mode == "amoled"

    /** Evita texto cortado em controles adjacentes quando a fonte é ampliada. */
    fun stackSecondaryActions(widthDp: Int, fontScale: Float): Boolean =
        widthDp < 360 || fontScale > 1.20f

    fun navigationHeightDp(fontScale: Float): Int = if (fontScale > 1.20f) 76 else 60
    fun actionLabelLines(fontScale: Float): Int = if (fontScale > 1.20f) 3 else 2
}
