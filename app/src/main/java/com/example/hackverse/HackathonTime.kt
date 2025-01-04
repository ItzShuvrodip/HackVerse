package com.example.hackverse

data class HackathonTime(
    val UID: String,
    val timestamp: Long
)
{
    constructor() : this("",0)
}