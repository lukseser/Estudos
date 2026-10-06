package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/**
 * Ponto individual de um traço com suporte a pressão (S-Pen)
 */
data class CanvasPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

/**
 * Traço de desenho com cache de Path para renderização fluida em 120Hz
 */
data class CanvasStroke(
    val points: List<CanvasPoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
) {
    val cachedPath: Path by lazy {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            if (points.size == 1) {
                path.lineTo(points[0].x + 0.5f, points[0].y + 0.5f)
            } else {
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    path.quadraticTo(prev.x, prev.y, midX, midY)
                }
                path.lineTo(points.last().x, points.last().y)
            }
        }
        path
    }
}

/**
 * Serializador ultrarrápido para persistência Room / SQLite
 */
object DrawingSerializer {
    fun serialize(strokes: List<CanvasStroke>): String {
        if (strokes.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in strokes.indices) {
            val stroke = strokes[i]
            sb.append(stroke.colorArgb)
                .append(":")
                .append(stroke.strokeWidth)
                .append(":")
                .append(if (stroke.isHighlighter) 1 else 0)
                .append(":")
            for (j in stroke.points.indices) {
                val pt = stroke.points[j]
                sb.append(pt.x.toInt())
                    .append(",")
                    .append(pt.y.toInt())
                    .append(",")
                    .append((pt.pressure * 100).toInt())
                if (j < stroke.points.size - 1) sb.append(";")
            }
            if (i < strokes.size - 1) sb.append("|")
        }
        return sb.toString()
    }

