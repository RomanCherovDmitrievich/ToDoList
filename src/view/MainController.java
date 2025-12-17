package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.animation.AnimationTimer;
import javafx.collections.transformation.SortedList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleObjectProperty;

import model.Task;
import model.Priority;
import model.TaskManager;
import viewmodel.TaskViewModel;
import util.JsonUtil;
import util.AudioManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * @class MainController
 * @brief Контроллер главного окна приложения To-Do List
 * 
 * @details Управляет пользовательским интерфейсом главного окна, обрабатывает все пользовательские взаимодействия,
 *          координирует работу между моделью данных и представлением. Отвечает за отображение задач,
 *          обработку действий пользователя, сортировку, поиск и анимацию снежинок.
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @see TaskManager
 * @see TaskViewModel
 * @see NewTaskController
 * @see JsonUtil
 * @see AudioManager
 * 
 * @note Реализует паттерн MVC/MVVM для разделения ответственности
 * @warning Все методы, помеченные @FXML, вызываются автоматически JavaFX
 */
public class MainController {
    /** 
     * @brief Таблица для отображения задач
     * @details JavaFX TableView, которая отображает список задач в виде таблицы.
     *          Связана с TaskViewModel для двусторонней привязки данных.
     */
    @FXML private TableView<TaskViewModel> taskTable;

    /** @brief Колонка таблицы для заголовка задачи */
    @FXML private TableColumn<TaskViewModel, String> titleColumn;

    /** @brief Колонка таблицы для описания задачи */
    @FXML private TableColumn<TaskViewModel, String> descriptionColumn;

    /** @brief Колонка таблицы для срока выполнения задачи */
    @FXML private TableColumn<TaskViewModel, LocalDateTime> deadlineColumn;
    
    /** @brief Колонка таблицы для приоритета задачи */
    @FXML private TableColumn<TaskViewModel, Priority> priorityColumn;
    
    /** @brief Колонка таблицы для категории задачи */
    @FXML private TableColumn<TaskViewModel, String> categoryColumn;
    
    /** @brief Колонка таблицы для статуса выполнения задачи */
    @FXML private TableColumn<TaskViewModel, Boolean> statusColumn;
    
    /** 
     * @brief Поле для поиска задач
     * @details Позволяет пользователю искать задачи по тексту в заголовке или описании.
     *          Поиск происходит в реальном времени по мере ввода текста.
     */
    @FXML private TextField searchField;

    /** @brief Кнопка для добавления новой задачи */
    @FXML private Button addButton;

    /** @brief Кнопка для удаления выбранной задачи */
    @FXML private Button deleteButton;

    /** @brief Кнопка для сортировки задач по дате */
    @FXML private Button sortByDateButton;

    /** @brief Кнопка для сортировки задач по приоритету */
    @FXML private Button sortByPriorityButton;

    /** 
     * @brief Метка для отображения статистики задач
     * @details Показывает общее количество задач, количество выполненных и просроченных задач.
     */
    @FXML private Label statusLabel;

    /** 
     * @brief Панель для анимации снежинок
     * @details Контейнер JavaFX, на котором отрисовываются анимированные снежинки.
     *          Используется для создания фоновой новогодней анимации.
     */
    @FXML private Pane snowPane; // Панель для снежинок

    /** @brief Метка с заголовком приложения */
    @FXML private Label appTitleLabel;

    /** @brief Главный контейнер интерфейса */
    @FXML private Pane mainContainer;
    

    /** @brief Менеджер задач для работы с бизнес-логикой */
    private TaskManager taskManager;
    
    /** @brief Менеджер аудио для воспроизведения звуковых эффектов */
    private AudioManager audioManager;
    
    /** @brief Таймер для анимации снежинок */
    private AnimationTimer snowTimer;
    
    /** @brief Генератор случайных чисел для создания снежинок */
    private Random random;
    
    /** @brief Список данных снежинок для анимации */
    private List<SnowflakeData> snowflakes;
    
    /** @brief Наблюдаемый список задач для отображения в таблице */
    private ObservableList<TaskViewModel> taskList;

    /** @brief Флаг направления сортировки по дате (true - возрастание, false - убывание) */
    private boolean sortByDateAscending = true;

    /** 
     * @brief Флаг направления сортировки по приоритету
     * @details true - неважные сверху, false - важные сверху (по умолчанию)
     */
    private boolean sortByPriorityAscending = false; // По умолчанию: важные сверху
    
