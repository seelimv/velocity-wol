
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.velocity.api)
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
}
