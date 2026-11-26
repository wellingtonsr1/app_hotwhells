package br.com.diecastcollector.ui.theme

/**
 * Aparência escolhida pelo usuário (Spec 006, FR-401).
 *
 * É um estado de apresentação: não é dado da coleção, não vai para o banco nem para o
 * pacote de backup. A escolha vive em `AppearancePreferences` (camada de aplicação) e
 * chega ao tema apenas como este enum — `core:ui` não conhece DataStore (NFR-406).
 */
public enum class ThemeMode {
    /** Acompanha o tema do Android, inclusive mudanças com o app aberto (FR-407). */
    SYSTEM,

    /** Força a paleta clara, independentemente do Android (BR-403). */
    LIGHT,

    /** Força a paleta escura, independentemente do Android (BR-403). */
    DARK,
}

/**
 * Única resolução de "escuro?" do app (FR-405).
 *
 * `systemInDark` só é consultado em [ThemeMode.SYSTEM]; nos modos forçados a escolha do
 * usuário prevalece sobre o Android.
 */
public fun ThemeMode.isDark(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
