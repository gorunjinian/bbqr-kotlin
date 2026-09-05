plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

group = "com.gorunjinian"
version = "1.0.3"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.jzlib)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()

    // Sign only where a key is actually available. CI supplies one via
    // ORG_GRADLE_PROJECT_signingInMemoryKey; locally there is none, which keeps
    // publishToMavenLocal working without any GPG config on the machine. Maven
    // Local does not need signatures — only the Central bundle does.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates("com.gorunjinian", "bbqr", version.toString())

    pom {
        name.set("bbqr-kotlin")
        description.set("Pure Kotlin implementation of the BBQr protocol for splitting and joining data across multiple QR codes")
        url.set("https://github.com/gorunjinian/bbqr-kotlin")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("gorunjinian")
                name.set("gorunjinian")
                url.set("https://gorunjinian.com")
            }
        }

        scm {
            url.set("https://github.com/gorunjinian/bbqr-kotlin")
            connection.set("scm:git:git://github.com/gorunjinian/bbqr-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/gorunjinian/bbqr-kotlin.git")
        }
    }
}
