import SwiftUI

struct CalendarView: View {
    let onLogout: () -> Void

    private let selectedDate = "2026-03-25"
    private let dayTasks = [
        "Prepare mobile sync API",
        "Connect login to shared account",
        "Review offline queue"
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Calendar")
                        .font(.largeTitle.bold())
                    Spacer()
                    Button("Logout", action: onLogout)
                }

                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.blue.opacity(0.08))
                    .frame(height: 280)
                    .overlay(alignment: .topLeading) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Large monthly calendar goes here")
                                .font(.headline)
                            Text("Selected date: \(selectedDate)")
                            Text("Connect this screen to local storage and sync queue.")
                                .foregroundStyle(.secondary)
                        }
                        .padding(20)
                    }

                Text("Tasks for \(selectedDate)")
                    .font(.title2.bold())

                ForEach(dayTasks, id: \.self) { task in
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(.secondarySystemBackground))
                        .frame(height: 64)
                        .overlay(alignment: .leading) {
                            Text(task)
                                .padding(.horizontal, 16)
                        }
                }
            }
            .padding(20)
        }
    }
}
