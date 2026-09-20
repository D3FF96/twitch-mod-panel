package com.d3ff96.twitchmodpanel.data.remote

import com.d3ff96.twitchmodpanel.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/**
 * Simulates IRC/EventSub chat: seed messages + periodic fake arrivals.
 */
class ChatStub {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var pumpJob: Job? = null
    private var channelId: String = ""
    private val mutedUserIds = mutableSetOf<String>()

    private val sampleUsers = listOf(
        Triple("u1", "viewer_one", "#FF4500"),
        Triple("u2", "sub_fan", "#1E90FF"),
        Triple("u3", "lurker42", "#00FF7F"),
        Triple("u4", "mod_helper", "#9147FF"),
        Triple("u5", "new_chatter", "#FF69B4"),
    )

    private val sampleTexts = listOf(
        "PogChamp",
        "LUL nice play",
        "can we get a clip?",
        "KEKW",
        "gg",
        "hello chat!",
        "modCheck",
        "any mods online?",
        "7TV emotes when",
        "F in chat",
        "HYPE",
        "that's crazy",
    )

    fun connect(channelId: String) {
        this.channelId = channelId
        mutedUserIds.clear()
        _messages.value = seedMessages(channelId)
        pumpJob?.cancel()
        pumpJob = scope.launch {
            while (isActive) {
                delay(Random.nextLong(2_500, 5_500))
                appendRandom()
            }
        }
    }

    fun disconnect() {
        pumpJob?.cancel()
        pumpJob = null
        mutedUserIds.clear()
        _messages.value = emptyList()
    }

    fun sendLocal(channelId: String, text: String, asUser: String = "StubMod") {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            channelId = channelId,
            userId = "10001",
            userLogin = asUser.lowercase(),
            displayName = asUser,
            text = text,
            timestampMs = System.currentTimeMillis(),
            badges = listOf("moderator"),
            color = "#9147FF",
            isModerator = true,
        )
        _messages.value = _messages.value + msg
    }

    /** Posts a system / mod notice line into the chat feed. */
    fun appendSystemNotice(channelId: String, text: String) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            channelId = channelId,
            userId = "system",
            userLogin = "system",
            displayName = "Mod",
            text = text,
            timestampMs = System.currentTimeMillis(),
            badges = listOf("moderator"),
            color = "#ADADB8",
            isModerator = true,
        )
        _messages.value = (_messages.value + msg).takeLast(200)
    }

    fun markDeleted(messageId: String) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(isDeleted = true, text = "<message deleted>") else it
        }
    }

    fun muteUser(userId: String) {
        mutedUserIds.add(userId)
    }

    fun unmuteUser(userId: String) {
        mutedUserIds.remove(userId)
    }

    private fun seedMessages(channelId: String): List<ChatMessage> {
        val now = System.currentTimeMillis()
        return listOf(
            ChatMessage("m1", channelId, "u1", "viewer_one", "ViewerOne", "Welcome to the stub chat!", now - 60_000, listOf(), "#FF4500"),
            ChatMessage("m2", channelId, "u2", "sub_fan", "SubFan", "Loving the stream Kappa", now - 45_000, listOf("subscriber"), "#1E90FF", isSubscriber = true),
            ChatMessage("m3", channelId, "u4", "mod_helper", "ModHelper", "Keep it civil, folks", now - 30_000, listOf("moderator"), "#9147FF", isModerator = true),
            ChatMessage("m4", channelId, "u3", "lurker42", "Lurker42", "first time here, hi!", now - 15_000, listOf(), "#00FF7F"),
            ChatMessage("m5", channelId, "u5", "new_chatter", "NewChatter", "any 7TV emotes?", now - 5_000, listOf(), "#FF69B4"),
        )
    }

    private fun appendRandom() {
        if (channelId.isEmpty()) return
        val (uid, login, color) = sampleUsers.random()
        if (uid in mutedUserIds) return
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            channelId = channelId,
            userId = uid,
            userLogin = login,
            displayName = login.replaceFirstChar { it.uppercase() },
            text = sampleTexts.random(),
            timestampMs = System.currentTimeMillis(),
            color = color,
            isSubscriber = Random.nextBoolean(),
        )
        val next = (_messages.value + msg).takeLast(200)
        _messages.value = next
    }
}
