package util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @class AudioManager
 * @brief Менеджер для работы с аудио в приложении
 * 
 * @details Класс AudioManager реализует паттерн Singleton для управления звуковыми эффектами.
 * Обеспечивает загрузку, воспроизведение и контроль аудио ресурсов приложения.
 * Поддерживает включение/выключение звуков и регулировку громкости.
 * 
 * @author Разработчик
 * @version 1.0
 * @date 2025-11-30
 * 
 * @note Использует JavaFX Media API для воспроизведения звуков
 * @warning Звуки требуют наличия соответствующих файлов в resources/audio/
 * @see Media
 * @see MediaPlayer
 * 
 * @singleton Гарантирует единственный экземпляр в приложении
 */
public class AudioManager {
    
    /**
     * @brief Единственный экземпляр AudioManager
     * @details Статическое поле для реализации паттерна Singleton
     */
    private static AudioManager instance;
    
    /**
     * @brief Коллекция медиа-плееров
     * @details Карта для хранения загруженных звуков по их именам
     * 
     * @key String - название звука (например, "startup", "add", "delete")
     * @value MediaPlayer - плеер для воспроизведения звука
     */
    private Map<String, MediaPlayer> mediaPlayers;
    
    /**
     * @brief Флаг включения звуков
     * @details Определяет, будут ли воспроизводиться звуки в приложении
     */
    private boolean soundsEnabled = true;
    
    /**
     * @brief Приватный конструктор
     * @details Инициализирует коллекцию медиа-плееров и загружает звуки
     * 
     * @note Конструктор приватный для реализации паттерна Singleton
     * @see #loadSounds()
     */
    private AudioManager() {
        mediaPlayers = new HashMap<>();
        loadSounds();
    }
    
    /**
     * @brief Получает единственный экземпляр AudioManager
     * @details Реализация паттерна Singleton с синхронизацией для многопоточности
     * 
     * @return AudioManager единственный экземпляр класса
     * 
     * @note Метод synchronized для безопасного использования в многопоточной среде
     * @warning При первом вызове создает новый экземпляр и загружает звуки
     */
    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    /**
     * @brief Загружает все звуки приложения
     * @details Загружает аудио файлы из папки resources/audio/ и создает MediaPlayer для каждого
     * 
     * @throws Exception если возникают ошибки при загрузке файлов
     * 
     * @note В случае ошибки отключает звуки и продолжает работу
     * @note Текущая версия загружает только стартовый звук "startup3.mp3"
     * 
     * @see Media
     * @see MediaPlayer
     */
    private void loadSounds() {
        try {
            // Загружаем стартовый звук
            URL startupSoundUrl = getClass().getResource("/resources/audio/startup3.mp3");
            if (startupSoundUrl != null) {
                Media startupMedia = new Media(startupSoundUrl.toString());
                MediaPlayer startupPlayer = new MediaPlayer(startupMedia);
                startupPlayer.setVolume(0.3);
                mediaPlayers.put("startup", startupPlayer);
            } else {
                System.out.println("Аудио файл не найден, звуки отключены");
                soundsEnabled = false;
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка загрузки аудио файлов: " + e.getMessage());
            // Продолжаем работу без звуков
            soundsEnabled = false;
        }
    }
    
    /**
     * @brief Воспроизводит стартовый звук
     * @details Проигрывает звук запуска приложения с начала
     * 
     * @note Звук воспроизводится только если soundsEnabled = true
     * @note Если плеер не найден или звуки отключены, метод ничего не делает
     * 
     * @see #soundsEnabled
     * @see MediaPlayer#seek(javafx.util.Duration)
     * @see MediaPlayer#play()
     */
    public void playStartupSound() {
        if (!soundsEnabled) return;
        
        MediaPlayer player = mediaPlayers.get("startup");
        if (player != null) {
            player.seek(player.getStartTime());
            player.play();
        }
    }

    public int getAudioFilesCount() {
        return mediaPlayers.size();
    }
    
    /**
     * @brief Воспроизводит звук добавления
     * @details Проигрывает звук при добавлении новой задачи
     * 
     * @note В текущей реализации звук "add" не загружается
     * @note Метод зарезервирован для будущего расширения функциональности
     */
    public void playAddSound() {
        if (!soundsEnabled) return;
        
        MediaPlayer player = mediaPlayers.get("add");
        if (player != null) {
            player.seek(player.getStartTime());
            player.play();
        }
    }
    
    /**
     * @brief Воспроизводит звук удаления
     * @details Проигрывает звук при удалении задачи
     * 
     * @note В текущей реализации звук "delete" не загружается
     * @note Метод зарезервирован для будущего расширения функциональности
     */
    public void playDeleteSound() {
        if (!soundsEnabled) return;
        
        MediaPlayer player = mediaPlayers.get("delete");
        if (player != null) {
            player.seek(player.getStartTime());
            player.play();
        }
    }
    
    /**
     * @brief Воспроизводит звук завершения
     * @details Проигрывает звук при отметке задачи как выполненной
     * 
     * @note В текущей реализации звук "complete" не загружается
     * @note Метод зарезервирован для будущего расширения функциональности
     */
    public void playCompleteSound() {
        if (!soundsEnabled) return;
        
        MediaPlayer player = mediaPlayers.get("complete");
        if (player != null) {
            player.seek(player.getStartTime());
            player.play();
        }
    }
    
    /**
     * @brief Включает/выключает звуки
     * @details Устанавливает флаг, определяющий будут ли воспроизводиться звуки
     * 
     * @param enabled true - включить звуки, false - выключить
     * 
     * @note Изменение этого флага не останавливает уже играющие звуки
     * @see #stopAllSounds() для остановки текущего воспроизведения
     */
    public void setSoundsEnabled(boolean enabled) {
        this.soundsEnabled = enabled;
    }
    
    /**
     * @brief Проверяет, включены ли звуки
     * @details Возвращает текущее состояние флага soundsEnabled
     * 
     * @return true если звуки включены, false если выключены
     * 
     * @see #soundsEnabled
     */
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }
    
