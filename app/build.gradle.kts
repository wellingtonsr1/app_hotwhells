import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

/**
 * Assinatura de release (T136, R.15).
 *
 * `keystore.properties` e o `.jks` ficam fora do versionamento (.gitignore). Sem o arquivo
 * — outro checkout, CI — o build de release continua funcionando, porém sem assinatura.
 * Atualizar o app no aparelho exige **esta** chave: sem ela, só reinstalando do zero.
 */
val keystoreProperties: Properties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "br.com.hotwheelscollector"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.hotwheelscollector"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // FR-171: a data de compilação é gravada no build, não inventada na tela.
        buildConfigField("String", "BUILD_TIME", "\"${Instant.now()}\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // v1 é distribuída por sideload em celular: as bibliotecas nativas do ML Kit
            // somam ~11 MB por ABI, e x86/x86_64 só existem em emulador. Filtro aqui, e
            // não em defaultConfig, para o APK de debug continuar rodando no emulador.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty(
                "user.home",
                rootProject.layout.projectDirectory.dir(".tools/user-home").asFile.absolutePath,
            )
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
    }
}

/**
 * O build `nonMinifiedRelease` existe só para gerar o Baseline Profile e roda no emulador
 * x86_64; o release de entrega continua arm-only (ver `ndk.abiFilters` acima).
 */
afterEvaluate {
    android.buildTypes.getByName("nonMinifiedRelease").ndk.abiFilters.add("x86_64")
}

dependencies {
    baselineProfile(project(":benchmark"))

    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(platform(libs.compose.bom))

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    // Os testes instrumentados limpam as tabelas do banco real entre um teste e outro.
    androidTestImplementation(libs.room.runtime)
    debugImplementation(libs.compose.ui.test.manifest)
}
