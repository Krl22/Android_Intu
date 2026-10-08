package com.intu.taxi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Measures requests at their natural height; the gradient's height must not constrain actions. */
@Composable
internal fun DriverHeaderContent(modifier: Modifier = Modifier, scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.then(if (scrollable) Modifier.fillMaxSize().verticalScroll(scrollState)
            else Modifier.fillMaxWidth())
            .padding(top = 50.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
