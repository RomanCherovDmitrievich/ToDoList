package util;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Менеджер аудио с циклическим плейлистом.
 */
public class AudioManager {
    private static AudioManager instance;

    private final List<URI> playlist = new ArrayList<>();
    private MediaPlayer currentPlayer;
    private int currentTrackIndex = -1;

    private boolean soundsEnabled = true;
    private double masterVolume = 0.35;

    private boolean javafxReady = false;

    private AudioManager() {
        refreshPlaylist();
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    private boolean ensureJavafxReady() {
        if (javafxReady) {
            return true;
        }
        try {
            Platform.startup(() -> {});
            javafxReady = true;
        } catch (IllegalStateException e) {
            // Toolkit already initialized.
            javafxReady = true;
        } catch (Exception e) {
            javafxReady = false;
        }
        return javafxReady;
    }

    /**
     * Пересканирует треки из внешней папки /audio и встроенных ресурсов.
     */
    public synchronized void refreshPlaylist() {
        playlist.clear();

        Set<String> unique = new HashSet<>();

        // 1) Внешняя папка (приоритетно для пользовательской музыки)
        for (Path dir : resolveExternalAudioDirs()) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try {
                Files.list(dir)
                    .filter(Files::isRegularFile)
                    .filter(path -> isAudioFile(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        URI uri = path.toUri();
                        if (unique.add(uri.toString())) {
                            playlist.add(uri);
                        }
                    });
            } catch (Exception e) {
                System.err.println("Audio scan error (" + dir + "): " + e.getMessage());
            }
        }

        // 2) Встроенные fallback-треки
        addBundledTrack("/resources/audio/startup.mp3", unique);
        addBundledTrack("/resources/audio/startup2.mp3", unique);
        addBundledTrack("/resources/audio/startup3.mp3", unique);

        if (playlist.isEmpty()) {
            soundsEnabled = false;
        }
    }

    private List<Path> resolveExternalAudioDirs() {
        List<Path> dirs = new ArrayList<>();

        String env = System.getenv("TODOLIST_AUDIO_DIR");
        if (env != null && !env.isBlank()) {
            dirs.add(Paths.get(env));
        }

        dirs.add(PathResolver.getAudioDir());
        dirs.add(Paths.get("audio"));
        dirs.add(Paths.get("/audio"));

        return dirs;
    }

    private void addBundledTrack(String resourcePath, Set<String> unique) {
        try {
            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                return;
            }
            URI uri = URI.create(url.toString());
            if (unique.add(uri.toString())) {
                playlist.add(uri);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isAudioFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a");
    }

    public synchronized void startPlaylist() {
        if (!soundsEnabled) {
            return;
        }

        if (!ensureJavafxReady()) {
            return;
        }

        if (playlist.isEmpty()) {
            refreshPlaylist();
        }

        if (playlist.isEmpty()) {
            return;
        }

        if (currentPlayer == null) {
            playTrackAt(0);
            return;
        }

        currentPlayer.play();
    }

    public synchronized void playStartupSound() {
        startPlaylist();
    }

    public synchronized void playAddSound() {
        // Не прерываем фоновый плейлист.
    }

    public synchronized void playDeleteSound() {
        // Не прерываем фоновый плейлист.
    }

    public synchronized void playCompleteSound() {
        // Не прерываем фоновый плейлист.
    }

    private synchronized void playTrackAt(int index) {
        if (playlist.isEmpty()) {
            return;
        }

        if (!ensureJavafxReady()) {
            soundsEnabled = false;
            return;
        }

        int safeIndex = ((index % playlist.size()) + playlist.size()) % playlist.size();
        URI uri = playlist.get(safeIndex);

        disposeCurrentPlayer();

        try {
            Media media = new Media(uri.toString());
            currentPlayer = new MediaPlayer(media);
            currentPlayer.setVolume(masterVolume);
            currentPlayer.setOnEndOfMedia(this::playNextTrack);
            currentPlayer.setOnError(() -> {
                System.err.println("Media player error: " + currentPlayer.getError());
                playNextTrack();
            });
            currentTrackIndex = safeIndex;
            currentPlayer.play();
        } catch (Exception e) {
            System.err.println("Cannot play track: " + uri + " -> " + e.getMessage());
            playNextTrack();
        }
    }

    private synchronized void playNextTrack() {
        if (playlist.isEmpty()) {
            return;
        }
        playTrackAt(currentTrackIndex + 1);
    }

    private void disposeCurrentPlayer() {
        if (currentPlayer == null) {
            return;
        }
        try {
            currentPlayer.stop();
            currentPlayer.dispose();
        } catch (Exception ignored) {
        }
        currentPlayer = null;
    }

    public synchronized int getAudioFilesCount() {
        return playlist.size();
    }

    public synchronized void setSoundsEnabled(boolean enabled) {
        this.soundsEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public synchronized boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public synchronized void stopAllSounds() {
        if (currentPlayer != null) {
            currentPlayer.stop();
        }
    }

    public synchronized void setMasterVolume(double volume) {
        if (volume < 0.0) {
            volume = 0.0;
        } else if (volume > 1.0) {
            volume = 1.0;
        }

        masterVolume = volume;

        if (currentPlayer != null) {
            currentPlayer.setVolume(volume);
        }
    }

    public synchronized void dispose() {
        stopAllSounds();
        disposeCurrentPlayer();
        playlist.clear();
        currentTrackIndex = -1;
        instance = null;
    }

    public synchronized boolean hasAudioFiles() {
        return !playlist.isEmpty();
    }

    public synchronized String[] getAvailableSounds() {
        List<String> names = new ArrayList<>();
        for (URI uri : playlist) {
            try {
                File file = Paths.get(uri).toFile();
                names.add(file.getName());
            } catch (Exception e) {
                names.add(uri.toString());
            }
        }
        return names.toArray(new String[0]);
    }
}
