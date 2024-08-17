plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.kaciras.esbench"
version = "1.1.0"

repositories {
    intellijPlatform {
        defaultRepositories()
    }
    mavenCentral()
}

// Target IDE Platform, IU = Intellij Ultimate
dependencies {
    intellijPlatform {
        create("IU", "2023.3")
        bundledPlugins(listOf("JavaScript", "NodeJS"))
        javaCompiler()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = ""
        }
    }
    signing {
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
    }
    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
}
