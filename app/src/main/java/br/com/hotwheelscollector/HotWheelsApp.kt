package br.com.hotwheelscollector

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Ponto de entrada do processo. O grafo de dependências é montado em tempo de compilação
 * (Hilt), sem reflexão em runtime — decisão D4 do research.md.
 */
@HiltAndroidApp
class HotWheelsApp : Application()
