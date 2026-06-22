package ru.kubsu.borshchevyk.feature.chat.common.model

enum class MediaType(val backendType: String, val displayName: String) {
    PHOTO("PHOTO", "Photo"),
    VIDEO("VIDEO", "Video"),
    CIRCLE("CIRCLE", "Circle"),
    VOICE("VOICE", "Voice"),
    FILE("FILE", "File")
}
