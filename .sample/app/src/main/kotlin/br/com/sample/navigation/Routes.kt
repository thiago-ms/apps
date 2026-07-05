package br.com.sample.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rotas de navegação. As abas aparecem no bottom navigation; destinos empilhados
 * (detalhe, edição, etc.) ficam fora do enum e são navegados por push.
 */
object Routes {
    const val HOME = "home"
    // Exemplo de destino empilhado com argumento:
    // const val DETAIL = "detail/{id}"
    // const val ARG_ID = "id"
    // fun detail(id: Long) = "detail/$id"
}

/** Aba do bottom navigation. A ordem define a posição na barra. */
enum class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Início", Icons.Filled.Home),
    // Adicione novas abas aqui, uma por tela.
}

val TAB_ROUTES: Set<String> = TabDestination.entries.map { it.route }.toSet()
