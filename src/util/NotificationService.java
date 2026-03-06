package util;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Кроссплатформенный сервис системных уведомлений.
 */
public class NotificationService {
    private static NotificationService instance;

    private TrayIcon trayIcon;
    private boolean trayReady;

    private NotificationService() {
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public synchronized void showNotification(String title, String message) {
        if (title == null || title.isBlank()) {
            title = "ToDoList";
        }
        if (message == null || message.isBlank()) {
            return;
        }

        // На macOS системное уведомление через osascript обычно надежнее для background-окна.
        if (isMac() && showMacNotification(title, message)) {
            return;
        }

        ensureTrayIcon();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            return;
        }

        System.out.println("[NOTIFY] " + title + ": " + message);
    }

    private boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac");
    }

    private boolean showMacNotification(String title, String message) {
        String escapedTitle = escapeAppleScript(title);
        String escapedMessage = escapeAppleScript(message);
        ProcessBuilder builder = new ProcessBuilder(
            "osascript",
            "-e",
            "display notification \"" + escapedMessage + "\" with title \"" + escapedTitle + "\""
        );

        try {
            Process process = builder.start();
            int code = process.waitFor();
            return code == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private String escapeAppleScript(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void ensureTrayIcon() {
        if (trayReady) {
            return;
        }
        trayReady = true;

        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image icon = loadIcon();
            trayIcon = new TrayIcon(icon, "ToDoList");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (AWTException e) {
            trayIcon = null;
        }
    }

    private Image loadIcon() {
        // Легкая fallback-иконка, если внешняя не найдена.
        BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        try {
            File iconFile = new File("src/resources/images/app_icon.png");
            if (iconFile.exists()) {
                return javax.imageio.ImageIO.read(iconFile);
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    public synchronized void shutdown() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        trayReady = false;
    }
}
