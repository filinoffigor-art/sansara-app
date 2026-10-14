plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

val sansaraKeystorePath = System.getenv("SANSARA_KEYSTORE_PATH")
val sansaraKeystorePassword = System.getenv("SANSARA_KEYSTORE_PASSWORD")
val sansaraKeyAlias = System.getenv("SANSARA_KEY_ALIAS")
val sansaraKeyPassword = System.getenv("SANSARA_KEY_PASSWORD")

android {
    namespace = "ru.sansara.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.sansara.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 32
        versionName = "0.25.0-mother"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        buildConfigField("String", "ADMIN_PHONE", "\"+79263046019\"")
        buildConfigField("String", "TILDA_YML_URL", "\"https://sansararitual.ru/tstore/yml/624c35d532ff51ab882ee8b031b31a93.yml\"")
        val backendApiUrl = providers.gradleProperty("SANSARA_BACKEND_API_URL").orElse("").get().trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "BACKEND_API_URL", "\"$backendApiUrl\"")
        val backendApiKey = providers.gradleProperty("SANSARA_BACKEND_API_KEY").orElse("").get()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "BACKEND_API_KEY", "\"$backendApiKey\"")
        // Код главного администратора для первого запуска рабочей сборки на «чистом» телефоне (секрет CI, в репозиторий не попадает).
        val adminBootstrap = providers.gradleProperty("SANSARA_ADMIN_CODE").orElse("").get().trim().filter { it.isLetterOrDigit() }
        buildConfigField("String", "ADMIN_BOOTSTRAP_CODE", "\"$adminBootstrap\"")
    }

    signingConfigs {
        if (!sansaraKeystorePath.isNullOrBlank() &&
            !sansaraKeystorePassword.isNullOrBlank() &&
            !sansaraKeyAlias.isNullOrBlank() &&
            !sansaraKeyPassword.isNullOrBlank()
        ) {
            create("sansaraPersistent") {
                storeFile = file(sansaraKeystorePath)
                storePassword = sansaraKeystorePassword
                keyAlias = sansaraKeyAlias
                keyPassword = sansaraKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfigs.findByName("sansaraPersistent")?.let { signingConfig = it }
        }
        // Рабочая сборка для установки на телефон: без тестовых кодов, без выбора роли, без демо-данных.
        // Подписана тем же ключом, что и прежние сборки, поэтому ставится поверх с сохранением данных.
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("sansaraPersistent") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestUtil("androidx.test:orchestrator:1.5.1")
    testImplementation("junit:junit:4.13.2")
}


kapt {
    correctErrorTypes = true
}
