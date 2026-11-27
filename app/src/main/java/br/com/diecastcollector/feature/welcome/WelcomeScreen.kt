package br.com.diecastcollector.feature.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.diecastcollector.R

/**
 * Propriedade que a suíte instrumentada liga antes de abrir qualquer activity. Sem ela, o
 * primeiro teste de cada execução pararia na apresentação em vez de exercitar a tela que
 * pretende medir (ver `DiecastTestRunner`).
 */
public const val WELCOME_SKIP_PROPERTY: String = "diecast.skipWelcome"

/**
 * Largura máxima do bloco de texto da apresentação.
 *
 * Menor que [br.com.diecastcollector.ui.components.formMaxWidth] de propósito: são três
 * linhas, e um botão de 720 dp em tablet ou janela livre fica desproporcional à chamada.
 */
private val welcomeMaxWidth = 480.dp

/**
 * Altura abaixo da qual a janela é considerada baixa (paisagem de celular, janela livre).
 *
 * Nessa faixa a arte perde o centro e o bloco de texto precisa caber em pouco mais de
 * 300 dp: o título desce um degrau de escala e os espaçamentos encolhem.
 */
private val welcomeShortHeight = 480.dp

/**
 * Largura a partir da qual a apresentação passa a ter duas colunas — a mesma fronteira da
 * classe média usada pelo resto do app (FR-301) —, desde que a janela seja mais larga que
 * alta. Em tablet em retrato (700 × 900 dp) duas colunas de 350 dp ficariam estreitas para
 * o nome e a ação, e a arte inteira com o texto no rodapé usa melhor a altura.
 */
private val welcomeTwoPaneWidth = 600.dp

/** A partir da classe expandida (840 dp) a divisão em colunas vale mesmo em retrato. */
private val welcomeWideWidth = 840.dp

/**
 * Abertura do app: a arte completa da marca (cartela, expositor e carro) com o nome e a
 * assinatura do produto por cima.
 *
 * Aparece em toda inicialização e só sai no toque em **Começar** — por isso não há estado
 * persistido nem atraso automático. A arte vem sem texto e o texto é desenhado em Compose:
 * fica nítido em qualquer densidade, acompanha o tamanho de fonte do sistema e não vira
 * imagem de novo quando o nome mudar. Como o fundo é sempre escuro, o texto é branco nos
 * dois temas e o degradê garante contraste da chamada e do botão sobre a foto.
 *
 * A tela se reorganiza pela largura do contêiner (FR-301, FR-303):
 *
 * - **compacta** (celular em retrato): arte de fundo inteira e o bloco de texto no rodapé,
 *   como desenhado na identidade do produto;
 * - **média e expandida** (celular em paisagem, tablet, janela livre): duas colunas — a arte
 *   à esquerda, sem ser esticada nem perder o assunto, e o texto à direita, centralizado na
 *   altura, para o botão não atravessar 1.000 dp de tela.
 *
 * Em qualquer classe o bloco de texto tem largura de leitura (FR-308), rola quando a fonte
 * do sistema a 200% ou a pouca altura não deixam tudo caber (FR-326) e respeita os insets de
 * `safeDrawing`, inclusive recorte de câmera em paisagem (FR-325).
 */
@Composable
public fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // A apresentação é escura por desenho em qualquer tema: é a cor que sustenta o
            // texto branco sobre a arte (FR-328 — identidade preservada).
            .background(Color.Black),
    ) {
        val duasColunas = maxWidth >= welcomeWideWidth ||
            (maxWidth >= welcomeTwoPaneWidth && maxWidth > maxHeight)

        if (duasColunas) {
            TwoPaneWelcome(
                onStart = onStart,
                shortHeight = maxHeight < welcomeShortHeight,
            )
        } else {
            ArtworkWelcome(
                onStart = onStart,
                shortHeight = maxHeight < welcomeShortHeight,
            )
        }
    }
}

/**
 * Apresentação compacta: arte ocupando toda a janela e texto no rodapé.
 *
 * O bloco centralizado só difere do desenho original em janelas estreitas de verdade: a
 * largura máxima não chega a valer abaixo de 480 dp, então no celular em retrato nada muda.
 */
@Composable
private fun ArtworkWelcome(
    onStart: () -> Unit,
    shortHeight: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.welcome_art),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.30f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.94f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    horizontal = 24.dp,
                    vertical = if (shortHeight) 16.dp else 24.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WelcomeCopy(
                onStart = onStart,
                shortHeight = shortHeight,
                modifier = Modifier
                    .widthIn(max = welcomeMaxWidth)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * Apresentação em duas colunas: arte à esquerda, texto centralizado à direita.
 *
 * A arte recebe `Crop` ancorado no centro porque a imagem é retrato: recortada pelo topo em
 * uma janela larga ela mostraria só as faixas vermelhas, e o carro — o assunto da arte —
 * sumiria. O degradê horizontal no encontro das colunas evita uma emenda dura entre a foto
 * e o fundo preto.
 */
@Composable
private fun TwoPaneWelcome(
    onStart: () -> Unit,
    shortHeight: Boolean,
) {
    Row(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Image(
                painter = painterResource(R.drawable.welcome_art),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black,
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            WelcomeCopy(
                onStart = onStart,
                shortHeight = shortHeight,
                modifier = Modifier
                    .widthIn(max = welcomeMaxWidth)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * Nome, assinatura e ação de entrada.
 *
 * Rola quando não couber — fonte do sistema a 200%, janela baixa — para a ação nunca sair
 * da área visível (FR-326).
 */
@Composable
private fun WelcomeCopy(
    onStart: () -> Unit,
    shortHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (shortHeight) 8.dp else 12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = if (shortHeight) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.displaySmall
            },
            color = Color.White,
            letterSpacing = 1.sp,
        )
        Text(
            text = stringResource(R.string.welcome_tagline),
            style = if (shortHeight) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = Color.White.copy(alpha = 0.85f),
        )
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(top = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
