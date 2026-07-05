package br.com.sample.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.sample.feature.home.HomeScreen

/**
 * Casca de navegação: bottom navigation com as abas de [TabDestination]. A barra só
 * aparece nas abas; destinos empilhados (detalhe, edição) escondem a barra. Trocar de
 * aba limpa a pilha da aba (popUpTo no destino inicial).
 */
@Composable
fun SampleApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val emAba = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (emAba) {
                NavigationBar {
                    TabDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.trocarAba(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Só reservamos o espaço da barra inferior; o topo (status bar) é tratado
            // pela TopAppBar de cada tela — evita o padding-top duplicado.
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            // Troca de tela seca (sem animação de transição).
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.HOME) {
                HomeScreen()
            }
            // Registre aqui os destinos empilhados de cada feature, por exemplo:
            // composable(
            //     route = Routes.DETAIL,
            //     arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
            // ) { entry ->
            //     val id = entry.arguments?.getLong(Routes.ARG_ID) ?: return@composable
            //     DetailScreen(id = id, onBack = { navController.popBackStack() })
            // }
        }
    }
}

/** Troca de aba resetando a pilha da aba. */
private fun NavHostController.trocarAba(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
