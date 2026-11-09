package view.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ErrorCircle

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import model.StringsManager

@Composable
fun ErrorView(errorMessage: String){
    val strings by StringsManager.strings.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Regular.ErrorCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = FluentTheme.colors.system.critical.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.errorTitle,
                style = FluentTheme.typography.bodyStrong,
                color = FluentTheme.colors.system.critical
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = FluentTheme.typography.body,
                color = FluentTheme.colors.text.text.secondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}