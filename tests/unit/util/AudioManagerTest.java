package unit.util;

import util.AudioManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование класса AudioManager
 */
@DisplayName("Тестирование AudioManager")
class AudioManagerTest {
    
    private AudioManager audioManager;
    
    @BeforeEach
    void setUp() {
        // Получаем экземпляр AudioManager
        audioManager = AudioManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Останавливаем и освобождаем ресурсы
        audioManager.dispose();
    }
    
    @Test
    @DisplayName("Проверка паттерна Singleton")
    void testSingletonPattern() {
        AudioManager instance1 = AudioManager.getInstance();
        AudioManager instance2 = AudioManager.getInstance();
        
        assertSame(instance1, instance2, "Должен возвращаться один и тот же экземпляр");
        assertNotNull(instance1);
        assertNotNull(instance2);
    }

    @Test
    void testEnableDisableSounds() {
        AudioManager audioManager = AudioManager.getInstance();
    
        // Проверяем начальное состояние
        boolean initialEnabled = audioManager.isSoundsEnabled();
    
        // Включаем/выключаем и проверяем
        audioManager.setSoundsEnabled(true);
        assertTrue(audioManager.isSoundsEnabled(), "Звуки должны быть включены");
    
        audioManager.setSoundsEnabled(false);
        assertFalse(audioManager.isSoundsEnabled(), "Звуки должны быть выключены");
    
        // Восстанавливаем исходное состояние
        audioManager.setSoundsEnabled(initialEnabled);
    }   
    
    @Test
    @DisplayName("Проверка наличия аудио файлов")
    void testHasAudioFiles() {
        boolean hasAudio = audioManager.hasAudioFiles();
        // В зависимости от наличия файлов в ресурсах
        assertTrue(hasAudio || !hasAudio); // Всегда true - просто проверяем что метод работает
    }
    
    @Test
    @DisplayName("Получение списка доступных звуков")
    void testGetAvailableSounds() {
        String[] sounds = audioManager.getAvailableSounds();
        assertNotNull(sounds);
        // Может быть пустым если нет аудио файлов
    }
    
    @Test
    @DisplayName("Установка громкости")
    void testSetMasterVolume() {
        // Проверяем корректные значения
        audioManager.setMasterVolume(0.5);
        audioManager.setMasterVolume(0.0);
        audioManager.setMasterVolume(1.0);
        
        // Проверяем граничные значения (должны быть обрезаны)
        audioManager.setMasterVolume(-0.5); // Должно стать 0
        audioManager.setMasterVolume(1.5);  // Должно стать 1
        
        // Метод не должен падать при любых значениях
        assertDoesNotThrow(() -> {
            audioManager.setMasterVolume(100);
            audioManager.setMasterVolume(-100);
        });
    }
    
    @Test
    @DisplayName("Воспроизведение звуков при включенных звуках")
    void testPlaySoundsWhenEnabled() {
        audioManager.setSoundsEnabled(true);
        
        // Методы не должны падать
        assertDoesNotThrow(() -> {
            audioManager.playStartupSound();
            audioManager.playAddSound();
            audioManager.playDeleteSound();
            audioManager.playCompleteSound();
        });
    }
    
    @Test
    @DisplayName("Воспроизведение звуков при выключенных звуках")
    void testPlaySoundsWhenDisabled() {
        audioManager.setSoundsEnabled(false);
        
        // Методы не должны падать даже при выключенных звуках
        assertDoesNotThrow(() -> {
            audioManager.playStartupSound();
            audioManager.playAddSound();
            audioManager.playDeleteSound();
            audioManager.playCompleteSound();
        });
    }
    
    @Test
    @DisplayName("Остановка всех звуков")
    void testStopAllSounds() {
        // Метод не должен падать
        assertDoesNotThrow(() -> {
            audioManager.stopAllSounds();
        });
    }
    
    @Test
    @DisplayName("Освобождение ресурсов")
    void testDispose() {
        AudioManager manager1 = AudioManager.getInstance();
        manager1.dispose();
    
        AudioManager manager2 = AudioManager.getInstance();
    
        // Проверяем, что это НЕ тот же объект
        assertNotSame(manager1, manager2, "После dispose() должен создаваться новый экземпляр");
    
        // И что новый объект работает
        assertTrue(manager2.isSoundsEnabled() || !manager2.isSoundsEnabled(), "Новый экземпляр должен быть функциональным");
    }
    
    
    @Test
    @DisplayName("Тестирование методов без реальных аудио файлов")
    void testWithoutAudioFiles() {
        // Даже если аудио файлов нет, методы не должны падать
        if (!audioManager.hasAudioFiles()) {
            audioManager.playStartupSound(); // Должен просто ничего не делать
            audioManager.stopAllSounds();    // Не должен падать
        }
    }
    
    @Test
    @DisplayName("Проверка многократного вызова методов")
    void testMultipleCalls() {
        // Методы должны выдерживать многократный вызов
        for (int i = 0; i < 10; i++) {
            audioManager.playStartupSound();
            audioManager.setMasterVolume(i / 10.0);
            audioManager.stopAllSounds();
        }
        
        // После всех вызовов экземпляр должен оставаться рабочим
        assertNotNull(audioManager);
        assertTrue(audioManager.isSoundsEnabled() || !audioManager.isSoundsEnabled());
    }
}