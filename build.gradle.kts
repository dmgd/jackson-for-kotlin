import java.util.Base64

plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
    signing
    distribution
}

group = "uk.co.abstrate"
version = "1.0.0-SNAPSHOT"

val isReleaseVersion = !project.version.toString().endsWith("-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    val jacksonVersion = "2.17.2"
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            from(components["java"])
            pom {
                name = project.name
                description = "Bootstrap for services using jackson for json de/serialisation, providing a default object mapper that works nicely out of the box"
                url = "https://github.com/dmgd/jackson-for-kotlin"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Jordan Stewart"
                        email = "jordan.r.stewart@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:dmgd/jackson-for-kotlin.git"
                    developerConnection = "scm:git:git@github.com:dmgd/jackson-for-kotlin.git"
                    url = "https://github.com/dmgd/jackson-for-kotlin"
                }
            }
        }
        create<MavenPublication>("bundle") {
            artifact(tasks.distZip)
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repos/bundles"))
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD"),
    )
    sign(publishing.publications)
}

distributions {
    main {
        contents {
            from(layout.buildDirectory.dir("repos/bundles"))
        }
    }
}

tasks.named("distZip") {
    dependsOn("publishLibPublicationToMavenRepository")
}

tasks.register<Exec>("publishToMavenCentral") {
    onlyIf {
        isReleaseVersion
    }
    dependsOn(tasks.distZip)
    val credentials = "${System.getenv("MAVEN_CENTRAL_USERNAME")}:${System.getenv("MAVEN_CENTRAL_PASSWORD")}"
    val token = Base64.getEncoder().encodeToString(credentials.toByteArray())
    commandLine(
        "curl",
        "--request",
        "POST",
        "--header",
        "Authorization: Bearer $token",
        "--form",
        "bundle=@build/distributions/${project.name}-${project.version}.zip",
        "https://central.sonatype.com/api/v1/publisher/upload",
    )
}