    /**
     * @brief Останавливает все звуки
     * @details Останавливает воспроизведение на всех активных медиа-плеерах
     * 
     * @note Метод не освобождает ресурсы плееров
     * @see #dispose() для полного освобождения ресурсов
     */
    public void stopAllSounds() {
        for (MediaPlayer player : mediaPlayers.values()) {
            if (player != null) {
                player.stop();
            }
        }
    }
    
    /**
     * @brief Устанавливает громкость для всех звуков
     * @details Устанавливает уровень громкости для всех загруженных медиа-плееров
     * 
     * @param volume Уровень громкости от 0.0 (тишина) до 1.0 (максимальная)
     * 
     * @note Значение автоматически ограничивается диапазоном [0.0, 1.0]
     * @warning Изменение громкости применяется ко всем последующим воспроизведениям
     */
    public void setMasterVolume(double volume) {
        if (volume < 0) volume = 0;
        if (volume > 1) volume = 1;
        
        for (MediaPlayer player : mediaPlayers.values()) {
            if (player != null) {
                player.setVolume(volume);
            }
        }
    }
    
    /**
     * @brief Освобождает ресурсы
     * @details Останавливает все звуки, освобождает ресурсы медиа-плееров и очищает коллекцию
     * 
     * @note Этот метод должен вызываться при завершении работы приложения
     * @note После вызова dispose() объект AudioManager можно продолжать использовать
     * 
     * @see MediaPlayer#dispose()
     * @warning Повторный вызов loadSounds() потребуется для восстановления функциональности
     */
    public void dispose() {
        stopAllSounds();
        for (MediaPlayer player : mediaPlayers.values()) {
            if (player != null) {
                player.dispose();
            }
        }
        mediaPlayers.clear();
        instance = null; // СБРАСЫВАЕМ ИНСТАНС!
    }
    
    /**
     * @brief Проверяет наличие аудио файлов
     * @details Определяет, были ли успешно загружены какие-либо звуки
     * 
     * @return true если есть хотя бы один загруженный звук, false если коллекция пуста
     * 
     * @see #mediaPlayers
     */
    public boolean hasAudioFiles() {
        // Проверяем, есть ли вообще загруженные аудио файлы
        return !mediaPlayers.isEmpty();
    }
    
    /**
     * @brief Получает список доступных звуков
     * @details Возвращает массив названий всех загруженных звуков
     * 
     * @return String[] Массив названий звуков
     * 
     * @note Названия соответствуют ключам в коллекции mediaPlayers
     * @see #mediaPlayers
     */
    public String[] getAvailableSounds() {
        return mediaPlayers.keySet().toArray(new String[0]);
    }
}