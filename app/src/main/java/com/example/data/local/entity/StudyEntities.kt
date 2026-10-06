package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val mode: String = "FREE", // "FREE" ou "SLIDES"
    val freeTextContent: String = "",
    val drawingData: String = "", // Traços de caneta / anotações S-Pen serializados
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "slide_blocks")
data class SlideBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val pageIndex: Int = 0,
    val imagePath: String? = null,
    val notesContent: String = "",
    val drawingData: String = "" // Traços de caneta / anotações S-Pen deste slide
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long = 0,
    val subjectName: String = "",
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val explanation: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long = 0,
    val subjectName: String = "",
    val front: String,
    val back: String,
    val createdAt: Long = System.currentTimeMillis()
)
