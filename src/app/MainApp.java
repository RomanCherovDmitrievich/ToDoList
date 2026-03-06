package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.TaskManager;
import util.AudioManager;
import util.NotificationService;
import util.TaskReminderService;
import util.ThemeManager;

import java.io.IOException;
import java.util.Objects;

/**
 * Точка входа JavaFX-приложения.
 */
public class MainApp extends Application {
    private static final String APP_TITLE = "ToDoList";
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 700;

    private Stage primaryStage;

    public static void main(String[] args) {
        try {
            Class.forName("javafx.application.Application");
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Missing dependency: " + e.getMessage());
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TaskReminderService.getInstance().stop();
            TaskManager.getInstance().close();
            AudioManager.getInstance().dispose();
            NotificationService.getInstance().shutdown();
        }));

        launch(args);
    }

    @Override
    public void init() {
        // Прогреваем сервисы на старте, чтобы избежать лагов в UI-потоке.
        TaskManager.getInstance();
        AudioManager.getInstance();
        TaskReminderService.getInstance();
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            ThemeManager.applyTheme(scene, ThemeManager.getCurrentTheme());

            stage.setTitle(APP_TITLE);
            stage.setScene(scene);
            stage.setMinWidth(860);
            stage.setMinHeight(560);

            try {
                Image icon = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/resources/images/app_icon.png")
                ));
                stage.getIcons().add(icon);
            } catch (Exception ignored) {
            }

            stage.show();
        } catch (IOException e) {
            showFatalError("Не удалось загрузить UI", e.getMessage());
        } catch (Exception e) {
            showFatalError("Ошибка запуска", e.toString());
        }
    }

    @Override
    public void stop() {
        TaskReminderService.getInstance().stop();
        TaskManager.getInstance().close();
        AudioManager.getInstance().dispose();
        NotificationService.getInstance().shutdown();
    }

    private void showFatalError(String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Критическая ошибка");
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
