import com.varabyte.kobweb.gradle.library.util.configAsKobwebLibrary

val user = project.property("user") as String
val dev = project.property("dev") as String
val mail = project.property("mail") as String
val devURL = project.property("devURL") as String
val repo = project.property("repo") as String
val g = project.property("g") as String
val artifact = project.property("artifact") as String
val v = project.property("v") as String
val desc = project.property("desc") as String
val inception = project.property("inception") as String

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.dokka)
}

group = g
version = v

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
    configAsKobwebLibrary()

    applyDefaultHierarchyTemplate()

    sourceSets {
        jsMain.dependencies {
            implementation(libs.compose.html.core)
            implementation(libs.compose.runtime)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.kermit)
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
        footerMessage.set("&copy; 2026 $dev <$mail>")
    }
}

tasks.named("jsBrowserTest") {
    onlyIf { false }
}
