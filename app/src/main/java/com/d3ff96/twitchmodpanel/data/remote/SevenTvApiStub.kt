package com.d3ff96.twitchmodpanel.data.remote

import com.d3ff96.twitchmodpanel.domain.model.Emote
import com.d3ff96.twitchmodpanel.domain.model.EmoteSource
import kotlinx.coroutines.delay

/**
 * Stub for 7TV REST API — sample emote set so UI can render names/urls offline.
 */
class SevenTvApiStub {
    suspend fun getGlobalEmotes(): List<Emote> {
        delay(150)
        return listOf(
            Emote("7tv_1", "CatJam", "https://cdn.7tv.app/emote/60ae7316f6a2c3b332d21139/2x.webp", EmoteSource.SEVENTV),
            Emote("7tv_2", "NODDERS", "https://cdn.7tv.app/emote/60ae8d2d88e2d8c66f17d2f9/2x.webp", EmoteSource.SEVENTV),
            Emote("7tv_3", "BASED", "https://cdn.7tv.app/emote/60aed5aa88e2d8c66f17d5f1/2x.webp", EmoteSource.SEVENTV),
            Emote("7tv_4", "Clueless", "https://cdn.7tv.app/emote/60af1c3b88e2d8c66f17d8a0/2x.webp", EmoteSource.SEVENTV),
        )
    }

    suspend fun getChannelEmotes(channelId: String): List<Emote> {
        delay(150)
        return listOf(
            Emote("7tv_ch_$channelId", "ChannelCool", "https://cdn.7tv.app/emote/placeholder/2x.webp", EmoteSource.SEVENTV),
            Emote("ffz_1", "OMEGALUL", "https://cdn.frankerfacez.com/emote/128054/2", EmoteSource.FFZ),
            Emote("bttv_1", "monkaS", "https://cdn.betterttv.net/emote/56e9f39db3d4ef265bae5b03/2x", EmoteSource.BTTV),
        )
    }
}
