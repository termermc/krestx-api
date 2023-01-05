import java.net.URI

class Project {
    companion object {
        const val name = "krestx-api"
        const val group = "net.termer.krestx"
        const val version = "1.1.0"
        const val repo = "github.com/termermc/krestx-api"
        const val description = "An opinionated Vert.x REST library built with Kotlin and coroutines. API portion."
    }
}

class Versions {
    companion object {
        const val jvm = "8"
        const val kotlin = "1.7.20"
        const val vertx = "4.3.3"
        const val jupiter = "5.9.0"
    }
}

group = Project.group
version = Project.version
description = Project.description

plugins {
    val ktVer = "1.7.20" // Versions.kotlin cannot be accessed in this scope

    kotlin("jvm") version ktVer

    id("org.jetbrains.dokka") version ktVer
    id("maven-publish")
    id("signing")

    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Vert.x
    val vertxArtifacts = arrayOf(
        "vertx-core",
        "vertx-web",
        "vertx-lang-kotlin",
        "vertx-lang-kotlin-coroutines"
    )
    for(artifact in vertxArtifacts) {
        compileOnly("io.vertx:$artifact:${Versions.vertx}")
        testImplementation("io.vertx:$artifact:${Versions.vertx}")
    }

    // Testing prerequisites
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.jupiter}")

    // Specific libraries required for testing
    testImplementation("io.vertx:vertx-web-client:${Versions.vertx}")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")

    // Docs
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Versions.kotlin}")
}

tasks.test {
    useJUnitPlatform()
}

// Set JVM version
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(Versions.jvm))
    }
}
tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.${Versions.jvm}"
    }
}
tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.${Versions.jvm}"
    }
}

tasks.create("kdoc") {
    dependsOn("dokkaHtml")
}
tasks.javadoc {
    dependsOn("dokkaJavadoc")
}

tasks.create("javadocJar", org.gradle.jvm.tasks.Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from("build/dokka/javadoc")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to Project.name,
            "Implementation-Version" to Project.version
        )
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.dokkaHtml {
    moduleName.set(Project.name)
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            groupId = Project.group
            artifactId = Project.name
            version = Project.version

            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set(Project.name)
                description.set(Project.description)
                url.set("https://${Project.repo}")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("termer")
                        name.set("Michael Termer")
                        email.set("termer@termer.net")
                    }
                }

                scm {
                    connection.set("scm:git:git://${Project.repo}.git")
                    developerConnection.set("scm:git:ssh://${Project.repo}.git")
                    url.set("https://${Project.repo}")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = properties["nexusUsername"] as String
                password = properties["nexusPassword"] as String
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}