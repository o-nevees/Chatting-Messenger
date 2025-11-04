plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.service"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
           buildConfigField("String", "API_HOST", "\"appshub.shop\"")
            
        }
        debug {
            buildConfigField("String", "API_HOST", "\"appshub.shop\"")
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
        buildConfig = true
        viewBinding = false
        compose = false
    }
    
    
}

dependencies {
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.okhttp)
    
    implementation(libs.protobuf.java)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins.create("java") {
            }
        }
    }
}