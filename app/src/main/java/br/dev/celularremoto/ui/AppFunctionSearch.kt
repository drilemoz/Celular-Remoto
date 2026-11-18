package br.dev.celularremoto.ui

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal enum class FunctionArea(val title: String) {
    CONTROL("Controlar"), MONITOR("Monitorado"), BATTERY("Bateria"), SETTINGS("Ajustes")
}

internal enum class ControllerTool {
    FILES, ALERTS, APPS, MESSAGES, SCREEN, PHOTO, CAMERAS, SIREN, TORCH, LOCK, POWER, LOCATION
}

internal enum class SearchShortcut {
    CONTROL, MONITOR, PAIR, DEVICE_DETAILS, RENAME_DEVICE, CONNECTION_DIAGNOSTICS, CALLS, MESSAGE_NOTICES,
    SHOW_MONITOR_INFO, GENERATE_QR, PREPARE_PHOTO, ADB
}

internal enum class BatterySearchPage(val tab: String) {
    SUMMARY("resumo"), TESTS("testes"), HISTORY("historico"),
    DIAGNOSTIC("resumo"), GOALS("resumo"), SESSION("resumo")
}

internal sealed interface FunctionDestination {
    data class Shortcut(val target: SearchShortcut) : FunctionDestination
    data class Controller(val tool: ControllerTool) : FunctionDestination
    data class Settings(val section: String, val anchors: List<String> = emptyList()) : FunctionDestination
    data class Battery(val page: BatterySearchPage, val focus: String? = null, val remote: Boolean? = null) : FunctionDestination
    data class MonitorRow(val anchors: List<String>) : FunctionDestination
}

internal data class AppFunction(
    val id: String,
    val area: FunctionArea,
    val title: String,
    val path: String,
    val aliases: String,
    val destination: FunctionDestination,
)

/** Índice local de nomes de funções, não de dados de usuário. */
internal class AppFunctionSearch(entries: List<AppFunction>) {
    private data class Document(
        val entry: AppFunction, val title: String, val path: String, val aliases: String,
        val titleWords: Set<String>, val aliasWords: Set<String>, val pathWords: Set<String>,
    )

    private val documents = entries.map { entry ->
        val title = fold(entry.title)
        val path = fold("${entry.area.title} ${entry.path}")
        val aliases = fold(entry.aliases)
        Document(entry, title, path, aliases, words(title), words(aliases), words(path))
    }

    /** Todas as palavras relevantes precisam corresponder. Empates preservam a ordem do catálogo. */
    fun find(raw: String, area: FunctionArea? = null): List<AppFunction> {
        val query = fold(raw.take(120))
        val terms = words(query)
        val scoped = documents.filter { area == null || it.entry.area == area }
        if (query.isBlank()) return scoped.map { it.entry }
        if (terms.isEmpty()) return emptyList()
        return scoped.mapNotNull { document ->
            val ranks = terms.map { term ->
                maxOf(score(term, document.titleWords, 90), score(term, document.aliasWords, 68),
                    score(term, document.pathWords, 46))
            }
            if (ranks.any { it == 0 }) null else {
                val boost = when {
                    document.title == query -> 800
                    document.title.startsWith("$query ") -> 320
                    hasPhrase(document.title, query) -> 215
                    hasPhrase(document.aliases, query) -> 100
                    hasPhrase(document.path, query) -> 36
                    else -> 0
                }
                document.entry to (ranks.sum() + boost)
            }
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun score(term: String, tokens: Set<String>, exact: Int): Int = when {
        term in tokens -> exact
        term.length >= 2 && tokens.any { it.startsWith(term) } -> exact - 25
        term.length >= 4 && tokens.any { singleTypo(term, it) } -> exact - 39
        else -> 0
    }

    private fun hasPhrase(where: String, query: String): Boolean =
        " $where ".contains(" $query ")

    companion object {
        private val marks = Regex("\\p{M}+")
        private val separators = Regex("[^\\p{L}\\p{N}]+")
        private val ignored = setOf("a", "o", "as", "os", "de", "do", "da", "dos", "das", "no", "na",
            "nos", "nas", "em", "por", "para", "com", "e", "um", "uma", "como", "me", "quero", "onde", "encontro", "encontrar", "achar", "preciso")

        fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(marks, "").lowercase(Locale.ROOT).replace(separators, " ").trim()

        private fun words(value: String): Set<String> =
            value.split(' ').filter { it.isNotBlank() && it !in ignored }.toSet()

        /** Um erro simples por palavra: substituição, inserção, omissão ou troca de vizinhas. */
        private fun singleTypo(a: String, b: String): Boolean {
            if (abs(a.length - b.length) > 1) return false
            val first = (0 until minOf(a.length, b.length)).firstOrNull { a[it] != b[it] }
                ?: minOf(a.length, b.length)
            if (a.length == b.length) {
                if (first == a.length) return true
                if (a.regionMatches(first + 1, b, first + 1, a.length - first - 1)) return true
                return first + 1 < a.length && a[first] == b[first + 1] && a[first + 1] == b[first] &&
                    a.regionMatches(first + 2, b, first + 2, a.length - first - 2)
            }
            val shorter = if (a.length < b.length) a else b
            val longer = if (a.length < b.length) b else a
            return shorter.regionMatches(first, longer, first + 1, shorter.length - first)
        }
    }
}