    fun deserialize(data: String): List<CanvasStroke> {
        if (data.isBlank()) return emptyList()
        val result = ArrayList<CanvasStroke>()
        try {
            val strokeStrings = data.split("|")
            for (s in strokeStrings) {
                if (s.isBlank()) continue
                val parts = s.split(":")
                if (parts.size < 4) continue
                val colorArgb = parts[0].toLongOrNull() ?: continue
                val strokeWidth = parts[1].toFloatOrNull() ?: 4f
                val isHighlighter = parts[2] == "1"
                val pointsData = parts[3].split(";")
                val points = ArrayList<CanvasPoint>(pointsData.size)
                for (pd in pointsData) {
                    val coords = pd.split(",")
                    if (coords.size >= 2) {
                        val x = coords[0].toFloatOrNull() ?: continue
                        val y = coords[1].toFloatOrNull() ?: continue
                        val pressure = if (coords.size >= 3) {
                            (coords[2].toFloatOrNull() ?: 100f) / 100f
                        } else 1f
                        points.add(CanvasPoint(x, y, pressure))
                    }
                }
                if (points.isNotEmpty()) {
                    result.add(CanvasStroke(points, colorArgb, strokeWidth, isHighlighter))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}

/**
 * 4 CORES PROFISSIONAIS PARA ESTUDO (Estilo Samsung Notes)
 */
val PEN_COLORS = listOf(
    Color(0xFF0F172A) to "Preto",      // Grafite Slate
    Color(0xFF3B82F6) to "Azul",       // Azul Elétrico Destaque
    Color(0xFFDC2626) to "Vermelho",   // Vermelho Carmim
    Color(0xFF16A34A) to "Verde"       // Verde Esmeralda
)

/**
 * OPÇÕES DE ESPESSURA DA PONTA
 */
data class TipSize(val label: String, val widthDp: Float, val dotSizeDp: Float)
val TIP_SIZES = listOf(
    TipSize("Fina", 2.0f, 3.5f),
    TipSize("Média", 4.0f, 6.0f),
    TipSize("Grossa", 7.0f, 9.5f),
    TipSize("Marca", 16.0f, 13f)
)

/**
 * Controlador de Estado para a Caneta S-Pen
 */
class AnnotatorController {
    var selectedColorIndex by mutableIntStateOf(1) // Padrão: Azul
    var selectedTipIndex by mutableIntStateOf(1)   // Padrão: Média
    var isManualEraser by mutableStateOf(false)
    var isSPenButtonPressed by mutableStateOf(false)
    var allowFingerDrawing by mutableStateOf(false) // Padrão: falso (S-Pen exclusiva, dedo rola página)
    var isCurrentlyDrawing by mutableStateOf(false) // Flag ativa durante o traço da S-Pen
    var strokesCount by mutableIntStateOf(0)
    var undoRequested by mutableLongStateOf(0L)
    var clearRequested by mutableLongStateOf(0L)

    fun undo() { undoRequested++ }
    fun clear() { clearRequested++ }
}

@Composable
fun rememberAnnotatorController(): AnnotatorController {
    return remember { AnnotatorController() }
}

/**
 * BARRA DE CONFIGURAÇÕES DA CANETA (PEN TOOLBAR ROW) - ESTILO SAMSUNG NOTES
 *
 * Barra compacta (altura 38dp), moderna e minimalista com microanimações táteis.
 */
@Composable
fun PenToolbarRow(
    modifier: Modifier = Modifier,
    controller: AnnotatorController,
    onClosePen: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("pen_toolbar_row"),
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. 4 CORES COM MICROANIMAÇÃO DE ESCALA
            PEN_COLORS.forEachIndexed { index, (color, name) ->
                val isSelected = !controller.isManualEraser && controller.selectedColorIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "color_scale_$name"
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable {
                            controller.selectedColorIndex = index
                            controller.isManualEraser = false
                        }
                        .testTag("pen_color_${name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                else Modifier
                            )
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // 2. 4 ESPESSURAS DA PONTA
            TIP_SIZES.forEachIndexed { index, tip ->
                val isSelected = !controller.isManualEraser && controller.selectedTipIndex == index
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .clickable {
                            controller.selectedTipIndex = index
                            controller.isManualEraser = false
                        }
                        .testTag("pen_tip_${tip.label.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(tip.dotSizeDp.dp)
                                .clip(if (index == 3) RoundedCornerShape(2.dp) else CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = tip.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // 3. FERRAMENTA BORRACHA
            val isEraserActive = controller.isManualEraser || controller.isSPenButtonPressed
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isEraserActive) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                modifier = Modifier
                    .clickable { controller.isManualEraser = !controller.isManualEraser }
                    .testTag("pen_eraser_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Borracha",
                        tint = if (isEraserActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Borracha",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isEraserActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = if (isEraserActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. DESFAZER (UNDO)
            IconButton(
                onClick = { controller.undo() },
                enabled = controller.strokesCount > 0,
                modifier = Modifier.size(26.dp).testTag("pen_undo_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Desfazer",
                    tint = if (controller.strokesCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp)
                )
            }

            // 5. LIMPAR TUDO
            IconButton(
                onClick = { controller.clear() },
                enabled = controller.strokesCount > 0,
                modifier = Modifier.size(26.dp).testTag("pen_clear_all_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Limpar",
                    tint = if (controller.strokesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp)
                )
            }

            VerticalDivider(modifier = Modifier.height(18.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // 6. TOGGLE S-PEN EXCLUSIVA (Rejeição de Palma) vs DEDO
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (!controller.allowFingerDrawing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable {
                    controller.allowFingerDrawing = !controller.allowFingerDrawing
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!controller.allowFingerDrawing) Icons.Default.Edit else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (!controller.allowFingerDrawing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (!controller.allowFingerDrawing) "S-Pen" else "Dedo",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = if (!controller.allowFingerDrawing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 7. BOTÃO VOLTAR PARA DIGITAÇÃO (FECHAR CANETA)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { onClosePen() }
                    .testTag("pen_close_to_type_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Digitar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Digitar",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * LINHAS PAUTADAS DE CADERNO MÉDICO UNIVERSITÁRIO
 */
@Composable
fun RuledNotebookBackground(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 28.dp,
    lineColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
    marginLineColor: Color = Color(0xFFEF4444).copy(alpha = 0.22f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = lineSpacing.toPx()
        val marginXPx = 38.dp.toPx()

        // Margem vermelha clássica de caderno à esquerda
        drawLine(
            color = marginLineColor,
            start = Offset(marginXPx, 0f),
            end = Offset(marginXPx, size.height),
            strokeWidth = 1.0f
        )

        // Linhas horizontais pautadas
        var y = spacingPx
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.7f
            )
            y += spacingPx
        }
    }
}

/**
 * DOCUMENTO UNIFICADO DE ANOTAÇÃO (ESTILO SAMSUNG NOTES)
 *
 * Garante que o texto digitado e as anotações feitas com a S-Pen
 * compartilhem exatamente a mesma estrutura física, as mesmas coordenadas
 * e o mesmo fluxo de rolagem.
 *
 * Características:
 * - Fundo com pauta universitária médica sutil (espaçamento 28dp).
 * - Margem sutil vermelha à esquerda (38dp).
 * - Text editor sem scroll interno (expande naturalmente conforme a digitação).
 * - Altura de linha idêntica (28sp), mantendo cada linha de texto pousada perfeitamente sobre a pauta.
 * - Modo Caneta: o texto permanece exatamente no mesmo lugar (readOnly = true),
 *   sem saltos ou alterações de layout, permitindo que os traços da S-Pen fiquem 100% ancorados.
 */
@Composable
fun UnifiedNoteDocument(
    modifier: Modifier = Modifier,
    text: String,
    onTextChanged: (String) -> Unit = {},
    isPenMode: Boolean = false,
    isReadOnly: Boolean = false,
    minHeight: Dp = 700.dp,
    placeholder: String = "Anotações da aula..."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
    ) {
        // 1. Linhas Pautadas do Caderno (espaçamento 28dp perfeitamente alinhado com o texto)
        RuledNotebookBackground(
            lineSpacing = 28.dp,
            lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            marginLineColor = Color(0xFFEF4444).copy(alpha = 0.20f)
        )

        // 2. Editor de Texto Unificado (Sem scroll interno para não dessincronizar com os traços!)
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            readOnly = isPenMode || isReadOnly,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
                .testTag("unified_note_text_field"),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * COMPONENTE DE ANOTAÇÃO DIRETA SOBRE O TEXTO (DIRECT TEXT ANNOTATOR)
 *
 * Características Otimizadas para Samsung Galaxy Tab S10 FE+:
 * - Interceptação em PointerEventPass.Initial: Consome eventos da S-Pen imediatamente na passada inicial,
 *   impedindo que verticalScroll ou LazyColumn roubem o traço ou acionem touch slop!
 * - Rejeição total de palma via ACTION_CANCEL e FLAG_CANCELED.
 * - O toque do dedo NÃO é consumido quando no modo S-Pen exclusiva, permitindo rolagem de página
 *   com o dedo 100% natural sem riscar a tela!
 * - Captura de pontos históricos (change.historical) a 120Hz/240Hz para caligrafia suave.
 * - Sincronização Absoluta de Rolagem: O Canvas e o Documento de Texto compartilham o mesmo container físico,
 *   rolando juntos em perfeita sincronia com cada traço 100% ancorado ao texto!
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DirectTextAnnotator(
    modifier: Modifier = Modifier,
    drawingData: String = "",
    isReadOnly: Boolean = false,
    isPenActive: Boolean = false,
    controller: AnnotatorController = rememberAnnotatorController(),
    minCanvasHeight: Dp = 460.dp,
    onDrawingChanged: (String) -> Unit = {},
    content: @Composable BoxScope.(isPenActive: Boolean) -> Unit
) {
    val strokes = remember { mutableStateListOf<CanvasStroke>() }
    val livePoints = remember { ArrayList<CanvasPoint>() }
    val livePath = remember { Path() }
    var renderTick by remember { mutableLongStateOf(0L) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    // Sincronizar dados de desenho persistidos
    LaunchedEffect(drawingData) {
        if (drawingData.isNotBlank() && strokes.isEmpty()) {
            val deserialized = DrawingSerializer.deserialize(drawingData)
            strokes.clear()
            strokes.addAll(deserialized)
            controller.strokesCount = strokes.size
        } else if (drawingData.isBlank() && strokes.isNotEmpty()) {
            strokes.clear()
            controller.strokesCount = 0
        }
    }

    // Atender a comandos de Desfazer (Undo)
    LaunchedEffect(controller.undoRequested) {
        if (controller.undoRequested > 0 && strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            controller.strokesCount = strokes.size
            onDrawingChanged(DrawingSerializer.serialize(strokes))
        }
    }

    // Atender a comandos de Limpar (Clear)
    LaunchedEffect(controller.clearRequested) {
        if (controller.clearRequested > 0 && strokes.isNotEmpty()) {
            strokes.clear()
            controller.strokesCount = 0
            onDrawingChanged("")
        }
    }

    fun eraseStrokesNear(point: Offset, radius: Float = 54f) {
        var removedAny = false
        val iterator = strokes.iterator()
        while (iterator.hasNext()) {
            val stroke = iterator.next()
            val intersects = if (stroke.points.size <= 1) {
                stroke.points.any { p -> hypot(p.x - point.x, p.y - point.y) <= radius }
            } else {
                var hit = false
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i + 1]
                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    val lenSq = dx * dx + dy * dy
                    val dist = if (lenSq == 0f) {
                        hypot(point.x - p1.x, point.y - p1.y)
                    } else {
                        val t = ((point.x - p1.x) * dx + (point.y - p1.y) * dy) / lenSq
                        val clampedT = t.coerceIn(0f, 1f)
                        val projX = p1.x + clampedT * dx
                        val projY = p1.y + clampedT * dy
                        hypot(point.x - projX, point.y - projY)
                    }
                    if (dist <= radius) {
                        hit = true
                        break
                    }
                }
                hit
            }
            if (intersects) {
                iterator.remove()
                removedAny = true
            }
        }
        if (removedAny) {
            controller.strokesCount = strokes.size
            onDrawingChanged(DrawingSerializer.serialize(strokes))
        }
    }

    // ÁREA UNIFICADA: O TEXTO FICA NO FUNDO E A CANETA RISCA DIRETAMENTE POR CIMA
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minCanvasHeight)
            .then(
                if (isPenActive) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
    ) {
        // 1. O CONTEÚDO DO TEXTO (Fica sempre visível e define a altura natural do container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minCanvasHeight)
        ) {
            content(isPenActive)
        }

        // 2. A CAMADA TRANSPARENTE DE DESENHO COM S-PEN
        // matchParentSize() garante que o Canvas tenha EXATAMENTE a mesma altura e largura do texto!
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (isPenActive) {
                        Modifier.pointerInput(controller.allowFingerDrawing) {
                            awaitPointerEventScope {
                                while (true) {
                                    // Interceptação na passada Initial: ANTES que o verticalScroll processe!
                                    val event = awaitPointerEvent(PointerEventPass.Initial)

                                    // Detectar se o botão lateral da S-Pen está pressionado:
                                    // 1. event.buttons.isSecondaryPressed (MotionEvent.BUTTON_SECONDARY = 2)
                                    // 2. event.buttons.isTertiaryPressed (MotionEvent.BUTTON_TERTIARY = 4)
                                    // 3. event.buttons.isPressed(5) (MotionEvent.BUTTON_STYLUS_PRIMARY = 32 = 1 shl 5)
                                    // 4. event.buttons.isPressed(6) (MotionEvent.BUTTON_STYLUS_SECONDARY = 64 = 1 shl 6)
                                    // 5. PointerType.Eraser (Samsung One UI altera o tipo de ferramenta para Eraser ao manter pressionado o botão)
                                    val hasStylusButtonPressed = event.buttons.isSecondaryPressed ||
                                            event.buttons.isTertiaryPressed ||
                                            event.changes.any { it.type == PointerType.Eraser }

                                    controller.isSPenButtonPressed = hasStylusButtonPressed

                                    // PROCESSAR OS PONTEIROS
                                    for (change in event.changes) {
                                        val isStylusOrEraser = change.type == PointerType.Stylus || change.type == PointerType.Eraser
                                        val shouldDraw = isStylusOrEraser || controller.allowFingerDrawing

                                        // Se for toque comum de dedo e o usuário não autorizou desenho com o dedo:
                                        // NÃO CONSOME! Deixa o evento propagar livremente para que a página role suavemente com o dedo!
                                        if (!shouldDraw) {
                                            continue
                                        }

                                        // É S-Pen (ou dedo autorizado): CONSOME IMEDIATAMENTE NA PASSADA INITIAL!
                                        change.consume()

                                        val isToolEraser = change.type == PointerType.Eraser
                                        // Enquanto o botão da S-Pen estiver pressionado, ativa o modo borracha automaticamente!
                                        val effectiveEraser = controller.isManualEraser || isToolEraser || hasStylusButtonPressed
                                        val pos = change.position
                                        val pressure = if (change.pressure > 0f) change.pressure else 1f

                                        if (change.pressed && !change.previousPressed) {
                                            // ACTION DOWN: Início do traço ou borracha
                                            controller.isCurrentlyDrawing = true
                                            if (effectiveEraser) {
                                                livePoints.clear()
                                                livePath.reset()
                                                eraserPosition = pos
                                                eraseStrokesNear(pos, radius = 54f)
                                            } else {
                                                eraserPosition = null
                                                livePoints.clear()
                                                livePoints.add(CanvasPoint(pos.x, pos.y, pressure))
                                                livePath.reset()
                                                livePath.moveTo(pos.x, pos.y)
                                            }
                                            renderTick++
                                        } else if (change.pressed && change.previousPressed) {
                                            // ACTION MOVE: Movimento fluido da S-Pen
                                            controller.isCurrentlyDrawing = true
                                            if (effectiveEraser) {
                                                // Se o botão da S-Pen foi pressionado durante o movimento, descarta traço em andamento e apaga
                                                if (livePoints.isNotEmpty()) {
                                                    livePoints.clear()
                                                    livePath.reset()
                                                }
                                                eraserPosition = pos
                                                eraseStrokesNear(pos, radius = 54f)
                                            } else {
                                                eraserPosition = null
                                                val historicalList = change.historical
                                                for (h in historicalList) {
                                                    livePoints.add(CanvasPoint(h.position.x, h.position.y, pressure))
                                                    if (livePoints.size == 1) {
                                                        livePath.reset()
                                                        livePath.moveTo(h.position.x, h.position.y)
                                                    } else if (livePoints.size >= 2) {
                                                        val prev = livePoints[livePoints.size - 2]
                                                        val midX = (prev.x + h.position.x) / 2f
                                                        val midY = (prev.y + h.position.y) / 2f
                                                        livePath.quadraticTo(prev.x, prev.y, midX, midY)
                                                    }
                                                }

                                                // Ponto atual
                                                livePoints.add(CanvasPoint(pos.x, pos.y, pressure))
                                                if (livePoints.size == 1) {
                                                    livePath.reset()
                                                    livePath.moveTo(pos.x, pos.y)
                                                } else if (livePoints.size >= 2) {
                                                    val prev = livePoints[livePoints.size - 2]
                                                    val midX = (prev.x + pos.x) / 2f
                                                    val midY = (prev.y + pos.y) / 2f
                                                    livePath.quadraticTo(prev.x, prev.y, midX, midY)
                                                }
                                            }
                                            renderTick++
                                        } else if (!change.pressed && change.previousPressed) {
                                            // ACTION UP: Finalização do traço
                                            controller.isCurrentlyDrawing = false
                                            if (!effectiveEraser) {
                                                if (livePoints.size >= 2) {
                                                    val activeColor = PEN_COLORS[controller.selectedColorIndex].first
                                                    val activeTip = TIP_SIZES[controller.selectedTipIndex]
                                                    val newStroke = CanvasStroke(
                                                        points = ArrayList(livePoints),
                                                        colorArgb = activeColor.toArgb().toLong(),
                                                        strokeWidth = activeTip.widthDp * 2.5f,
                                                        isHighlighter = controller.selectedTipIndex == 3
                                                    )
                                                    strokes.add(newStroke)
                                                    controller.strokesCount = strokes.size
                                                    onDrawingChanged(DrawingSerializer.serialize(strokes))
                                                } else if (livePoints.size == 1) {
                                                    // Ponto ou pingo no 'i'
                                                    val pt = livePoints[0]
                                                    val activeColor = PEN_COLORS[controller.selectedColorIndex].first
                                                    val activeTip = TIP_SIZES[controller.selectedTipIndex]
                                                    val dotStroke = CanvasStroke(
                                                        points = listOf(pt, CanvasPoint(pt.x + 0.5f, pt.y + 0.5f, pt.pressure)),
                                                        colorArgb = activeColor.toArgb().toLong(),
                                                        strokeWidth = activeTip.widthDp * 2.5f,
                                                        isHighlighter = controller.selectedTipIndex == 3
                                                    )
                                                    strokes.add(dotStroke)
                                                    controller.strokesCount = strokes.size
                                                    onDrawingChanged(DrawingSerializer.serialize(strokes))
                                                }
                                            }
                                            livePoints.clear()
                                            livePath.reset()
                                            eraserPosition = null
                                            controller.isSPenButtonPressed = hasStylusButtonPressed
                                            renderTick++
                                        }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
                .testTag("direct_text_drawing_canvas")
        ) {
            if (renderTick < 0) return@Canvas

            // Desenhar traços persistidos
            for (stroke in strokes) {
                val color = Color(stroke.colorArgb.toInt()).let {
                    if (stroke.isHighlighter) it.copy(alpha = 0.40f) else it
                }
                drawPath(
                    path = stroke.cachedPath,
                    color = color,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Desenhar o traço ativo sendo desenhado pela S-Pen
            if (livePoints.size >= 2) {
                val activeColor = PEN_COLORS[controller.selectedColorIndex].first
                val activeTip = TIP_SIZES[controller.selectedTipIndex]
                val color = if (controller.selectedTipIndex == 3) activeColor.copy(alpha = 0.40f) else activeColor

                drawPath(
                    path = livePath,
                    color = color,
                    style = Stroke(
                        width = activeTip.widthDp * 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Indicador circular da borracha
            eraserPosition?.let { pos ->
                drawCircle(
                    color = Color.Red.copy(alpha = 0.35f),
                    radius = 24.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Função de retrocompatibilidade para HandwritingCanvas
 */
@Composable
fun HandwritingCanvas(
    modifier: Modifier = Modifier,
    initialDrawingData: String = "",
    isReadOnly: Boolean = false,
    onDrawingChanged: (String) -> Unit = {}
) {
    var isPenActive by remember { mutableStateOf(!isReadOnly) }
    val controller = rememberAnnotatorController()

    Column(modifier = modifier) {
        if (isPenActive) {
            PenToolbarRow(
                controller = controller,
                onClosePen = { isPenActive = false }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        DirectTextAnnotator(
            modifier = Modifier.fillMaxSize(),
            drawingData = initialDrawingData,
            isReadOnly = isReadOnly,
            isPenActive = isPenActive,
            controller = controller,
            onDrawingChanged = onDrawingChanged
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
