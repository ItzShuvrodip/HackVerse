package com.example.hackverse

data class Hackathon(
    val UID: String,
    val title: String,
    val organisingauthority: String,
    val organisingevent: String,
    val fee: String,
    val venue: String,
    val date: String,
    val lastdate: String,
    val size: String,
    val eligibility: String,
    val overview: String,
    val eligibilityd: String,
    val structure: String,
    val judging: String,
    val reward: String,
    val name: String,
    val number: String,
    val email: String,
    val ImURL:String,
    val likes:Int
)
{
    constructor() : this("","","", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",0)
}
