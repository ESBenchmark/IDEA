plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.kaciras"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2")

    // Target IDE Platform, IU = Intellij Ultimate
    type.set("IU")

    plugins.set(listOf("JavaScript", "NodeJS"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
