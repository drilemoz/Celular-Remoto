package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.R

/** Estilo de diálogo compartilhado por telas legadas e novas; preserva callbacks,
 * confirmação negativa e semântica das ações perigosas. */
internal fun themedPopup(context: Context): AlertDialog.Builder {
    val night = when (SettingsStore.theme(context)) {
        "dark", "amoled" -> true
        "system" -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        else -> false
    }
    return AlertDialog.Builder(context,
        if (SettingsStore.theme(context) == "amoled") R.style.MonitorDialogAmoled
        else if (night) R.style.MonitorDialogDark
        else R.style.MonitorDialogLight)
}
