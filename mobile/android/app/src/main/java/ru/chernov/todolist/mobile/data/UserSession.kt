package ru.chernov.todolist.mobile.data

data class UserSession(
    val userId: String,
    val username: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)
