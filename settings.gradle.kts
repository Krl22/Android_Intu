pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox Maven con token de descargas (usamos propiedad MAPBOX_DOWNLOADS_TOKEN)
        val mapboxToken = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
            ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            isAllowInsecureProtocol = false
            credentials {
                username = "mapbox"
                password = mapboxToken
            }
        }
    }
}

rootProject.name = "Intu"
include(":app")
 