package com.intu.taxi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.models.ParticipantRating
import com.intu.taxi.ui.theme.AppearanceColors

/** Estrella con el promedio, o "Nuevo" con pocas calificaciones. */
@Composable
fun RatingBadge(rating: ParticipantRating, modifier: Modifier = Modifier) {
    val description = if (rating.isNew) "Usuario nuevo, pocas calificaciones"
    else "Calificación ${rating.label} de 5, ${rating.count} calificaciones"
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .background(AppearanceColors.tint(Color(0xFFFFF4DE)), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF5A524), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            rating.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppearanceColors.foreground(Color(0xFF1E1F47))
        )
    }
}
