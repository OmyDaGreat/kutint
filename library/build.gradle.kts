import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

val user: String by project
val dev: String by project
val mail: String by project
val devURL: String by project
val repo: String by project
val g: String by project
val artifact: String by project
val v: String by project
val desc: String by project
val inception: String by project

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinter)
}

group = g
version = v

kotlin {
    jvm()

    android {
        namespace = "$g.$artifact"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        withJava()
        withHostTest {}
        withDeviceTest {}

        compilerOptions {
            jvmTarget.set(JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js {
        nodejs()
    }

    applyDefaultHierarchyTemplate()

    @Suppress("unused")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(g, artifact, v)

    pom {
        name = repo
        description = desc
        inceptionYear = inception
        url = "https://github.com/$user/$repo"
        licenses {
            license {
                name = "MIT License"
                url = "https://mit.malefic.xyz"
            }
        }
        developers {
            developer {
                name = dev
                email = mail
                url = devURL
            }
        }
        scm {
            url = "https://github.com/$user/$repo"
            connection = "scm:git:git://github.com/$user/$repo.git"
            developerConnection = "scm:git:ssh://github.com/$user/$repo.git"
        }
    }
}

dokka {
    pluginsConfiguration.html {
        footerMessage.set("&copy; 2025 $dev <$mail>")
    }
}
