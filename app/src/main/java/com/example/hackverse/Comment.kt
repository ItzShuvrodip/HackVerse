package com.example.hackverse

data class Comment(
    val commentID: String,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Long
)
{
    constructor() : this("","", "", "", 0)
}
