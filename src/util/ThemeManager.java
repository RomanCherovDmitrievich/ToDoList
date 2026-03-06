package util;

import javafx.scene.Scene;
import repository.DatabaseManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ThemeManager {
    private static final Map<String, String> THEMES = new LinkedHashMap<>();

    static {
        THEMES.put("Summer", "/resources/styles/styles.css");
        THEMES.put("Dark", "/resources/styles/styles-dark.css");
        THEMES.put("Ocean", "/resources/styles/styles-ocean.css");
        THEMES.put("Custom", "CUSTOM");
    }

    private ThemeManager() {
    }

    public static String getCurrentTheme() {
        String stored = DatabaseManager.getInstance().getSetting("theme", "Summer");
        if (stored == null) {
            return "Summer";
        }
        if (THEMES.containsKey(stored)) {
            return stored;
        }
        String normalized = stored.trim().toLowerCase(Locale.ROOT);
        for (String key : THEMES.keySet()) {
            if (key.toLowerCase(Locale.ROOT).equals(normalized)) {
                return key;
            }
        }
        return "Summer";
    }

    public static void setTheme(String theme) {
        String resolved = resolveTheme(theme);
        DatabaseManager.getInstance().saveSetting("theme", resolved);
    }

    public static void applyTheme(Scene scene, String theme) {
        if (scene == null) {
            return;
        }
        theme = resolveTheme(theme);
        scene.getStylesheets().clear();
        String path = THEMES.get(theme);
        if ("CUSTOM".equals(path)) {
            Path custom = getCustomThemePath();
            if (custom != null && Files.exists(custom)) {
                scene.getStylesheets().add(custom.toUri().toString());
                return;
            }
            path = THEMES.get("Summer");
        }
        scene.getStylesheets().add(Objects.requireNonNull(ThemeManager.class.getResource(path)).toExternalForm());
    }

    public static String[] getAvailableThemes() {
        return THEMES.keySet().toArray(new String[0]);
    }

    public static Path getCustomThemePath() {
        return PathResolver.getDataDir().resolve("custom-theme.css");
    }

    private static String resolveTheme(String theme) {
        if (theme == null) {
            return "Summer";
        }
        if (THEMES.containsKey(theme)) {
            return theme;
        }
        String normalized = theme.trim().toLowerCase(Locale.ROOT);
        for (String key : THEMES.keySet()) {
            if (key.toLowerCase(Locale.ROOT).equals(normalized)) {
                return key;
            }
        }
        return "Summer";
    }
}
