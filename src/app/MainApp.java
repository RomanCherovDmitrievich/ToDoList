package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import util.AudioManager;

import java.io.IOException;
import java.util.Objects;

/**
 * @class MainApp
 * @brief Главный класс приложения To-Do List
 * 
 * @details Класс MainApp является точкой входа JavaFX приложения.
 * Отвечает за инициализацию, настройку и запуск главного окна приложения.
 * Управляет жизненным циклом приложения, загрузкой ресурсов и обработкой ошибок.
 * 
 * @author Разработчик
 * @version 1.0
 * @date 2025-11-30
 * 
 * @extends Application (JavaFX)
 * 
 * @see view.MainController
 * @see util.AudioManager
 * @see model.TaskManager
 * @see util.JsonUtil
 * 
 * @note Этот класс использует паттерн Singleton для AudioManager и TaskManager
 * @warning Требует JavaFX SDK в classpath для работы
 */
public class MainApp extends Application {
    
    /** 
     * @brief Заголовок приложения
     * @details Отображается в заголовке главного окна
     */
    private static final String APP_TITLE = "Умный планировщик задач";
    
    /** 
     * @brief Начальная ширина окна
     * @details Ширина главного окна при запуске приложения (в пикселях)
     */
    private static final int WINDOW_WIDTH = 900;
    
    /** 
     * @brief Начальная высота окна
     * @details Высота главного окна при запуске приложения (в пикселях)
     */
    private static final int WINDOW_HEIGHT = 600;
    
    /** 
     * @brief Минимальная ширина окна
     * @details Минимальная ширина, до которой пользователь может уменьшить окно
     */
    private static final int MIN_WIDTH = 700;
    
    /** 
     * @brief Минимальная высота окна
     * @details Минимальная высота, до которой пользователь может уменьшить окно
     */
    private static final int MIN_HEIGHT = 500;
    
    /** 
     * @brief Главное окно приложения
     * @details Ссылка на первичную Stage (окно) JavaFX приложения
     */
    private Stage primaryStage;
    
    /** 
     * @brief Менеджер аудио
     * @details Управляет звуковыми эффектами приложения (Singleton)
     */
    private AudioManager audioManager;
    
    /**
     * @brief Точка входа в приложение
     * @details Главный метод, вызываемый JVM для запуска приложения
     * 
     * @param args Аргументы командной строки (не используются)
     * 
     * @throws ClassNotFoundException если JavaFX не найден в classpath
     * @throws RuntimeException если не удается запустить JavaFX Application
     * 
     * @note Метод проверяет наличие JavaFX перед запуском приложения
     * @warning Без JavaFX приложение не сможет запуститься
     * 
     * @see Application#launch(String...)
     */
    public static void main(String[] args) {
        // Проверяем доступность JavaFX
        try {
            Class.forName("javafx.application.Application");
            launch(args);
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка: JavaFX не найден в classpath!");
            System.err.println("Убедитесь, что вы добавили javafx-sdk в библиотеки проекта.");
            System.err.println("Текущая версия: Java 25.0.1, JavaFX 25.0.1");
            System.exit(1);
        }
    }
    
    /**
     * @brief Метод инициализации приложения
     * @details Вызывается автоматически JavaFX перед методом start()
     *          Выполняет предварительную настройку приложения
     * 
     * @note В этом методе:
     *   - Выводится информация о версиях Java и JavaFX
     *   - Инициализируется AudioManager
     *   - Логируются ошибки инициализации
     * 
     * @warning Исключения в этом методе не прерывают запуск приложения
     * 
     * @see AudioManager#getInstance()
     */
    @Override
    public void init() {
        System.out.println("Инициализация приложения...");
        System.out.println("Версия Java: " + System.getProperty("java.version"));
        System.out.println("Версия JavaFX: " + System.getProperty("javafx.version"));
        
        try {
            // Инициализируем AudioManager
            audioManager = AudioManager.getInstance();
            System.out.println("Аудио менеджер инициализирован");
        } catch (Exception e) {
            System.err.println("Ошибка инициализации аудио менеджера: " + e.getMessage());
        }
    }
    
