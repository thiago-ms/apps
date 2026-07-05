package br.com.sample.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sample.core.ui.component.SectionHeader

/**
 * Tela inicial. Ponto de partida do app — substitua pelo conteúdo real da primeira
 * tela. As features dependem apenas de :core:* e recebem callbacks de navegação
 * como parâmetros (ex.: onOpenDetail), nunca navegam diretamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Sample") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            SectionHeader("Bem-vindo")
            Spacer(Modifier.height(12.dp))
            Text(
                "Esqueleto de app Android multi-módulo. Edite feature/home para começar.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
