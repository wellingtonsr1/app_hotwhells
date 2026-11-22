package br.com.diecastcollector.feature.transfer

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * Dados que o manifesto do pacote declara (FR-140).
 *
 * Ficam num só lugar porque a exportação de controle de dados e o backup gravam o mesmo
 * pacote: duas cópias da mesma string fariam os dois caminhos divergirem sem ninguém notar.
 */
internal fun appVersion(context: Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
}

internal fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
