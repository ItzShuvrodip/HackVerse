package com.example.hackverse

data class ChatData(
    val commentID: String,
    val userId: String,
    val teamName:String,
    val username: String,
    val text: String,
    val timestamp: Long
)
{
    constructor() : this("","", "","", "", 0)
}
