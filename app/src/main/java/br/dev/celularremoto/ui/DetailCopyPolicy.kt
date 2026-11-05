package br.dev.celularremoto.ui

/** Somente dados recebidos pelo snapshot autenticado, sem novas consultas ou permissões. */
internal object DetailCopyPolicy {
    data class Field(val section: String, val label: String, val value: String)

    private val missing = setOf(
        "não disponível", "não compartilhado", "não informado", "não informada",
        "sem leitura", "sem leitura/permissão", "permissão não concedida",
        "não identificado", "indisponível", "desconhecido"
    )

    fun isCopyable(value: String): Boolean {
        val clean = value.trim()
        return clean.isNotEmpty() && clean.lowercase() !in missing && clean != "null"
    }

    fun report(device: String, updated: String, fields: List<Field>): String = buildString {
        append("Celular Remoto — dados do aparelho")
        if (device.isNotBlank()) append("\nAparelho: ").append(safe(device))
        if (isCopyable(updated)) append("\nÚltima consulta: ").append(safe(updated))
        var lastSection: String? = null
        for (field in fields) {
            if (!isCopyable(field.value)) continue
            if (field.section != lastSection) {
                append("\n\n").append(safe(field.section))
                lastSection = field.section
            }
            append("\n").append(safe(field.label)).append(": ").append(safe(field.value))
        }
    }

    private fun safe(value: String): String = value.trim().replace(Regex("[\r\n\t]+"), " ")
}
