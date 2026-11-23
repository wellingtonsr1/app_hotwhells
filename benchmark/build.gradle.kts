plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

/**
 * Duas suítes, um módulo `com.android.test` (não vira APK de produto):
 *
 * - `StartupBaselineProfile` gera o Baseline Profile de abertura (T135), copiado para
 *   `app/src/main/baseline-prof.txt` e empacotado pelo AGP em `assets/dexopt/baseline.prof`;
 * - `CatalogBenchmark` mede a abertura e a rolagem da Coleção com 500 variantes (SC-003 da
 *   spec 003), contra o build `:app:benchmark`.
 */
android {
    namespace = "br.com.diecastcollector.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // O perfil é gerado no emulador x86_64 do projeto, não em aparelho físico.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        // Perfil de abertura (StartupBaselineProfile) e medição de SC-003 (CatalogBenchmark):
        // o MacrobenchmarkRule recusa rodar se o seu tipo não estiver nesta lista.
        testInstrumentationRunnerArguments["androidx.benchmark.enabledRules"] =
            "BaselineProfile,Macrobenchmark"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.espresso.core)
}
