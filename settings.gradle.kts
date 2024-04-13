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
        maven {
            // Using an environment variable to handle the URL, username, and password for security
            url = uri(System.getenv("CARDINAL_COMMERCE_URL") ?: "https://cardinalcommerceprod.jfrog.io/artifactory/android")
            credentials {
                username = System.getenv("CARDINAL_COMMERCE_USERNAME") ?: "paypal_sgerritz"
                password = System.getenv("CARDINAL_COMMERCE_PASSWORD") ?: "AKCp8jQ8tAahqpT5JjZ4FRP2mW7GMoFZ674kGqHmupTesKeAY2G8NcmPKLuTxTGkKjDLRzDUQ"
            }
        }
    }
}

rootProject.name = "Dishcovery"
include(":app")
