package com.mediabuyer.bot

data class PostItem(
    val id: Int,
    val title: String,
    val content: String,
    val groupUrl: String,
    val scheduledAt: Long
)
