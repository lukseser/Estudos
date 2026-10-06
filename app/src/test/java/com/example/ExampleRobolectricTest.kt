package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.util.CsvHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Estudos do Sergio", appName)
  }

  @Test
  fun `parse sample questions csv correctly`() {
    val stream = ByteArrayInputStream(CsvHelper.SAMPLE_QUESTIONS_CSV.toByteArray(Charsets.UTF_8))
    val questions = CsvHelper.parseQuestionsCsv(stream)
    assertTrue(questions.isNotEmpty())
    assertEquals("Anatomia Humana", questions[0].subjectName)
    assertEquals("A", questions[0].correctAnswer)
  }

  @Test
  fun `parse sample flashcards csv correctly`() {
    val stream = ByteArrayInputStream(CsvHelper.SAMPLE_FLASHCARDS_CSV.toByteArray(Charsets.UTF_8))
    val flashcards = CsvHelper.parseFlashcardsCsv(stream)
    assertTrue(flashcards.isNotEmpty())
    assertEquals("Anatomia Humana", flashcards[0].subjectName)
  }
}
