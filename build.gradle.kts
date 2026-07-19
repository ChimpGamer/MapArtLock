plugins {
    java
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.run.paper)
}

group = "nl.chimpgamer"
version = "1.1.9"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
    // Surfaces deprecated Bukkit API that Paper has a supported replacement for.
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks {
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }
}

sonar {
    properties {
        property("sonar.projectKey", "map-art-lock")
        property("sonar.projectVersion", project.version.toString())
        property("sonar.sourceEncoding", "UTF-8")
    }
}
