import Foundation

protocol AuthRepository {
    func login(identifier: String, password: String) async throws -> UserSession
    func register(username: String, email: String, password: String) async throws -> UserSession
    func requestPasswordReset(identifier: String) async throws
    func confirmPasswordReset(identifier: String, code: String, newPassword: String) async throws
}

protocol SyncRepository {
    func pullTasks(updatedAfter: String?) async throws -> [TaskDTO]
    func pushTasks(_ tasks: [TaskDTO]) async throws
}
