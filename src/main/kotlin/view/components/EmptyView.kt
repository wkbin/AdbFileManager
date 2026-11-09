package view.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.FolderProhibited

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
import androidx.compose.ui.unit.dp
import model.StringsManager

@Composable
fun EmptyView(){
    val strings by StringsManager.strings.collectAsState()
    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Regular.FolderProhibited,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = FluentTheme.colors.text.accent.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.errorEmptyDirectory,
                style = FluentTheme.typography.bodyStrong,
                color = FluentTheme.colors.text.text.secondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.errorNoFiles,
                style = FluentTheme.typography.body,
                color = FluentTheme.colors.text.text.secondary.copy(alpha = 0.7f)
            )
        }
    }
}