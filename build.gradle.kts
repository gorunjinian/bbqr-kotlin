plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

group = "com.gorunjinian"
version = "1.0.0"

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
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
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
