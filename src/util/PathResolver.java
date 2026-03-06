package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathResolver {
    private static final String APP_DIR_NAME = "ToDoList";

    private PathResolver() {
    }

    public static Path getDataDir() {
        Path projectData = Paths.get("data");
        if (Files.isDirectory(projectData)) {
            return projectData.toAbsolutePath();
        }

        String userHome = System.getProperty("user.home", ".");
        PlatformUtil.OsFamily os = PlatformUtil.detectOs();

        return switch (os) {
            case MAC -> Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME);
            case REDOS, LINUX -> Paths.get(userHome, ".local", "share", APP_DIR_NAME);
            case WINDOWS -> {
                String appData = System.getenv("APPDATA");
                if (appData != null && !appData.isBlank()) {
                    yield Paths.get(appData, APP_DIR_NAME);
                }
                yield Paths.get(userHome, "AppData", "Roaming", APP_DIR_NAME);
            }
            case OTHER -> Paths.get(userHome, APP_DIR_NAME);
        };
    }

    public static Path getAudioDir() {
        Path projectAudio = Paths.get("audio");
        if (Files.isDirectory(projectAudio)) {
            return projectAudio.toAbsolutePath();
        }

        Path dataDir = getDataDir();
        return dataDir.resolve("audio");
    }

    public static Path getDatabaseFile() {
        return getDataDir().resolve("todolist.db");
    }

    public static Path getTasksJsonFile() {
        return getDataDir().resolve("tasks.json");
    }

    public static Path getNotificationsLog() {
        return getDataDir().resolve("notifications_outbox.log");
    }
}
