import groovy.util.Node

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("1.3.20-eap-52")
    id("maven-publish")
    id("signing")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    jcenter()
}

kotlin {
    jvm() {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js()
    mingwX64()
    linuxX64()
    macosX64()

    sourceSets {
        commonMain {
            dependencies {
                api(kotlin("stdlib-common"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvm {
            compilations["main"].defaultSourceSet.dependencies {
                api(kotlin("stdlib-jdk8"))
            }
            compilations["test"].defaultSourceSet.dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        js {
            compilations["main"].defaultSourceSet.dependencies {
                api(kotlin("stdlib-js"))
            }
            compilations["test"].defaultSourceSet.dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val nativeMain by creating

        configure(listOf(linuxX64(), mingwX64(), macosX64())) {
            compilations["main"].defaultSourceSet.dependsOn(nativeMain)
        }
    }
}

publishing {
    repositories {
        maven(uri("$buildDir/repo"))
    }
}

// Publishing

//// Add a Javadoc JAR to each publication as required by Maven Central

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.value("javadoc")
    // TODO: instead of a single empty Javadoc JAR, generate real documentation for each module
}

publishing {
    publications.withType<MavenPublication>().all {
        artifact(javadocJar)
    }
}

//// The root publication also needs a sources JAR as it does not have one by default

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.value("sources")
}

publishing.publications.withType<MavenPublication>().getByName("kotlinMultiplatform").artifact(sourcesJar)

//// Customize the POMs adding the content required by Maven Central

fun customizeForMavenCentral(pom: org.gradle.api.publish.maven.MavenPom) = pom.withXml {
    fun Node.add(key: String, value: String) {
        appendNode(key).setValue(value)
    }

    fun Node.node(key: String, content: Node.() -> Unit) {
        appendNode(key).also(content)
    }

    asNode().run {
        add("description", "Demo project for Kotlin Multiplatform library publishing to Maven Central")
        add("name", "Kotlin MPP Maven Central Demo")
        add("url", "https://github.com/h0tk3y/k-new-mpp-samples")
        node("organization") {
            add("name", "com.github.h0tk3y")
            add("url", "https://github.com/h0tk3y")
        }
        node("issueManagement") {
            add("system", "github")
            add("url", "https://github.com/h0tk3y/k-new-mpp-samples/issues")
        }
        node("licenses") {
            node("license") {
                add("name", "Apache License 2.0")
                add("url", "https://github.com/h0tk3y/k-new-mpp-samples/blob/master/LICENSE")
                add("distribution", "repo")
            }
        }
        node("scm") {
            add("url", "https://github.com/h0tk3y/k-new-mpp-samples")
            add("connection", "scm:git:git://github.com/h0tk3y/k-new-mpp-samples.git")
            add("developerConnection", "scm:git:ssh://github.com/h0tk3y/k-new-mpp-samples.git")
        }
        node("developers") {
            node("developer") {
                add("name", "h0tk3y")
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().all {
        customizeForMavenCentral(pom)
    }
}

//// Sign the publications:

////// Also requires that signing.keyId, signing.password, and signing.secretKeyRingFile are provided as Gradle
////// properties.

////// No complex signing configuration is required here, as the signing plugin interoperates with maven-publish
////// and can simply add the signature files directly to the publications:

publishing {
    publications.withType<MavenPublication>().all {
        signing.sign(this@all)
    }
}