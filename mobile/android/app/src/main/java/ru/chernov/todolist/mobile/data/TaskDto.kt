package ru.chernov.todolist.mobile.data

data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val priority: PriorityDto,
    val category: CategoryDto,
    val completed: Boolean,
    val overdue: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val ownerUserId: String,
    val sortIndex: Int,
    val recurrenceRule: String,
    val reminderOffsetMinutes: Int,
    val recurrenceEnd: String?,
    val deletedAt: String?
)

enum class PriorityDto {
    URGENT,
    IMPORTANT,
    NORMAL
}

enum class CategoryDto {
    WORK,
    HOME,
    STUDY,
    OTHER
}
