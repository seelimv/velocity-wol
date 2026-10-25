
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
    alias(libs.plugins.minotaur)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

configurations {
    testImplementation {
        extendsFrom(compileOnly.get())
    }
}

dependencies {
    compileOnly(libs.velocity.api)

    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.slf4j.simple)
}

kotlin {
    jvmToolchain(25)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        relocate("kotlin", "dev.seelim.velocitywol.libs.kotlin")
        relocate("kotlinx", "dev.seelim.velocitywol.libs.kotlinx")
    }

    build {
        dependsOn(shadowJar)
    }

    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }

    named("modrinth") {
        dependsOn(named("modrinthSyncBody"))
    }

    test {
        useJUnitPlatform()
    }
}

modrinth {
    projectId.set("gzFHVGLE")
    versionNumber.set(project.version.toString())
    versionName.set("Velocity-WoL ${project.version}")
    versionType.set("release")
    uploadFile.set(tasks.shadowJar)

    loaders.add("velocity")
    gameVersions.addAll(
        "26.1", "26.1.1", "26.1.2", "26.2", "26.3",
    )
    changelog.set("See full changelog on GitHub: https://github.com/seelimv/velocity-wol/releases/tag/v${project.version}")

    syncBodyFrom.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText)
}
