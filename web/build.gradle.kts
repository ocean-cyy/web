plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "top.xuqingquan.web"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourcePrefix = "scaffold_"
        buildConfigField("String", "AgentWebVersionName", "\"3.0.7\"")
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets {
        named("main") {
            jniLibs {
                srcDir("libs")//说明so的路径为该libs路径，关联所有so文件
            }
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("androidx.appcompat:appcompat:1.6.1")
    compileOnly("com.google.android.material:material:1.9.0")
    implementation("top.xuqingquan:utils:3.1.8")
    //download（需要用到web下载的时候需要依赖）
    compileOnly("com.github.Justson:Downloader:v5.0.4-androidx")
    compileOnly("com.tencent.tbs:tbssdk:44286")
    compileOnly("com.tencent.sonic:sdk:3.1.0")
    //test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
}

afterEvaluate {
    publishing {
        publications {
            register("release", MavenPublication::class) {
                from(components["release"])
                groupId = "top.xuqingquan"
                artifactId = "web"
                version = "3.0.7"
            }
        }
    }
}