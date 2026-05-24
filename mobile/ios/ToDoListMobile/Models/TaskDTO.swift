import Foundation

enum TaskPriority: String, Codable {
    case urgent = "URGENT"
    case important = "IMPORTANT"
    case normal = "NORMAL"
}

enum TaskCategory: String, Codable {
    case work = "WORK"
    case home = "HOME"
    case study = "STUDY"
    case other = "OTHER"
}

struct TaskDTO: Identifiable, Codable {
    let id: String
    let title: String
    let description: String
    let startTime: String
    let endTime: String
    let priority: TaskPriority
    let category: TaskCategory
    let completed: Bool
    let overdue: Bool
    let createdAt: String
    let updatedAt: String
    let ownerUserId: String
    let sortIndex: Int
    let recurrenceRule: String
    let reminderOffsetMinutes: Int
    let recurrenceEnd: String?
    let deletedAt: String?
}
