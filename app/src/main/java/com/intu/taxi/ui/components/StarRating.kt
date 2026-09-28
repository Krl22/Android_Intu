package com.intu.taxi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cinco estrellas. Con [onRate] se pueden tocar para calificar; sin él solo muestran la nota. */
@Composable
fun StarRating(
    stars: Int,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    onRate: ((Int) -> Unit)? = null
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            val filled = i <= stars
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "$i ${if (i == 1) "estrella" else "estrellas"}",
                tint = if (filled) Color(0xFFF5A524) else Color(0xFFB0B5BD),
                modifier = Modifier
                    .size(size)
                    .then(if (onRate != null) Modifier.clickable { onRate(i) } else Modifier)
            )
        }
    }
}
