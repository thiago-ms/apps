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
    }
}

rootProject.name = "Sample"

// App host (thin) + módulos. O :core concentra tema/componentes (e domínio/dados
// quando houver); cada tela é um módulo de feature (Android Library) próprio, para
// builds incrementais/paralelos e manutenção isolada. Features dependem só de
// :core:* (nunca de :app nem umas das outras) — a navegação entre elas vive no :app.
include(":app")
include(":core:ui")
include(":feature:home")
// Ao criar um app real, adicione aqui os demais módulos, por exemplo:
// include(":core:data")      // Room / repositório / domínio
// include(":feature:detail")
// include(":feature:settings")
