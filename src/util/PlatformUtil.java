package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class PlatformUtil {
    public enum OsFamily {
        MAC,
        REDOS,
        WINDOWS,
        LINUX,
        OTHER
    }

    private PlatformUtil() {
    }

    public static OsFamily detectOs() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            return OsFamily.MAC;
        }
        if (osName.contains("win")) {
            return OsFamily.WINDOWS;
        }
        if (osName.contains("linux")) {
            if (isRedOs()) {
                return OsFamily.REDOS;
            }
            return OsFamily.LINUX;
        }
        return OsFamily.OTHER;
    }

    private static boolean isRedOs() {
        File osRelease = new File("/etc/os-release");
        if (!osRelease.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(osRelease))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lower = line.toLowerCase();
                if (lower.startsWith("id=") && lower.contains("redos")) {
                    return true;
                }
                if (lower.startsWith("name=") && lower.contains("redos")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
