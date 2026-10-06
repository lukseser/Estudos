package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun WysiwygToolbar(
    modifier: Modifier = Modifier,
    onApplyFormat: (prefix: String, suffix: String) -> Unit,
    onApplyColor: (colorName: String) -> Unit,
    onApplyHighlight: (highlightName: String) -> Unit,
    onClearFormat: () -> Unit,
    onInsertList: (isNumbered: Boolean) -> Unit,
    isPenActive: Boolean = false,
    onTogglePen: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("wysiwyg_toolbar"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Negrito
            ToolbarIconButton(
                icon = Icons.Default.FormatBold,
                description = "Negrito",
                testTag = "format_bold_btn",
                onClick = { onApplyFormat("**", "**") }
            )

            // Itálico
            ToolbarIconButton(
                icon = Icons.Default.FormatItalic,
                description = "Itálico",
                testTag = "format_italic_btn",
                onClick = { onApplyFormat("*", "*") }
            )

            // Sublinhado
            ToolbarIconButton(
                icon = Icons.Default.FormatUnderlined,
                description = "Sublinhado",
                testTag = "format_underlined_btn",
                onClick = { onApplyFormat("<u>", "</u>") }
            )

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Cores de Fonte
            ColorChip(color = Color(0xFF0F172A), tooltip = "Escuro", onClick = { onApplyColor("escuro") })
            ColorChip(color = Color(0xFF3B82F6), tooltip = "Azul", onClick = { onApplyColor("azul") })
            ColorChip(color = Color(0xFFDC2626), tooltip = "Vermelho", onClick = { onApplyColor("vermelho") })
            ColorChip(color = Color(0xFF16A34A), tooltip = "Verde", onClick = { onApplyColor("verde") })

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Cores de Marca-texto
            Icon(
                imageVector = Icons.Default.Highlight,
                contentDescription = "Marca-texto",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )

            ColorChip(color = Color(0xFFFEF08A), tooltip = "Amarelo", isHighlight = true, onClick = { onApplyHighlight("amarelo") })
            ColorChip(color = Color(0xFFBBF7D0), tooltip = "Verde", isHighlight = true, onClick = { onApplyHighlight("verde") })
            ColorChip(color = Color(0xFFFBCFE8), tooltip = "Rosa", isHighlight = true, onClick = { onApplyHighlight("rosa") })

            // Borracha de formatação
            ToolbarIconButton(
                icon = Icons.Default.AutoFixNormal,
                description = "Limpar",
                testTag = "clear_format_btn",
                tint = MaterialTheme.colorScheme.error,
                onClick = onClearFormat
            )

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Listas
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                description = "Marcadores",
                testTag = "format_bullets_btn",
                onClick = { onInsertList(false) }
            )

            ToolbarIconButton(
                icon = Icons.Default.FormatListNumbered,
                description = "Numerada",
                testTag = "format_numbered_btn",
                onClick = { onInsertList(true) }
            )

            if (onTogglePen != null) {
                VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPenActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isPenActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onTogglePen() }
                        .testTag("wysiwyg_pen_overlay_toggle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Caneta",
                            tint = if (isPenActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tb_icon_scale"
    )

    IconButton(
        onClick = {
            isPressed = true
            onClick()
            isPressed = false
        },
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ColorChip(
    color: Color,
    tooltip: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "color_chip_scale"
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = if (isHighlight) Color.Gray.copy(alpha = 0.4f) else Color.Transparent,
                shape = CircleShape
            )
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            }
    )
}
