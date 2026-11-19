plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.darielvrx.easyclaudemobile"
	compileSdk = 34
	
	signingConfigs {
		create("debug") {
			storeFile = file(System.getenv("DEBUG_KEYSTORE_PATH") ?: "debug.keystore")
			storePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD") ?: "android"
			keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
			keyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
		}
	}
	
	defaultConfig {
		applicationId = "com.darielvrx.easyclaudemobile"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "0.1.0-alpha.1"
	}
	
	buildTypes {
		debug {
			signingConfig = signingConfigs.getByName("debug")
		}
		release {
			// TODO: usar un signingConfig de release real antes de distribuir en serio
			isMinifyEnabled = false
			signingConfig = signingConfigs.getByName("debug")
		}
	}
	
	compileOptions {
		sourceCompability = JavaVersion.VERSION_1_8
		targetCompability = JavaVersion.VERSION_1_8
	}
	
	buildFeatures {
		compose = true
	}
}

dependencies {
	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation(platform("androidx.compose:compose-bom:2024.09.00"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.material3:material3")
}