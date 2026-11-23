package com.example

import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatRole
import com.example.data.ai.Dera3AiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Dera3AiChatTest {

    @Test
    fun buildSdkHistoryFiltersIncompleteAndLeadingAssistantMessages() {
        val messages = listOf(
            ChatMessage(id = "0", role = ChatRole.ASSISTANT, text = "Orphan intro"),
            ChatMessage(id = "1", role = ChatRole.USER, text = "Is bit.ly safe?"),
            ChatMessage(id = "2", role = ChatRole.ASSISTANT, text = "Avoid unknown short links."),
            ChatMessage(id = "3", role = ChatRole.USER, text = "What if I clicked it?"),
            ChatMessage(id = "4", role = ChatRole.ASSISTANT, text = "", isStreaming = true),
            ChatMessage(id = "5", role = ChatRole.ASSISTANT, text = "ignored", errorMessage = "timeout"),
        )

        val history = Dera3AiRepository.buildSdkHistory(messages)
        assertEquals(3, history.size)
        assertEquals("user", history[0].role)
        assertEquals("model", history[1].role)
        assertEquals("user", history[2].role)
    }

    @Test
    fun buildSdkHistoryCapsAtMaxTurnsAndHandlesEmptyInput() {
        assertTrue(Dera3AiRepository.buildSdkHistory(emptyList()).isEmpty())

        val manyTurns = (0 until 20).map { idx ->
            ChatMessage(
                id = "msg_$idx",
                role = if (idx % 2 == 0) ChatRole.USER else ChatRole.ASSISTANT,
                text = "Turn $idx",
            )
        }
        val capped = Dera3AiRepository.buildSdkHistory(manyTurns, maxTurns = 6)
        assertEquals(6, capped.size)
        assertEquals("user", capped.first().role)
    }
}
