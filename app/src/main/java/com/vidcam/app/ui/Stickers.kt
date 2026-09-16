package com.vidcam.app.ui

/** Stickers PNG con alfa incluidos en `assets/stickers`. */
val BUILT_IN_STICKERS: List<String> = listOf(
    "stickers/heart.png",
    "stickers/star.png",
    "stickers/sparkle.png",
    "stickers/bubble.png",
    "stickers/ring.png",
    "stickers/arrow.png",
    "stickers/check.png",
)

fun stickerAssetUri(path: String): String = "asset://$path"
