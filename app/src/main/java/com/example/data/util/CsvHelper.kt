package com.example.data.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.QuestionEntity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader

object CsvHelper {

    // Modelo de Questões
    const val SAMPLE_QUESTIONS_CSV =
        "Disciplina;Enunciado;OpcaoA;OpcaoB;OpcaoC;OpcaoD;Gabarito;Explicacao\n" +
        "Anatomia Humana;Qual o principal músculo responsável pela inspiração em repouso?;Diafragma;Intercostal interno;Pele;Trapézio;A;O diafragma realiza a maior parte do esforço ventilatório basal.\n" +
        "Fisiologia Geral;Qual a função principal da mielina nos axônios?;Condução saltatória rápida;Nutrição;Produção de glicose;Filtragem;A;A bainha de mielina isola eletricamente o axônio e acelera a condução do potencial de ação."

    // Modelo de Flashcards
    const val SAMPLE_FLASHCARDS_CSV =
        "Disciplina;Frente;Verso\n" +
        "Anatomia Humana;Quais os ossos do neurocrânio?;Frontal, Parietais (2), Occipital, Temporais (2), Esfenoide e Etmoide.\n" +
        "Farmacologia;Qual o mecanismo de ação dos beta-bloqueadores?;Antagonismo competitivo dos receptores beta-adrenérgicos, reduzindo frequência cardíaca e contratilidade."

    fun parseQuestionsCsv(inputStream: InputStream): List<QuestionEntity> {
        val result = mutableListOf<QuestionEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var isFirstLine = true

        reader.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    if (isFirstLine) {
                        isFirstLine = false
                        // Ignorar cabeçalho se contiver "Disciplina" ou "Enunciado"
                        if (trimmed.contains("Disciplina", ignoreCase = true) || trimmed.contains("Enunciado", ignoreCase = true)) {
                            return@forEach
                        }
                    }

                    val tokens = trimmed.split(";")
                    if (tokens.size >= 7) {
                        val disciplina = tokens[0].trim()
                        val enunciado = tokens[1].trim()
                        val opA = tokens[2].trim()
                        val opB = tokens[3].trim()
                        val opC = tokens[4].trim()
                        val opD = tokens[5].trim()
                        val gabarito = tokens[6].trim().uppercase()
                        val explicacao = if (tokens.size > 7) tokens[7].trim() else ""

                        val normalizedGabarito = when {
                            gabarito.startsWith("A") -> "A"
                            gabarito.startsWith("B") -> "B"
                            gabarito.startsWith("C") -> "C"
                            gabarito.startsWith("D") -> "D"
                            else -> "A"
                        }

                        if (enunciado.isNotBlank()) {
                            result.add(
                                QuestionEntity(
                                    subjectName = disciplina.ifBlank { "Geral" },
                                    prompt = enunciado,
                                    optionA = opA.ifBlank { "Opção A" },
                                    optionB = opB.ifBlank { "Opção B" },
                                    optionC = opC.ifBlank { "Opção C" },
                                    optionD = opD.ifBlank { "Opção D" },
                                    correctAnswer = normalizedGabarito,
                                    explanation = explicacao
                                )
                            )
                        }
                    }
                }
            }
        }
        return result
    }

    fun parseFlashcardsCsv(inputStream: InputStream): List<FlashcardEntity> {
        val result = mutableListOf<FlashcardEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var isFirstLine = true

        reader.useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    if (isFirstLine) {
                        isFirstLine = false
                        if (trimmed.contains("Disciplina", ignoreCase = true) || trimmed.contains("Frente", ignoreCase = true)) {
                            return@forEach
                        }
                    }

                    val tokens = trimmed.split(";")
                    if (tokens.size >= 3) {
                        val disciplina = tokens[0].trim()
                        val frente = tokens[1].trim()
                        val verso = tokens[2].trim()
                        if (frente.isNotBlank() && verso.isNotBlank()) {
                            result.add(
                                FlashcardEntity(
                                    subjectName = disciplina.ifBlank { "Geral" },
                                    front = frente,
                                    back = verso
                                )
                            )
                        }
                    } else if (tokens.size == 2) {
                        val frente = tokens[0].trim()
                        val verso = tokens[1].trim()
                        if (frente.isNotBlank() && verso.isNotBlank()) {
                            result.add(
                                FlashcardEntity(
                                    subjectName = "Geral",
                                    front = frente,
                                    back = verso
                                )
                            )
                        }
                    }
                }
            }
        }
        return result
    }

    fun createTemplateFile(context: Context, filename: String, content: String): Uri? {
        val dir = File(context.cacheDir, "templates").apply { if (!exists()) mkdirs() }
        val file = File(dir, filename)
        return try {
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
