pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Imin's built-in printer SDK (com.github.iminsoftware:IminPrinterLibrary) is published
        // via JitPack, built directly from https://github.com/iminsoftware/IminPrinterLibrary —
        // see PRINTER_SETUP.md for how this coordinate was verified against their source.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PosNova"
include(":app")