    /**
     * @brief Метод запуска JavaFX приложения
     * @details Основной метод настройки и отображения пользовательского интерфейса
     * 
     * @param primaryStage Главное окно приложения, предоставляемое JavaFX
     * 
     * @throws IOException если не удается загрузить FXML файл главного окна
     * @throws NullPointerException если не найден CSS файл или иконка
     * 
     * @note В этом методе:
     *   - Загружается FXML файл главного окна
     *   - Настраивается сцена с CSS стилями
     *   - Устанавливается иконка приложения
     *   - Настраиваются минимальные размеры окна
     *   - Добавляется обработчик закрытия окна
     *   - Отображается главное окно
     * 
     * @warning Все исключения перехватываются и показываются в диалоговом окне
     * 
     * @see FXMLLoader
     * @see Scene
     * @see Stage
     * @see #showErrorDialog(String, String, String)
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            this.primaryStage = primaryStage;
            this.primaryStage.setTitle(APP_TITLE);
            
            // Загружаем главное окно из FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            
            // Настраиваем сцену
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Загружаем CSS стили
            try {
                String css = Objects.requireNonNull(getClass().getResource("/resources/styles/styles.css")).toExternalForm();
                scene.getStylesheets().add(css);
                System.out.println("CSS стили загружены");
            } catch (NullPointerException e) {
                System.err.println("CSS файл не найден, используются стили по умолчанию");
            }
            
            // Настраиваем иконку приложения
            try {
                Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/app_icon.png")));
                primaryStage.getIcons().add(appIcon);
                System.out.println("Иконка приложения загружена");
            } catch (Exception e) {
                System.err.println("Не удалось загрузить иконку приложения: " + e.getMessage());
            }
            
            // Настраиваем главное окно
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.centerOnScreen();
            
            // Обработчик закрытия окна
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Закрытие приложения...");
                if (audioManager != null) {
                    audioManager.dispose();
                }
            });
            
            // Отображаем окно
            primaryStage.show();
            
            System.out.println("Приложение успешно запущено");
            
        } catch (IOException e) {
            System.err.println("Ошибка загрузки FXML файла: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Ошибка запуска", 
                          "Не удалось загрузить интерфейс приложения", 
                          e.getMessage());
        } catch (Exception e) {
            System.err.println("Критическая ошибка при запуске: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Критическая ошибка", 
                          "Приложение не может быть запущено", 
                          e.getMessage());
        }
    }
    
    /**
     * @brief Метод остановки приложения
     * @details Вызывается автоматически JavaFX при завершении приложения
     *          Выполняет очистку ресурсов и сохранение данных
     * 
     * @note В этом методе:
     *   - Сохраняются все задачи в JSON файл
     *   - Освобождаются ресурсы AudioManager
     *   - Логируется процесс остановки
     * 
     * @warning Метод гарантированно вызывается при нормальном завершении приложения
     * 
     * @see TaskManager#getInstance()
     * @see JsonUtil#saveTasks(List)
     * @see AudioManager#dispose()
     */
    @Override
    public void stop() {
        System.out.println("Остановка приложения...");
        
        // Сохраняем задачи перед выходом
        try {
            model.TaskManager taskManager = model.TaskManager.getInstance();
            util.JsonUtil.saveTasks(taskManager.getAllTasks());
            System.out.println("Задачи сохранены");
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении задач: " + e.getMessage());
        }
        
        // Освобождаем ресурсы AudioManager
        if (audioManager != null) {
            audioManager.dispose();
            System.out.println("Аудио ресурсы освобождены");
        }
        
        System.out.println("Приложение остановлено");
    }
    
    /**
     * @brief Возвращает главное окно приложения
     * @details Предоставляет доступ к первичному Stage для других компонентов
     * 
     * @return Stage Главное окно приложения
     * 
     * @note Используется для модальных диалогов и управления окном
     * 
     * @see Stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * @brief Показывает диалоговое окно с ошибкой
     * @details Создает и отображает модальное диалоговое окно с сообщением об ошибке
     * 
     * @param title Заголовок диалогового окна
     * @param header Основной заголовок сообщения
     * @param content Подробное описание ошибки
     * 
     * @note Используется для информирования пользователя о критических ошибках
     * @note Окно блокирует взаимодействие с основным приложением до закрытия
     * 
     * @see Alert
     * @see Alert.AlertType#ERROR
     */
    private void showErrorDialog(String title, String header, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * @brief Перезагружает главное окно
     * @details Загружает заново FXML файл главного окна и обновляет сцену
     * 
     * @note Используется для обновления интерфейса без перезапуска приложения
     * @note Сохраняет текущие размеры окна
     * 
     * @throws IOException если не удается загрузить FXML файл
     * 
     * @see FXMLLoader
     * @see Scene
     * @warning Не сохраняет состояние таблицы и другие UI элементы
     */
    public void reloadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
            
            // Восстанавливаем CSS
            try {
                String css = Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm();
                scene.getStylesheets().add(css);
            } catch (Exception e) {
                // Игнорируем ошибку CSS
            }
            
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            
        } catch (IOException e) {
            System.err.println("Ошибка перезагрузки интерфейса: " + e.getMessage());
        }
    }
}