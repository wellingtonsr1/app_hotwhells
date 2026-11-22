package br.com.diecastcollector

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Ponto de entrada do processo. O grafo de dependências é montado em tempo de compilação
 * (Hilt), sem reflexão em runtime — decisão D4 do research.md.
 *
 * Também fornece a fábrica de workers ao WorkManager, para que backup, restauração e
 * importação recebam os serviços do grafo (T134). A inicialização padrão do WorkManager é
 * removida no manifesto para que esta configuração seja a única.
 */
@HiltAndroidApp
class DiecastApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
