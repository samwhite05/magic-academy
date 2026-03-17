plugins {
    java
    id("com.gradleup.shadow") version "9.1.0" apply false
}

allprojects {
    group = "gg.magic.academy"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")  // MMO suite (if needed later)
        maven("https://mvnrepository.com/artifact/com.comphenix.protocol/ProtocolLib")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://mvnrepository.com/artifact/net.luckperms/api")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

subprojects {
    apply(plugin = "java")
    if (project.name != "api") {
        apply(plugin = "com.gradleup.shadow")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    if (project.name != "api") {
        tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
            if (project.name != "magic-core") {
                minimize()
            }
            relocate("com.zaxxer.hikari", "gg.magic.academy.libs.hikari")
        }

        tasks.named("build") {
            dependsOn("shadowJar")
        }
    } else {
        tasks.named("build") {
            dependsOn("jar")
        }
    }
}
