package ru.kubsu.borshchevyk.core.model.domain

data class DomainCallEvent(
    val type: String,
    val callId: String,
    val initiatorId: String,
    val participants: List<String>
)
