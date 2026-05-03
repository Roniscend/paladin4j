plugins {
    java
    `maven-publish`
}
allprojects {
    group = "io.paladin"
    version = "0.1.0-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}
subprojects {
    apply(plugin = "java")
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all"))
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
