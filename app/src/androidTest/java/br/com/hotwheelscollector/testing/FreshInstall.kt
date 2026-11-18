package br.com.hotwheelscollector.testing

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import java.io.File
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Instalação nova para testes que sobem o app de verdade.
 *
 * Limpa as tabelas em vez de apagar o arquivo do banco. O `AppDatabase` do Hilt fica aberto
 * durante toda a instrumentação: apagar o arquivo por baixo dele deixa o processo com um
 * schema perdido — o `SQLiteOpenHelper` já se considera aberto, não roda `onCreate` outra
 * vez e a consulta seguinte falha com `no such table`. Foi o que aconteceu quando dois
 * testes no mesmo processo apagavam o banco antes de subir a activity.
 *
 * A conexão daqui é uma segunda conexão ao mesmo arquivo, aberta só para a limpeza e
 * fechada em seguida; o schema continua sendo criado pelo próprio Room (com o callback que
 * liga as FKs e cria o índice único parcial) quando o arquivo ainda não existe.
 *
 * As fotos gravadas também são apagadas: sem isso uma execução anterior faria a asserção
 * de arquivo passar sem que a execução atual tivesse gravado nada.
 */
internal fun freshInstallRule(): TestRule = TestRule { base, _ ->
    object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val database = Room
                .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                .addCallback(AppDatabase.CALLBACK)
                .build()
            try {
                database.clearAllTables()
            } finally {
                database.close()
            }
            File(context.filesDir, "images").deleteRecursively()
            base.evaluate()
        }
    }
}
