plugins {
    `java-library`
    `maven-publish`
    jacoco
}
val jacksonVersion: String by project
val junitVersion: String by project
val wiremockVersion: String by project
val bouncyCastleVersion: String by project
val testcontainersVersion: String by project
dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:2.0.12")
    // BouncyCastle for local secp256k1 signing
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.3")
    // Testcontainers for integration tests
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}
tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against a live Paladin Docker container."
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "paladin-sdk-java"
            pom {
                name.set("Paladin Java SDK")
                description.set("Java SDK for interacting with Paladin privacy nodes via JSON-RPC and WebSocket")
                url.set("https://github.com/LFDT-Paladin/paladin")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/LFDT-Paladin/paladin-java-sdk")
            credentials {
                username = project.findProperty("githubUser") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("githubToken") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
java {
    withJavadocJar()
    withSourcesJar()
}
