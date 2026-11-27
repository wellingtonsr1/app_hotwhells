package br.com.diecastcollector

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import br.com.diecastcollector.feature.welcome.WELCOME_SKIP_PROPERTY

/**
 * Runner da suíte instrumentada.
 *
 * A maioria dos testes abre `MainActivity` pelo caminho padrão e espera a navegação; a
 * abertura com a arte da marca pararia o teste na tela errada. A propriedade é ligada aqui,
 * antes de qualquer activity existir, e o app só a consulta ao subir. Aparência e dados
 * continuam intocados.
 */
public class DiecastTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        System.setProperty(WELCOME_SKIP_PROPERTY, "true")
        super.onCreate(arguments)
    }
}
