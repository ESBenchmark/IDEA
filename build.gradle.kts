plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.kaciras.esbench"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3")

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
        sinceBuild.set("233")
        untilBuild.set("")
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
