package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.entity.SlideBlockEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfHelper {

    /**
     * Extrai todas as páginas de um arquivo PDF como imagens Bitmap salvas no disco.
     * Retorna a lista de caminhos absolutos dos arquivos de imagem gerados.
     */
    fun extractPdfSlides(context: Context, pdfUri: Uri): List<String> {
        val slidePaths = mutableListOf<String>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return emptyList()
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            val slidesDir = File(context.filesDir, "slides_${System.currentTimeMillis()}").apply {
                if (!exists()) mkdirs()
            }

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val slideFile = File(slidesDir, "slide_page_${i + 1}.jpg")
                FileOutputStream(slideFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()

                slidePaths.add(slideFile.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer?.close()
            pfd?.close()
        }

        return slidePaths
    }

    /**
     * Exporta a anotação/aula completa.
     * No modo SLIDES: Exporta em formato paisagem (A4 Landscape) com SLIDE À ESQUERDA e TEXTO À DIREITA.
     * No modo FREE: Exporta em formato A4 vertical elegante.
     */
    fun exportNoteToPdf(
        context: Context,
        title: String,
        subjectName: String,
        mode: String,
        freeTextContent: String,
        drawingData: String = "",
        slideBlocks: List<SlideBlockEntity>
    ): Uri? {
        val document = PdfDocument()
        var pageNumber = 1

        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = TextPaint().apply {
            color = android.graphics.Color.rgb(71, 85, 105)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val notesHeaderPaint = TextPaint().apply {
            color = android.graphics.Color.rgb(29, 78, 216) // Azul Royal
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val footerPaint = TextPaint().apply {
            color = android.graphics.Color.rgb(148, 163, 184)
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = android.graphics.Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val bgBoxPaint = Paint().apply {
            color = android.graphics.Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        if (mode == "FREE" || slideBlocks.isEmpty()) {
            // Documento de texto corrido em formato retrato (595 x 842)
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Cabeçalho
            canvas.drawText(title.ifBlank { "Resumo de Estudo" }, 40f, 48f, titlePaint)
            canvas.drawText("Estudos do Sergio  •  Disciplina: $subjectName  •  Data: $dateStr", 40f, 66f, subtitlePaint)
            canvas.drawLine(40f, 76f, (pageWidth - 40).toFloat(), 76f, dividerPaint)

            // Texto com quebra automática
            val contentWidth = pageWidth - 80
            val staticLayout = StaticLayout.Builder.obtain(
                freeTextContent,
                0,
                freeTextContent.length,
                bodyPaint,
                contentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1f)
                .build()

            canvas.save()
            canvas.translate(40f, 95f)
            staticLayout.draw(canvas)
            canvas.restore()

            // Desenhar anotações manuscritas/riscos da S-Pen DIRETAMENTE SOBRE O TEXTO
            if (drawingData.isNotBlank()) {
                val availableHeight = (pageHeight - 95f - 40f).coerceAtLeast(300f)
                drawHandwritingStrokes(canvas, drawingData, contentWidth.toFloat(), availableHeight, 40f, 95f)
            }

            // Rodapé
            canvas.drawText("Estudos do Sergio  •  Página 1", 40f, (pageHeight - 25).toFloat(), footerPaint)

            document.finishPage(page)
        } else {
            // MODO AULA COM SLIDES: FORMATO PAISAGEM LADO-A-LADO (842 x 595)
            // Slide à esquerda (50%) e Texto de anotações à direita (50%)
            val pageWidth = 842
            val pageHeight = 595

            for ((idx, block) in slideBlocks.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // CABEÇALHO SUPERIOR
                val slideTitle = "$title — Slide ${idx + 1} de ${slideBlocks.size}"
                canvas.drawText(slideTitle, 35f, 38f, titlePaint)
                canvas.drawText("Estudos do Sergio  •  Disciplina: $subjectName  •  Data: $dateStr", 35f, 54f, subtitlePaint)
                canvas.drawLine(35f, 64f, (pageWidth - 35).toFloat(), 64f, dividerPaint)

                // COLUNA DA ESQUERDA: SLIDE (X: 35 até 425 -> Largura: 390 pt)
                val leftColX = 35f
                val leftColY = 80f
                val leftColWidth = 390f
                val leftColHeight = 470f

                if (!block.imagePath.isNullOrBlank() && File(block.imagePath).exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(block.imagePath)
                    if (bitmap != null) {
                        val scale = (leftColWidth / bitmap.width).coerceAtMost(leftColHeight / bitmap.height)
                        val destWidth = (bitmap.width * scale).toInt()
                        val destHeight = (bitmap.height * scale).toInt()

                        val scaledBmp = Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, true)
                        val posX = leftColX + (leftColWidth - destWidth) / 2f
                        val posY = leftColY + (leftColHeight - destHeight) / 2f

                        // Fundo e moldura para o slide
                        canvas.drawRoundRect(
                            RectF(posX - 2f, posY - 2f, posX + destWidth + 2f, posY + destHeight + 2f),
                            6f, 6f, borderPaint
                        )
                        canvas.drawBitmap(scaledBmp, posX, posY, null)
                    }
                } else {
                    // Placeholder visual se não houver imagem
                    val rect = RectF(leftColX, leftColY + 40f, leftColX + leftColWidth, leftColY + 340f)
                    canvas.drawRoundRect(rect, 8f, 8f, bgBoxPaint)
                    canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
                    canvas.drawText("Slide ${idx + 1}", leftColX + 160f, leftColY + 190f, notesHeaderPaint)
                }

                // DIVISOR VERTICAL CENTRAL
                canvas.drawLine(440f, 75f, 440f, 555f, dividerPaint)

                // COLUNA DA DIREITA: TEXTO DAS ANOTAÇÕES (X: 455 até 807 -> Largura: 352 pt)
                val rightColX = 455f
                val rightColY = 80f
                val rightColWidth = 352

                // Cabeçalho da coluna de anotações
                canvas.drawText("Anotações da Aula", rightColX, rightColY + 12f, notesHeaderPaint)
                canvas.drawLine(rightColX, rightColY + 20f, (pageWidth - 35).toFloat(), rightColY + 20f, dividerPaint)

                val textContent = if (block.notesContent.isNotBlank()) {
                    block.notesContent
                } else {
                    if (block.drawingData.isNotBlank()) "" else "(Nenhuma anotação registrada para este slide)"
                }

                val staticLayout = StaticLayout.Builder.obtain(
                    textContent,
                    0,
                    textContent.length,
                    bodyPaint,
                    rightColWidth
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(3f, 1f)
                    .build()

                canvas.save()
                canvas.translate(rightColX, rightColY + 32f)
                staticLayout.draw(canvas)
                canvas.restore()

                // Desenho de anotações manuscritas/riscos deste slide DIRETAMENTE SOBRE O TEXTO
                if (block.drawingData.isNotBlank()) {
                    val availableHeight = (pageHeight - rightColY - 50f).coerceAtLeast(240f)
                    drawHandwritingStrokes(canvas, block.drawingData, rightColWidth.toFloat(), availableHeight, rightColX, rightColY + 32f)
                }

                // RODAPÉ
                canvas.drawText(
                    "Estudos do Sergio  •  Slide ${idx + 1}/${slideBlocks.size}",
                    (pageWidth - 220).toFloat(),
                    (pageHeight - 20).toFloat(),
                    footerPaint
                )

                document.finishPage(page)
            }
        }

        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        return try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun drawHandwritingStrokes(
        canvas: Canvas,
        drawingData: String,
        targetWidth: Float,
        targetHeight: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        if (drawingData.isBlank()) return
        val strokes = com.example.ui.components.DrawingSerializer.deserialize(drawingData)
        if (strokes.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (stroke in strokes) {
            for (p in stroke.points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
        }
        if (minX >= maxX || minY >= maxY) return

        val drawWidth = (maxX - minX).coerceAtLeast(80f)
        val drawHeight = (maxY - minY).coerceAtLeast(80f)
        val scale = (targetWidth / drawWidth).coerceAtMost(targetHeight / drawHeight).coerceAtMost(0.95f)

        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val paint = Paint().apply {
                color = stroke.colorArgb.toInt()
                if (stroke.isHighlighter) {
                    alpha = 110
                }
                strokeWidth = (stroke.strokeWidth * scale).coerceAtLeast(1.5f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            val path = android.graphics.Path()
            val p0 = stroke.points[0]
            val startX = offsetX + (p0.x - minX) * scale
            val startY = offsetY + (p0.y - minY) * scale
            path.moveTo(startX, startY)

            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val cur = stroke.points[i]
                val midX = offsetX + ((prev.x + cur.x) / 2f - minX) * scale
                val midY = offsetY + ((prev.y + cur.y) / 2f - minY) * scale
                val prevX = offsetX + (prev.x - minX) * scale
                val prevY = offsetY + (prev.y - minY) * scale
                path.quadTo(prevX, prevY, midX, midY)
            }
            canvas.drawPath(path, paint)
        }
    }
}
