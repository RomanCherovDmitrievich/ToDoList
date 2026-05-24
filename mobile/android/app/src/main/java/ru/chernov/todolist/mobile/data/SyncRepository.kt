package ru.chernov.todolist.mobile.data

interface AuthRepository {
    suspend fun login(identifier: String, password: String): UserSession
    suspend fun register(username: String, email: String, password: String): UserSession
    suspend fun requestPasswordReset(identifier: String)
    suspend fun confirmPasswordReset(identifier: String, code: String, newPassword: String)
}

interface SyncRepository {
    suspend fun pullTasks(updatedAfter: String?): List<TaskDto>
    suspend fun pushTasks(tasks: List<TaskDto>)
}
