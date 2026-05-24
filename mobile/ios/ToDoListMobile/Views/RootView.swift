import SwiftUI

struct RootView: View {
    @State private var isAuthenticated = false

    var body: some View {
        Group {
            if isAuthenticated {
                CalendarView(onLogout: { isAuthenticated = false })
            } else {
                LoginView(
                    onLogin: { isAuthenticated = true },
                    onRegister: { isAuthenticated = true }
                )
            }
        }
    }
}
