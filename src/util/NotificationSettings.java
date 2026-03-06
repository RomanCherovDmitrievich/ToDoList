package util;

import repository.DatabaseManager;

public final class NotificationSettings {
    private final boolean popupEnabled;
    private final boolean soundEnabled;
    private final boolean emailEnabled;
    private final int reminderOffsetMinutes;
    private final String emailTo;

    private NotificationSettings(boolean popupEnabled, boolean soundEnabled, boolean emailEnabled,
                                 int reminderOffsetMinutes, String emailTo) {
        this.popupEnabled = popupEnabled;
        this.soundEnabled = soundEnabled;
        this.emailEnabled = emailEnabled;
        this.reminderOffsetMinutes = reminderOffsetMinutes;
        this.emailTo = emailTo;
    }

    public static NotificationSettings load() {
        DatabaseManager db = DatabaseManager.getInstance();
        boolean popup = Boolean.parseBoolean(db.getSetting("notify_popup", "true"));
        boolean sound = Boolean.parseBoolean(db.getSetting("notify_sound", "false"));
        boolean email = Boolean.parseBoolean(db.getSetting("notify_email", "false"));
        int offset = parseInt(db.getSetting("reminder_minutes", "15"), 15);
        String emailTo = db.getSetting("notify_email_to", "");
        return new NotificationSettings(popup, sound, email, offset, emailTo);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean isPopupEnabled() { return popupEnabled; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public int getReminderOffsetMinutes() { return reminderOffsetMinutes; }
    public String getEmailTo() { return emailTo; }
}
