import buildlogic.getVersion
import buildlogic.stringyLibs

plugins {
    `java-library`
    id("buildlogic.common")
    id("buildlogic.common-java")
    id("io.papermc.paperweight.userdev")
}

val requiresReobfJar = project.name.startsWith("adapter-1_")

// The dev bundles for the 1.21 line pin a codebook whose ASM stops at Java 24 class files, and paperweight runs it
// on this project's toolchain - so on the toolchain the rest of the build wants, remapping them dies reading the
// JDK's own classes. They target Java 21 anyway, so hand those adapters a JDK that can read what it is given.
if (requiresReobfJar) {
    the<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

paperweight {
    injectPaperRepository = false
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION
}

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            excludeModule("net.fabricmc", "yarn")
        }
    }
    maven {
        name = "IntellectualSites"
        url = uri("https://repo.intellectualsites.dev/repository/maven-all/")
    }
    mavenCentral()
    // FAWE-Folia: paperweight adds its own FabricMC repository for the newer dev bundles, but not for
    // adapter-1_21's pinned one, so its yarn param mappings resolve against EngineHub's mirror alone -
    // which 404s for them. A plain repository declaration is not enough, as net.fabricmc is bound
    // exclusively to paperweight's repository; claim the group exclusively so it always has a home.
    exclusiveContent {
        forRepository {
            maven {
                name = "FabricMC"
                url = uri("https://maven.fabricmc.net/")
            }
        }
        filter {
            includeGroup("net.fabricmc")
        }
    }
    afterEvaluate {
        killNonEngineHubRepositories()
    }
}

dependencies {
    implementation(project(":worldedit-bukkit"))
    constraints {
        //Reduces the amount of libraries Gradle and IntelliJ need to resolve
        implementation("net.kyori:adventure-bom") {
            version { strictly(stringyLibs.getVersion("adventure").strictVersion) }
            because("Ensure a consistent version of adventure is used.")
        }
    }
}

java {
    // Required when we de-sync release option and declared Java versions.
    disableAutoTargetJvm()
}

tasks.named("assemble") {
    if (requiresReobfJar) {
        dependsOn("reobfJar")
    }
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
