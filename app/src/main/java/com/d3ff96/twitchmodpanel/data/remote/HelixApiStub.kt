package com.d3ff96.twitchmodpanel.data.remote

import com.d3ff96.twitchmodpanel.domain.model.Channel
import com.d3ff96.twitchmodpanel.domain.model.TwitchUser
import kotlinx.coroutines.delay

/**
 * Stub stand-in for Twitch Helix REST (Retrofit would wrap this later).
 * Supplies sample moderated channels and user profile.
 */
class HelixApiStub {
    suspend fun getCurrentUser(): TwitchUser {
        delay(200)
        return TwitchUser(
            id = "10001",
            login = "stub_mod",
            displayName = "StubMod",
            profileImageUrl = null,
        )
    }

    suspend fun getModeratedChannels(): List<Channel> {
        delay(300)
        return SampleData.channels
    }

    suspend fun getStreamStatus(channelId: String): Channel {
        delay(100)
        return SampleData.channels.find { it.id == channelId }
            ?: Channel(id = channelId, login = "unknown", displayName = "Unknown")
    }
}

object SampleData {
    val channels = listOf(
        Channel(
            id = "20001",
            login = "xqc",
            displayName = "xQc",
            isLive = true,
            title = "JUST CHATTING — stub stream",
            viewerCount = 42_000,
            gameName = "Just Chatting",
        ),
        Channel(
            id = "20002",
            login = "pokimane",
            displayName = "pokimane",
            isLive = false,
            title = "Offline stub",
            viewerCount = 0,
            gameName = "",
        ),
        Channel(
            id = "20003",
            login = "shroud",
            displayName = "shroud",
            isLive = true,
            title = "Ranked — sample data",
            viewerCount = 18_500,
            gameName = "VALORANT",
        ),
        Channel(
            id = "20004",
            login = "lirik",
            displayName = "LIRIK",
            isLive = true,
            title = "Variety stream (stub)",
            viewerCount = 9_200,
            gameName = "Minecraft",
        ),
    )
}