    /**
     * @class SnowflakeData
     * @brief Внутренний класс для хранения данных одной снежинки
     * @details Содержит графический элемент снежинки и параметры её анимации.
     */
    private class SnowflakeData {
        /** @brief Графический элемент снежинки (круг) */
        javafx.scene.shape.Circle circle;
        
        /** @brief Скорость падения снежинки (пикселей в секунду) */
        double speed;
        
        /** @brief Параметр для эффекта покачивания снежинки */
        double sway;

        /**
         * @brief Конструктор для создания данных снежинки
         * @param circle Графический элемент снежинки
         * @param speed Скорость падения
         * @param sway Параметр покачивания
         */
        SnowflakeData(javafx.scene.shape.Circle circle, double speed, double sway) {
            this.circle = circle;
            this.speed = speed;
            this.sway = sway;
        }
    }
    
    /**
     * @brief Инициализация контроллера
     * @details Вызывается автоматически JavaFX после загрузки FXML файла.
     *          Инициализирует все компоненты интерфейса, загружает задачи,
     *          настраивает слушатели событий и запускает анимацию.
     * 
     * @note Этот метод является точкой входа для инициализации контроллера
     * @warning Не вызывайте этот метод напрямую, он вызывается JavaFX Framework
     */
    @FXML
    public void initialize() {
        taskManager = TaskManager.getInstance();
        audioManager = AudioManager.getInstance();
        random = new Random();
        snowflakes = new ArrayList<>();
        
        // Инициализируем список задач
        taskList = FXCollections.observableArrayList();
        
        // Настраиваем заголовок
        appTitleLabel.setText("🎄 Умный планировщик задач 🎅");
        
        // Настраиваем колонки таблицы
        setupTableColumns();
        
        // Настраиваем адаптивную политику изменения размеров
        setupAdaptiveLayout();
        
        // Настраиваем иконки для кнопок
        setupButtons();
        
        // Загружаем задачи из файла
        loadTasks();
        updateStatusLabel();
        
        // Настраиваем поиск
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchTasks();
        });
        
        // Воспроизводим стартовую музыку
        audioManager.playStartupSound();
        
        // Инициализируем снежинки после загрузки интерфейса
        if (snowPane != null) {
            snowPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene != null) {
                    // Ждем немного для полной инициализации
                    javafx.application.Platform.runLater(() -> {
                        initSnowflakes();
                        startSnowAnimation();
                    });
                }
            });
        }
    }
    
    /**
     * @brief Настраивает колонки таблицы задач
     * @details Конфигурирует все колонки таблицы: устанавливает фабрики значений,
     *          создает кастомные ячейки для отображения с цветовым кодированием
     *          и специальными иконками.
     * 
     * @note Использует PropertyValueFactory для привязки данных из TaskViewModel
     * @see TaskViewModel
     */
    private void setupTableColumns() {
        // Колонка заголовка
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        
        // Колонка описания
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Колонка срока выполнения (используем LocalDateTime для сортировки)
        deadlineColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
        
        // Колонка приоритета (используем Priority для сортировки)
        priorityColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getPriority()));
        
        // Колонка категории
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryDisplay"));
        
        // Колонка статуса
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("completed"));
        
        // Кастомная ячейка для приоритета с цветом
        priorityColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<TaskViewModel, Priority> call(TableColumn<TaskViewModel, Priority> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Priority item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item.getDisplayName());
                            String color = item.getColor();
                            setStyle("-fx-text-fill: white; -fx-background-color: " + color + 
                                    "; -fx-background-radius: 5; -fx-padding: 3 6 3 6;");
                        }
                    }
                };
            }
        });
        
        // Кастомная ячейка для статуса (чекбокс)
        statusColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<TaskViewModel, Boolean> call(TableColumn<TaskViewModel, Boolean> param) {
                return new TableCell<>() {
                    private final CheckBox checkBox = new CheckBox();
                    
                    {
                        checkBox.setOnAction(event -> {
                            TaskViewModel task = getTableView().getItems().get(getIndex());
                            if (task != null) {
                                task.setCompleted(checkBox.isSelected());
                                taskManager.markAsCompleted(task.getId(), checkBox.isSelected());
                                updateStatusLabel();
                                JsonUtil.saveTasks(taskManager.getAllTasks());
                            }
                        });
                    }
                    
                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            TaskViewModel task = getTableView().getItems().get(getIndex());
                            if (task != null) {
                                checkBox.setSelected(item != null && item);
                                checkBox.setText("");
                                setGraphic(checkBox);
                            }
                        }
                    }
                };
            }
        });
        
        // Кастомная ячейка для заголовка с иконкой просроченности
        titleColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<TaskViewModel, String> call(TableColumn<TaskViewModel, String> param) {
                return new TableCell<>() {
                    private final HBox hbox = new HBox(5);
                    private final ImageView warningIcon = new ImageView();
                    private final Label titleLabel = new Label();
                    
                    {
                        hbox.getChildren().addAll(warningIcon, titleLabel);
                        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);
                        warningIcon.setFitHeight(16);
                        warningIcon.setFitWidth(16);
                        
                        // Загружаем иконку предупреждения (будет в ресурсах)
                        try {
                            Image warningImage = new Image(getClass().getResourceAsStream("/images/warning_icon.png"));
                            warningIcon.setImage(warningImage);
                        } catch (Exception e) {
                            // Если иконка не найдена, просто не показываем
                            warningIcon.setVisible(false);
                        }
                    }
                    
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            TaskViewModel task = getTableView().getItems().get(getIndex());
                            if (task != null) {
                                titleLabel.setText(item);
                                
                                // Показываем иконку предупреждения для просроченных задач
                                if (task.isOverdue() && !task.isCompleted()) {
                                    warningIcon.setVisible(true);
                                    titleLabel.setTextFill(Color.RED);
                                    titleLabel.setStyle("-fx-font-weight: bold;");
                                } else if (task.isCompleted()) {
                                    warningIcon.setVisible(false);
                                    titleLabel.setTextFill(Color.GRAY);
                                    titleLabel.setStyle("-fx-font-style: italic; text-decoration: line-through;");
                                } else {
                                    warningIcon.setVisible(false);
                                    titleLabel.setTextFill(Color.BLACK);
                                    titleLabel.setStyle("");
                                }
                                
                                setGraphic(hbox);
                            }
                        }
                    }
                };
            }
        });
        
        // Кастомная ячейка для даты (форматированное отображение)
        deadlineColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<TaskViewModel, LocalDateTime> call(TableColumn<TaskViewModel, LocalDateTime> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(LocalDateTime item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            TaskViewModel task = getTableView().getItems().get(getIndex());
                            if (task != null) {
                                setText(task.getFormattedEndTime());
                                
                                // Подсвечиваем просроченные задачи
                                if (task.isOverdue() && !task.isCompleted()) {
                                    setTextFill(Color.RED);
                                    setStyle("-fx-font-weight: bold;");
                                } else if (task.isCompleted()) {
                                    setTextFill(Color.GRAY);
                                    setStyle("-fx-font-style: italic;");
                                } else {
                                    setTextFill(Color.BLACK);
                                    setStyle("");
                                }
                            }
                        }
                    }
                };
            }
        });
    }
    
    /**
     * @brief Настраивает адаптивный layout интерфейса
     * @details Конфигурирует поведение интерфейса при изменении размеров окна.
     *          Устанавливает политику изменения размеров таблицы и слушатели
     *          для отслеживания изменений размеров окна.
     * 
     * @note Таблица использует CONSTRAINED_RESIZE_POLICY для автоматического
     *       изменения ширины колонок
     */
    private void setupAdaptiveLayout() {
        // Таблица будет адаптироваться к размеру окна
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Устанавливаем слушатели изменения размеров
        if (taskTable.getScene() != null) {
            setupResizeListeners();
        } else {
            taskTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupResizeListeners();
                }
            });
        }
    }
    
    /**
     * @brief Настраивает слушатели изменения размеров окна
     * @details Добавляет слушатели к свойствам ширины и высоты окна для
     *          адаптации интерфейса при изменении размеров.
     * 
     * @note Слушатели вызывают методы adjustTableColumns() и adjustTableHeight()
     *       при каждом изменении размеров окна
     */
    private void setupResizeListeners() {
        Stage stage = (Stage) taskTable.getScene().getWindow();
        
        // При изменении ширины окна - адаптируем таблицу
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustTableColumns();
            updateSnowflakesForWindow();
        });
        
        // При изменении высоты окна - показываем больше/меньше строк
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            adjustTableHeight();
            updateSnowflakesForWindow();
        });
    }
    
    /**
     * @brief Настраивает ширины колонок таблицы
     * @details Вычисляет оптимальные ширины колонок таблицы на основе
     *          текущей ширины таблицы и предопределенных процентных соотношений.
     * 
     * @note Процентные соотношения колонок:
     *       - Заголовок: 20%
     *       - Описание: 30%
     *       - Срок выполнения: 15%
     *       - Приоритет: 10%
     *       - Категория: 10%
     *       - Статус: 8%
     */
    private void adjustTableColumns() {
        if (taskTable.getScene() == null) return;
        
        double tableWidth = taskTable.getWidth();
        if (tableWidth <= 0) return;
        
        // Процентные ширины для колонок
        double[] columnPercentages = {0.20, 0.30, 0.15, 0.10, 0.10, 0.08};
        
        // Устанавливаем ширины
        titleColumn.setPrefWidth(tableWidth * columnPercentages[0]);
        descriptionColumn.setPrefWidth(tableWidth * columnPercentages[1]);
        deadlineColumn.setPrefWidth(tableWidth * columnPercentages[2]);
        priorityColumn.setPrefWidth(tableWidth * columnPercentages[3]);
        categoryColumn.setPrefWidth(tableWidth * columnPercentages[4]);
        statusColumn.setPrefWidth(tableWidth * columnPercentages[5]);
    }
    
    /**
     * @brief Настраивает высоту таблицы
     * @details Устанавливает предпочтительную высоту таблицы как процент от
     *          высоты окна, чтобы таблица занимала фиксированную часть интерфейса.
     * 
     * @note Таблица занимает 60% от доступной высоты окна,
     *       остальное пространство отводится под другие элементы интерфейса
     */
    private void adjustTableHeight() {
        if (taskTable.getScene() == null) return;
        
        double sceneHeight = taskTable.getScene().getHeight();
        // Таблица занимает 60% от доступной высоты
        taskTable.setPrefHeight(sceneHeight * 0.60);
    }
    
    /**
     * @brief Настраивает кнопки интерфейса
     * @details Загружает иконки для кнопок, устанавливает тексты, стили
     *          и обработчики событий для всех кнопок управления.
     * 
     * @note Если иконки не могут быть загружены, используются текстовые эмодзи
     * @warning Обработчики событий привязаны к методам sortByDate() и sortByPriority()
     */
    private void setupButtons() {
        // Настраиваем иконки для кнопок
        try {
            Image addImage = new Image(getClass().getResourceAsStream("/resources/images/add_icon.png"));
            addButton.setGraphic(new ImageView(addImage));
            addButton.setText(" Добавить");
            
            Image deleteImage = new Image(getClass().getResourceAsStream("/resources/images/delete_icon.png"));
            deleteButton.setGraphic(new ImageView(deleteImage));
            deleteButton.setText(" Удалить");
            
            Image sortDateImage = new Image(getClass().getResourceAsStream("/resources/images/sort_date.png"));
            sortByDateButton.setGraphic(new ImageView(sortDateImage));
            sortByDateButton.setText(" По дате");
            
            Image sortPriorityImage = new Image(getClass().getResourceAsStream("/resources/images/sort_priority.png"));
            sortByPriorityButton.setGraphic(new ImageView(sortPriorityImage));
            sortByPriorityButton.setText(" По приоритету");
            
        } catch (Exception e) {
            System.err.println("Не удалось загрузить иконки кнопок: " + e.getMessage());
            // Используем текст если иконки не загрузились
            addButton.setText("➕ Добавить");
            deleteButton.setText("🗑️ Удалить");
            sortByDateButton.setText("📅 По дате");
            sortByPriorityButton.setText("⚠️ По приоритету");
        }
        
        // Стили для кнопок сортировки
        sortByDateButton.setStyle("-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: #3498db; -fx-text-fill: white;");
        sortByPriorityButton.setStyle("-fx-font-size: 12px; -fx-padding: 5 10; -fx-background-color: #9b59b6; -fx-text-fill: white;");
        
        // Обработчики для кнопок сортировки
        sortByDateButton.setOnAction(e -> sortByDate());
        sortByPriorityButton.setOnAction(e -> sortByPriority());
    }
    
    /**
     * @brief Сортирует задачи по дате выполнения
     * @details Сортирует список задач по дате окончания (дедлайну).
     *          При повторном нажатии меняет направление сортировки.
     *          Обновляет текст кнопки для отображения текущего направления.
     * 
     * @event OnAction кнопки "По дате"
     * 
     * @note По умолчанию сортировка в возрастающем порядке (старые сверху)
     * @warning Отменяет предыдущую сортировку по приоритету
     */
    @FXML
    private void sortByDate() {
        if (taskList.isEmpty()) return;
        
        // Меняем направление сортировки
        sortByDateAscending = !sortByDateAscending;
        
        // Обновляем текст кнопки
        String arrow = sortByDateAscending ? " ↑" : " ↓";
        sortByDateButton.setText("По дате" + arrow);
        
        // Создаем компаратор для сортировки по дате
        Comparator<TaskViewModel> dateComparator = Comparator.comparing(TaskViewModel::getEndTime);
        
        // Если сортировка по убыванию (новые сверху), инвертируем
        if (!sortByDateAscending) {
            dateComparator = dateComparator.reversed();
        }
        
        // Применяем сортировку
        FXCollections.sort(taskList, dateComparator);
        
        // Показываем сообщение о сортировке
        String direction = sortByDateAscending ? "старые сверху" : "новые сверху";
        showAlert("Сортировка", "Задачи отсортированы по дате", 
                 "Задачи отсортированы по сроку выполнения (" + direction + ")");
    }
    
    /**
     * @brief Сортирует задачи по приоритету
     * @details Сортирует список задач по приоритету: URGENT → IMPORTANT → NORMAL.
     *          При повторном нажатии меняет направление сортировки.
     *          Внутри одинаковых приоритетов задачи сортируются по дате.
     * 
     * @event OnAction кнопки "По приоритету"
     * 
     * @note По умолчанию сортировка в порядке важности (важные сверху)
     * @warning Отменяет предыдущую сортировку по дате
     * 
     * @see Priority
     */
    @FXML
    private void sortByPriority() {
        if (taskList.isEmpty()) return;
        
        // Меняем направление сортировки
        sortByPriorityAscending = !sortByPriorityAscending;
        
        // Обновляем текст кнопки
        String arrow = sortByPriorityAscending ? " ↑" : " ↓";
        sortByPriorityButton.setText("По приоритету" + arrow);
        
        // Создаем компаратор для сортировки по приоритету
        Comparator<TaskViewModel> priorityComparator = Comparator.comparing(task -> {
            Priority p = task.getPriority();
            // Приоритеты: URGENT -> IMPORTANT -> NORMAL
            switch (p) {
                case URGENT: return 1;
                case IMPORTANT: return 2;
                case NORMAL: return 3;
                default: return 4;
            }
        });
        
        // Если сортировка по убыванию (важные сверху), это наше состояние по умолчанию
        if (!sortByPriorityAscending) {
            // Важные сверху (URGENT -> IMPORTANT -> NORMAL)
            // Это уже наше состояние по умолчанию для компаратора
        } else {
            // Неважные сверху (NORMAL -> IMPORTANT -> URGENT)
            priorityComparator = priorityComparator.reversed();
        }
        
        // Дополнительная сортировка по дате внутри одинаковых приоритетов
        priorityComparator = priorityComparator.thenComparing(TaskViewModel::getEndTime);
        
        // Применяем сортировку
        FXCollections.sort(taskList, priorityComparator);
        
        // Показываем сообщение о сортировке
        String direction = sortByPriorityAscending ? "неважные сверху" : "важные сверху";
        showAlert("Сортировка", "Задачи отсортированы по приоритету", 
                 "Задачи отсортированы по приоритету (" + direction + ")");
    }
    
    /**
     * @brief Инициализирует снежинки для всего окна
     * @details Очищает предыдущие снежинки и создает новые на основе
     *          текущих размеров окна. Используется при запуске приложения
     *          и при значительном изменении размеров окна.
     * 
     * @note Создает фиксированное количество снежинок (120)
     * @see #createSnowflakesForWindow(double, double)
     */
    private void initSnowflakes() {
        if (snowPane == null) return;
        
        // Очищаем старые снежинки
        snowflakes.clear();
        snowPane.getChildren().clear();
        
        // Получаем размеры всего окна, а не только snowPane
        Stage stage = (Stage) snowPane.getScene().getWindow();
        double windowWidth = stage.getWidth();
        double windowHeight = stage.getHeight();
        
        // Создаем много снежинок для всего окна
        createSnowflakesForWindow(windowWidth, windowHeight);
    }
    
    /**
     * @brief Создает снежинки для всего окна
     * @details Создает указанное количество снежинок со случайными параметрами:
     *          размером, позицией, прозрачностью, скоростью и амплитудой покачивания.
     * 
     * @param windowWidth Ширина окна для распределения снежинок
     * @param windowHeight Высота окна для распределения снежинок
     * 
     * @note Каждая снежинка имеет уникальные параметры для естественного вида
     */
    private void createSnowflakesForWindow(double windowWidth, double windowHeight) {
        // Много снежинок для всего окна (100-150)
        int snowflakeCount = 120;
        
        for (int i = 0; i < snowflakeCount; i++) {
            javafx.scene.shape.Circle snowflake = new javafx.scene.shape.Circle();
            
            // Очень маленькие снежинки (1-3 пикселя)
            double size = 1 + random.nextDouble() * 2;
            snowflake.setRadius(size);
            
            // Начальная позиция (случайная по всему окну)
            double x = random.nextDouble() * windowWidth;
            double y = random.nextDouble() * windowHeight;
            
            snowflake.setTranslateX(x);
            snowflake.setTranslateY(y);
            
            // Прозрачность
            snowflake.setOpacity(0.1 + random.nextDouble() * 0.6);
            
            // Белый цвет
            snowflake.setFill(Color.WHITE);
            
            // Добавляем легкое свечение
            snowflake.setEffect(new javafx.scene.effect.Glow(0.2));
            
            // Разная скорость падения
            double speed = 20 + random.nextDouble() * 40;
            double sway = random.nextDouble() * 2 - 1;
            
            snowflakes.add(new SnowflakeData(snowflake, speed, sway));
            snowPane.getChildren().add(snowflake);
        }
    }
    
    /**
     * @brief Обновляет снежинки при изменении размера окна
     * @details Проверяет положение существующих снежинок относительно новых границ окна
     *          и при необходимости корректирует их позиции. Также добавляет новые
     *          снежинки если окно увеличилось.
     * 
     * @note Количество снежинок адаптируется к площади окна
     * @warning Вызывается при каждом изменении размеров окна
     */
    private void updateSnowflakesForWindow() {
        if (snowPane == null || snowflakes.isEmpty()) return;
        
        Stage stage = (Stage) snowPane.getScene().getWindow();
        double windowWidth = stage.getWidth();
        double windowHeight = stage.getHeight();
        
        // Обновляем позиции существующих снежинок
        for (SnowflakeData snowflake : snowflakes) {
            javafx.scene.shape.Circle circle = snowflake.circle;
            
            // Если снежинка за пределами новой ширины
            if (circle.getTranslateX() > windowWidth) {
                circle.setTranslateX(random.nextDouble() * windowWidth);
            }
            
            // Если снежинка за пределами новой высоты
            if (circle.getTranslateY() > windowHeight) {
                circle.setTranslateY(random.nextDouble() * windowHeight);
            }
        }
        
        // Добавляем новые снежинки если окно увеличилось
        int currentCount = snowflakes.size();
        int desiredCount = (int)(windowWidth * windowHeight / 4000);
        desiredCount = Math.min(Math.max(desiredCount, 80), 150);
        
        if (currentCount < desiredCount) {
            for (int i = currentCount; i < desiredCount; i++) {
                javafx.scene.shape.Circle snowflake = new javafx.scene.shape.Circle();
                
                double size = 1 + random.nextDouble() * 2;
                snowflake.setRadius(size);
                
                double x = random.nextDouble() * windowWidth;
                double y = random.nextDouble() * windowHeight;
                
                snowflake.setTranslateX(x);
                snowflake.setTranslateY(y);
                snowflake.setOpacity(0.1 + random.nextDouble() * 0.6);
                snowflake.setFill(Color.WHITE);
                snowflake.setEffect(new javafx.scene.effect.Glow(0.2));
                
                double speed = 20 + random.nextDouble() * 40;
                double sway = random.nextDouble() * 2 - 1;
                
                snowflakes.add(new SnowflakeData(snowflake, speed, sway));
                snowPane.getChildren().add(snowflake);
            }
        }
    }
    
    /**
     * @brief Запускает анимацию снежинок
     * @details Создает и запускает AnimationTimer, который обновляет позиции
     *          снежинок каждый кадр анимации. Обеспечивает плавное падение
     *          снежинок с эффектами покачивания и мерцания.
     * 
     * @note Использует дельта-время для независимой от частоты кадров анимации
     * @see AnimationTimer
     * @see #updateSnowflakesAnimation(double)
     */
    private void startSnowAnimation() {
        if (snowTimer != null) {
            snowTimer.stop();
        }
        
        snowTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                updateSnowflakesAnimation(elapsedSeconds);
                lastUpdate = now;
            }
        };
        snowTimer.start();
    }
    
    /**
     * @brief Обновляет анимацию снежинок
     * @details Вычисляет новые позиции для каждой снежинки на основе
     *          прошедшего времени, скорости падения и эффектов анимации.
     *          Обеспечивает циклическое движение снежинок (при выходе за
     *          нижнюю границу они появляются сверху).
     * 
     * @param deltaTime Время, прошедшее с предыдущего обновления (в секундах)
     * 
     * @note Включает эффекты: падение, покачивание, мерцание
     */
    private void updateSnowflakesAnimation(double deltaTime) {
        if (snowPane == null) return;
        
        // Получаем размеры всего окна
        Stage stage = (Stage) snowPane.getScene().getWindow();
        double windowWidth = stage.getWidth();
        double windowHeight = stage.getHeight();
        long time = System.currentTimeMillis();
        
        for (SnowflakeData snowflake : snowflakes) {
            javafx.scene.shape.Circle circle = snowflake.circle;
            
            // Двигаем снежинку вниз
            double newY = circle.getTranslateY() + snowflake.speed * deltaTime;
            
            // Покачивание из стороны в сторону
            double currentX = circle.getTranslateX();
            double swayOffset = Math.sin(time * 0.001 + snowflake.sway) * snowflake.circle.getRadius() * 3;
            double newX = currentX + swayOffset * deltaTime * 10;
            
            // Если снежинка упала за нижнюю границу окна
            if (newY > windowHeight) {
                newY = 0;
                newX = random.nextDouble() * windowWidth;
            }
            
            // Если снежинка вышла за боковые границы окна
            if (newX < 0) newX = windowWidth;
            if (newX > windowWidth) newX = 0;
            
            circle.setTranslateX(newX);
            circle.setTranslateY(newY);
            
            // Легкое мерцание
            double flicker = 0.5 + 0.5 * Math.sin(time * 0.003 + currentX);
            circle.setOpacity(flicker * (0.1 + random.nextDouble() * 0.5));
        }
    }

    /**
     * @brief Загружает задачи из файла и отображает их в таблице
     * @details Загружает список задач из JSON файла с помощью JsonUtil,
     *          передает их в TaskManager и обновляет таблицу отображения.
     * 
     * @note Использует JsonUtil для сериализации/десериализации данных
     * @see JsonUtil
     * @see TaskManager
     * @see #refreshTable()
     * @see #updateStatusLabel()
     */
    private void loadTasks() {
        List<Task> tasks = JsonUtil.loadTasks();
        taskManager.loadTasks(tasks);
        refreshTable();
    }
    
    /**
     * @brief Обновляет содержимое таблицы задач
     * @details Очищает текущий список задач, загружает все задачи из TaskManager,
     *          преобразует их в TaskViewModel и устанавливает обновленный список
     *          в таблицу. Затем применяет сортировку по умолчанию.
     * 
     * @note Автоматически вызывается после изменений в списке задач
     * @see TaskViewModel
     * @see #sortByPriority()
     */
    private void refreshTable() {
        taskList.clear();
        List<Task> tasks = taskManager.getAllTasks();
        
        for (Task task : tasks) {
            taskList.add(new TaskViewModel(task));
        }
        
        // Устанавливаем список в таблицу
        taskTable.setItems(taskList);
        
        // Сортируем по умолчанию: сначала важные, потом по дате
        sortByPriorityAscending = false; // Важные сверху
        sortByPriority(); // Применяем сортировку
    }
    
    /**
     * @brief Обновляет статусную строку с информацией о задачах
     * @details Рассчитывает и отображает статистику по задачам:
     *          общее количество, количество выполненных и просроченных задач.
     *          Используется для предоставления пользователю сводной информации.
     * 
     * @note Вызывается после каждого изменения состояния задач
     * @see TaskManager#getAllTasks()
     * @see TaskManager#getTaskCount(boolean)
     * @see TaskManager#getOverdueTaskCount()
     */
    private void updateStatusLabel() {
        int total = taskManager.getAllTasks().size();
        int completed = taskManager.getTaskCount(true);
        int overdue = taskManager.getOverdueTaskCount();
        
        statusLabel.setText(String.format(
            "Всего задач: %d | Выполнено: %d | Просрочено: %d",
            total, completed, overdue
        ));
    }
    
    /**
     * @brief Обработчик кнопки добавления новой задачи
     * @details Открывает модальное диалоговое окно для создания новой задачи.
     *          Загружает FXML файл диалога, инициализирует контроллер и отображает окно.
     *          После закрытия диалога проверяет, была ли создана новая задача,
     *          и если да, добавляет её в менеджер задач и сохраняет изменения.
     * 
     * @event OnAction кнопки "Добавить"
     * 
     * @exception IOException если не удается загрузить FXML файл диалога
     * @see NewTaskController
     * @see JsonUtil#saveTasks(List)
     * @see #refreshTable()
     * @see #updateStatusLabel()
     */
    @FXML
    private void handleAddTask() {
        try {
            // Загружаем диалоговое окно
            FXMLLoader loader = new FXMLLoader(getClass().getResource("NewTaskDialog.fxml"));
            Parent root = loader.load();
            NewTaskController controller = loader.getController();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Новая задача");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            
            // Устанавливаем обработчик закрытия окна
            dialogStage.setOnHidden(event -> {
                if (controller.isTaskCreated()) {
                    Task newTask = controller.getCreatedTask();
                    taskManager.addTask(newTask);
                    JsonUtil.saveTasks(taskManager.getAllTasks());
                    refreshTable();
                    updateStatusLabel();
                }
            });
            
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось открыть окно создания задачи", e.getMessage());
        }
    }
    
    /**
     * @brief Обработчик кнопки удаления задачи
     * @details Удаляет выбранную в таблице задачу после подтверждения пользователем.
     *          Показывает диалог подтверждения удаления. Если пользователь соглашается,
     *          задача удаляется из менеджера задач, изменения сохраняются в файл,
     *          а таблица и статусная строка обновляются.
     * 
     * @event OnAction кнопки "Удалить"
     * 
     * @note Требует предварительного выбора задачи в таблице
     * @warning Безвозвратно удаляет задачу после подтверждения
     * 
     * @see TaskManager#removeTask(String)
     * @see JsonUtil#saveTasks(List)
     * @see #refreshTable()
     * @see #updateStatusLabel()
     * @see #showAlert(String, String, String)
     */
    @FXML
    private void handleDeleteTask() {
        TaskViewModel selectedTask = taskTable.getSelectionModel().getSelectedItem();
        
        if (selectedTask == null) {
            showAlert("Предупреждение", "Не выбрана задача", 
                     "Пожалуйста, выберите задачу для удаления из таблицы.");
            return;
        }
        
        // Подтверждение удаления
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удалить задачу?");
        alert.setContentText("Вы действительно хотите удалить задачу: " + selectedTask.getTitle() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean removed = taskManager.removeTask(selectedTask.getId());
            if (removed) {
                JsonUtil.saveTasks(taskManager.getAllTasks());
                refreshTable();
                updateStatusLabel();
                showAlert("Успех", "Задача удалена", 
                         "Задача успешно удалена из списка.");
            }
        }
    }
    
    /**
     * @brief Выполняет поиск задач по текстовому запросу
     * @details Фильтрует список задач на основе текстового запроса из поля поиска.
     *          Поиск выполняется в реальном времени по мере ввода текста.
     *          Обновляет таблицу, отображая только задачи, соответствующие запросу.
     * 
     * @note Вызывается слушателем изменения текста в поле поиска
     * @see TaskManager#searchTasks(String)
     * @see TaskViewModel
     */
    private void searchTasks() {
        String query = searchField.getText().trim();
        List<Task> foundTasks = taskManager.searchTasks(query);
        
        taskList.clear();
        for (Task task : foundTasks) {
            taskList.add(new TaskViewModel(task));
        }
    }
    
    /**
     * @brief Показывает информационное диалоговое окно
     * @details Создает и отображает стандартное диалоговое окно с заданными
     *          заголовком, заголовочным текстом и содержанием.
     *          Используется для уведомлений, предупреждений и сообщений об ошибках.
     * 
     * @param title Заголовок диалогового окна
     * @param header Заголовочный текст диалога (может быть null)
     * @param content Основное текстовое содержание сообщения
     * 
     * @note Использует AlertType.INFORMATION для информационных сообщений
     * @see Alert
     */
    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * @brief Обработчик закрытия приложения
     * @details Останавливает анимацию снежинок и закрывает главное окно приложения.
     *          Вызывается при завершении работы программы для корректного освобождения ресурсов.
     * 
     * @event Может быть привязан к кнопке закрытия или пункту меню "Выход"
     * 
     * @note Останавливает AnimationTimer для предотвращения утечек ресурсов
     * @see AnimationTimer#stop()
     */
    @FXML
    private void handleExit() {
        // Останавливаем анимацию снежинок
        if (snowTimer != null) {
            snowTimer.stop();
        }
        Stage stage = (Stage) taskTable.getScene().getWindow();
        stage.close();
    }
}