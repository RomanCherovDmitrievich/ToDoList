import SwiftUI

struct LoginView: View {
    @State private var identifier = ""
    @State private var password = ""

    let onLogin: () -> Void
    let onRegister: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("ToDoList Mobile")
                .font(.largeTitle.bold())
            Text("One account, shared tasks, offline-first sync.")
                .foregroundStyle(.secondary)

            TextField("Login or email", text: $identifier)
                .textFieldStyle(.roundedBorder)
            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            Button("Sign in", action: onLogin)
                .buttonStyle(.borderedProminent)
            Button("Create account", action: onRegister)
                .buttonStyle(.bordered)

            Spacer()
        }
        .padding(24)
    }
}
