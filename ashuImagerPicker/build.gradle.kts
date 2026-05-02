plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.ashu.ashuutils"
    compileSdk = 36 // use 34 for now, 36 is not stable yet (causes build issues)

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ✅ This ensures only release AAR is published
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.Ashu-Hasan"   // GitHub username
                artifactId = "Ashu_Image_Picker"             // Library name
                version = "1.0.2"                   // Match your Git tag
                from(components["release"])

                pom {
                    name.set("Ashu_Image_Picker")
                    description.set("A collection of reusable Android utility methods by Mr. Ash.")
                    url.set("https://github.com/Ashu-Hasan/Ashu_Image_Picker")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("ashuhasan")
                            name.set("Ashu Hasan")
                            email.set("ashu@example.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/Ashu-Hasan/Ashu_Image_Picker.git")
                        developerConnection.set("scm:git:ssh://github.com/Ashu-Hasan/Ashu_Image_Picker.git")
                        url.set("https://github.com/Ashu-Hasan/Ashu_Image_Picker")
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    implementation("com.google.android.gms:play-services-base:18.5.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.github.yalantis:ucrop:2.2.10")
}
