package view;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;

import model.Category;
import model.Priority;
import model.Task;
import model.TaskManager;
import repository.DatabaseManager;
import util.AudioManager;
import util.NotificationSettings;
import util.PathResolver;
import util.TaskReminderService;
import util.ThemeManager;
import viewmodel.TaskViewModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер главного окна.
 */
public class MainController {
    private enum SortMode {
        MANUAL,
        DATE,
        PRIORITY
    }

    @FXML private TabPane mainTabPane;

    @FXML private TableView<TaskViewModel> taskTable;
    @FXML private TableColumn<TaskViewModel, String> titleColumn;
    @FXML private TableColumn<TaskViewModel, String> descriptionColumn;
    @FXML private TableColumn<TaskViewModel, LocalDateTime> deadlineColumn;
    @FXML private TableColumn<TaskViewModel, Priority> priorityColumn;
    @FXML private TableColumn<TaskViewModel, Category> categoryColumn;
    @FXML private TableColumn<TaskViewModel, Boolean> statusColumn;

    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button sortByDateButton;
    @FXML private Button sortByPriorityButton;
    @FXML private Button focusButton;
    @FXML private MenuButton bulkMenuButton;
    @FXML private MenuItem bulkCompleteItem;
    @FXML private MenuItem bulkPriorityItem;
    @FXML private MenuItem bulkCategoryItem;
    @FXML private MenuItem bulkDeleteItem;

    @FXML private Label appTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label insightLabel;

    @FXML private DatePicker calendarPicker;
    @FXML private Button calendarAddButton;
    @FXML private Button calendarExportButton;
    @FXML private ListView<TaskViewModel> calendarTaskList;
    @FXML private Label calendarSummaryLabel;

    @FXML private PieChart completionChart;
    @FXML private PieChart categoryChart;
    @FXML private BarChart<String, Number> priorityChart;
    @FXML private LineChart<String, Number> timelineChart;

    @FXML private ComboBox<String> themeCombo;
    @FXML private ColorPicker customBgColorPicker;
    @FXML private ColorPicker customAccentColorPicker;
    @FXML private Button saveCustomThemeButton;

    @FXML private CheckBox notifyPopupCheck;
    @FXML private CheckBox notifySoundCheck;
    @FXML private CheckBox notifyEmailCheck;
    @FXML private Spinner<Integer> reminderValueSpinner;
    @FXML private ChoiceBox<String> reminderUnitChoice;
    @FXML private TextField notifyEmailField;
    @FXML private Button saveNotificationButton;

    @FXML private CheckBox musicEnabledCheck;
    @FXML private javafx.scene.control.Slider volumeSlider;
    @FXML private Label audioCountLabel;

    @FXML private CheckBox widgetEnabledCheck;
    @FXML private Button widgetRefreshButton;

    private final TaskManager taskManager = TaskManager.getInstance();
    private final AudioManager audioManager = AudioManager.getInstance();
    private final TaskReminderService reminderService = TaskReminderService.getInstance();
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    private final ObservableList<TaskViewModel> taskList = FXCollections.observableArrayList();
    private FilteredList<TaskViewModel> filteredTasks;

    private SortMode sortMode = SortMode.MANUAL;
    private boolean sortByDateAscending = true;
    private boolean sortByPriorityAscending = false;

    private TaskViewModel draggedTask;

    private Stage widgetStage;
    private ListView<String> widgetListView;
    private final ObservableList<String> widgetItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        appTitleLabel.setText("ToDoList");

        setupTableColumns();
        setupTableDataFlow();
        setupSearch();
        setupButtons();
        setupTableInteractions();
        setupCalendarTab();
        setupStatsTab();
        setupThemeSettings();
        setupNotificationSettings();
        setupMusicSettings();
        setupWidgetSettings();

        loadTasks();
        refreshAllViews();

        audioManager.refreshPlaylist();
        audioCountLabel.setText("Треков: " + audioManager.getAudioFilesCount());
        if (musicEnabledCheck.isSelected()) {
            audioManager.startPlaylist();
        }
        reminderService.start();
    }

    private void setupTableColumns() {
        taskTable.setEditable(true);

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        deadlineColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndTime()));
        priorityColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getPriority()));
        categoryColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCategory()));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("completed"));

        setupTitleColumn();
        setupDescriptionColumn();
        setupDeadlineColumn();
        setupPriorityColumn();
        setupCategoryColumn();
        setupStatusColumn();
    }

    private void setupTableDataFlow() {
        filteredTasks = new FilteredList<>(taskList, task -> true);
        taskTable.setItems(filteredTasks);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        taskTable.setPlaceholder(new Label("Задач пока нет. Добавьте первую задачу."));
        taskTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void setupTitleColumn() {
        titleColumn.setCellFactory(col -> new TextFieldTableCell<>(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : value;
            }

            @Override
            public String fromString(String value) {
                return value == null ? "" : value.trim();
            }
        }) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                TaskViewModel task = getRowTask(this);
                String prefix = task != null && task.isOverdue() && !task.isCompleted() ? "⚠ " : "";
                if (!isEditing()) {
                    setText(prefix + item);
                }
                if (task != null && task.isCompleted()) {
                    setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                } else if (task != null && task.isOverdue()) {
                    setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        titleColumn.setOnEditCommit(event -> {
            TaskViewModel task = event.getRowValue();
            if (task == null) {
                return;
            }
            String value = event.getNewValue();
            if (value == null || value.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Ошибка", "Пустой заголовок", "Задача должна иметь название.");
                taskTable.refresh();
                return;
            }
            task.setTitle(value.trim());
            persistTask(task);
        });
    }

    private void setupDescriptionColumn() {
        descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionColumn.setOnEditCommit(event -> {
            TaskViewModel task = event.getRowValue();
            if (task == null) {
                return;
            }
            String value = event.getNewValue();
            task.setDescription(value == null ? "" : value.trim());
            persistTask(task);
        });
    }

    private void setupDeadlineColumn() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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
                            return;
                        }

                        TaskViewModel task = getRowTask(this);
                        setText(item.format(formatter));

                        if (task != null && task.isCompleted()) {
                            setStyle("-fx-text-fill: #7f8c8d;");
                        } else if (task != null && task.isOverdue()) {
                            setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                };
            }
        });
    }

    private void setupPriorityColumn() {
        StringConverter<Priority> converter = new StringConverter<>() {
            @Override
            public String toString(Priority priority) {
                return priority == null ? "" : priority.getDisplayName();
            }

            @Override
            public Priority fromString(String string) {
                return Priority.fromDisplayName(string);
            }
        };

        priorityColumn.setCellFactory(col -> new ComboBoxTableCell<>(converter, Priority.values()) {
            @Override
            public void updateItem(Priority item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item.getDisplayName());
                setStyle(
                    "-fx-text-fill: white;" +
                    "-fx-background-color: " + item.getColor() + ";" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 3 8;"
                );
            }
        });

        priorityColumn.setOnEditCommit(event -> {
            TaskViewModel task = event.getRowValue();
            if (task == null || event.getNewValue() == null) {
                return;
            }
            task.setPriority(event.getNewValue());
            persistTask(task);
        });
    }

    private void setupCategoryColumn() {
        StringConverter<Category> converter = new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getDisplayName();
            }

            @Override
            public Category fromString(String string) {
                return Category.fromDisplayName(string);
            }
        };

        categoryColumn.setCellFactory(ComboBoxTableCell.forTableColumn(converter, Category.values()));
        categoryColumn.setOnEditCommit(event -> {
            TaskViewModel task = event.getRowValue();
            if (task == null || event.getNewValue() == null) {
                return;
            }
            task.setCategory(event.getNewValue());
            persistTask(task);
        });
    }

    private void setupStatusColumn() {
        statusColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<TaskViewModel, Boolean> call(TableColumn<TaskViewModel, Boolean> param) {
                return new TableCell<>() {
                    private final CheckBox checkBox = new CheckBox();

                    {
                        checkBox.setOnAction(event -> {
                            TaskViewModel task = getRowTask(this);
                            if (task == null) {
                                return;
                            }
                            boolean completed = checkBox.isSelected();
                            task.setCompleted(completed);
                            taskManager.markAsCompleted(task.getId(), completed);
                            reloadTasksFromManager();
                        });
                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            return;
                        }
                        TaskViewModel task = getRowTask(this);
                        if (task == null) {
                            setGraphic(null);
                            return;
                        }
                        checkBox.setSelected(Boolean.TRUE.equals(task.completedProperty().get()));
                        setGraphic(checkBox);
                    }
                };
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredTasks.setPredicate(task -> {
                if (query.isEmpty()) {
                    return true;
                }
                return task.getTitle().toLowerCase().contains(query)
                    || task.getDescription().toLowerCase().contains(query)
                    || task.getCategoryDisplay().toLowerCase().contains(query);
            });
        });
    }

    private void setupButtons() {
        sortByDateButton.setText("По дате ↓");
        sortByPriorityButton.setText("По приоритету ↑");
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        bulkMenuButton.setDisable(true);

        taskTable.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<TaskViewModel>) change -> {
                boolean hasSelection = !taskTable.getSelectionModel().getSelectedItems().isEmpty();
                editButton.setDisable(!hasSelection);
                deleteButton.setDisable(!hasSelection);
                bulkMenuButton.setDisable(!hasSelection);
            }
        );
    }

    private void setupTableInteractions() {
        taskTable.setRowFactory(tv -> {
            TableRow<TaskViewModel> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog(row.getItem());
                }
            });

            row.setOnDragDetected(event -> {
                if (row.isEmpty()) {
                    return;
                }
                if (!isManualOrderAllowed()) {
                    showAlert(Alert.AlertType.INFORMATION, "Порядок", "Перетаскивание временно отключено",
                        "Сначала очистите поиск, чтобы вручную менять порядок задач.");
                    return;
                }
                draggedTask = row.getItem();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(draggedTask.getId());
                db.setContent(content);
                event.consume();
            });

            row.setOnDragOver(event -> {
                if (draggedTask == null || row.isEmpty()) {
                    return;
                }
                if (row.getItem() != draggedTask) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                if (draggedTask == null || row.isEmpty()) {
                    return;
                }
                TaskViewModel target = row.getItem();
                int draggedIdx = taskList.indexOf(draggedTask);
                int targetIdx = taskList.indexOf(target);
                if (draggedIdx < 0 || targetIdx < 0) {
                    return;
                }
                if (draggedIdx != targetIdx) {
                    taskList.remove(draggedIdx);
                    int insertIndex = targetIdx;
                    if (insertIndex > taskList.size()) {
                        insertIndex = taskList.size();
                    }
                    taskList.add(insertIndex, draggedTask);
                    persistOrder();
                }
                draggedTask = null;
                event.setDropCompleted(true);
                refreshAllViews();
            });

            row.setOnDragDone(event -> draggedTask = null);

            ContextMenu menu = buildContextMenu(row);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings
                .when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(menu));

            return row;
        });
    }

    private ContextMenu buildContextMenu(TableRow<TaskViewModel> row) {
        MenuItem edit = new MenuItem("Редактировать");
        edit.setOnAction(event -> openEditDialog(row.getItem()));

        MenuItem duplicate = new MenuItem("Копировать");
        duplicate.setOnAction(event -> duplicateTask(row.getItem()));

        MenuItem reminder = new MenuItem("Настроить напоминание");
        reminder.setOnAction(event -> adjustReminder(row.getItem()));

        MenuItem delete = new MenuItem("Удалить");
        delete.setOnAction(event -> deleteTasks(List.of(row.getItem())));

        return new ContextMenu(edit, duplicate, reminder, delete);
    }

    private void setupCalendarTab() {
        calendarPicker.setValue(LocalDate.now());
        calendarTaskList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TaskViewModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String text = String.format("%s — %s (%s)",
                    item.getFormattedStartTime(),
                    item.getTitle(),
                    item.getPriorityDisplay());
                setText(text);
            }
        });
        updateCalendarView();
    }

    private void setupStatsTab() {
        completionChart.setAnimated(false);
        categoryChart.setAnimated(false);
        priorityChart.setAnimated(false);
        timelineChart.setAnimated(false);
    }

    private void setupThemeSettings() {
        themeCombo.getItems().setAll(ThemeManager.getAvailableThemes());
        themeCombo.setValue(ThemeManager.getCurrentTheme());

        customBgColorPicker.setValue(Color.web("#8fd3ff"));
        customAccentColorPicker.setValue(Color.web("#ffb84e"));

        themeCombo.setOnAction(event -> applySelectedTheme());
        saveCustomThemeButton.setOnAction(event -> handleSaveCustomTheme());

        taskTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applySelectedTheme();
            }
        });
    }

    private void setupNotificationSettings() {
        NotificationSettings settings = NotificationSettings.load();
        notifyPopupCheck.setSelected(settings.isPopupEnabled());
        notifySoundCheck.setSelected(settings.isSoundEnabled());
        notifyEmailCheck.setSelected(settings.isEmailEnabled());
        notifyEmailField.setText(settings.getEmailTo());

        reminderUnitChoice.getItems().addAll("мин", "час", "день");
        reminderValueSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10080, 15));
        setReminderSpinner(settings.getReminderOffsetMinutes());

        notifyEmailField.setDisable(!notifyEmailCheck.isSelected());
        notifyEmailCheck.selectedProperty().addListener((obs, oldVal, newVal) -> notifyEmailField.setDisable(!newVal));
    }

    private void setupMusicSettings() {
        boolean enabled = Boolean.parseBoolean(dbManager.getSetting("music_enabled", "true"));
        double volume = parseDouble(dbManager.getSetting("music_volume", "0.35"), 0.35);

        musicEnabledCheck.setSelected(enabled);
        volumeSlider.setValue(volume);
        audioManager.setMasterVolume(volume);
        audioManager.setSoundsEnabled(enabled);

        audioCountLabel.setText("Треков: " + audioManager.getAudioFilesCount());

        musicEnabledCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setSoundsEnabled(newVal);
            dbManager.saveSetting("music_enabled", String.valueOf(newVal));
            if (newVal) {
                audioManager.startPlaylist();
            } else {
                audioManager.stopAllSounds();
            }
        });

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double vol = newVal == null ? 0.35 : newVal.doubleValue();
            audioManager.setMasterVolume(vol);
            dbManager.saveSetting("music_volume", String.format(Locale.US, "%.2f", vol));
        });
    }

    private void setupWidgetSettings() {
        boolean enabled = Boolean.parseBoolean(dbManager.getSetting("widget_enabled", "false"));
        widgetEnabledCheck.setSelected(enabled);
        widgetRefreshButton.setDisable(!enabled);

        widgetEnabledCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            dbManager.saveSetting("widget_enabled", String.valueOf(newVal));
            widgetRefreshButton.setDisable(!newVal);
            if (newVal) {
                showWidget();
            } else {
                hideWidget();
            }
        });

        widgetRefreshButton.setOnAction(event -> updateWidget());

        if (enabled) {
            Platform.runLater(this::showWidget);
        }
    }

    private void loadTasks() {
        taskList.clear();
        for (Task task : taskManager.getAllTasks()) {
            taskList.add(new TaskViewModel(task));
        }
        applySortMode();
    }

    @FXML
    private void sortByDate() {
        if (taskList.isEmpty()) {
            return;
        }
        sortMode = SortMode.DATE;
        sortByDateAscending = !sortByDateAscending;
        applySortMode();
    }

    @FXML
    private void sortByPriority() {
        if (taskList.isEmpty()) {
            return;
        }
        sortMode = SortMode.PRIORITY;
        sortByPriorityAscending = !sortByPriorityAscending;
        applySortMode();
    }

    private void applySortMode() {
        if (sortMode == SortMode.MANUAL) {
            Comparator<TaskViewModel> comparator = Comparator
                .comparingInt((TaskViewModel vm) -> vm.getTask().getSortIndex())
                .thenComparing(TaskViewModel::getEndTime);
            FXCollections.sort(taskList, comparator);
            return;
        }

        if (sortMode == SortMode.DATE) {
            Comparator<TaskViewModel> comparator = Comparator.comparing(TaskViewModel::getEndTime);
            if (!sortByDateAscending) {
                comparator = comparator.reversed();
            }
            FXCollections.sort(taskList, comparator);
            sortByDateButton.setText(sortByDateAscending ? "По дате ↑" : "По дате ↓");
            return;
        }

        Comparator<TaskViewModel> comparator = Comparator
            .comparing((TaskViewModel vm) -> priorityRank(vm.getPriority()))
            .thenComparing(TaskViewModel::getEndTime);
        if (sortByPriorityAscending) {
            comparator = comparator.reversed();
        }
        FXCollections.sort(taskList, comparator);
        sortByPriorityButton.setText(sortByPriorityAscending ? "По приоритету ↓" : "По приоритету ↑");
    }

    @FXML
    private void handleAddTask() {
        Task created = showTaskDialog(null, null);
        if (created == null) {
            return;
        }
        taskManager.addTask(created);
        taskList.add(new TaskViewModel(created));
        applySortMode();
        refreshAllViews();
    }

    @FXML
    private void handleAddTaskForDate() {
        LocalDate selected = calendarPicker.getValue();
        Task created = showTaskDialog(null, selected);
        if (created == null) {
            return;
        }
        taskManager.addTask(created);
        taskList.add(new TaskViewModel(created));
        applySortMode();
        refreshAllViews();
    }

    @FXML
    private void handleEditTask() {
        TaskViewModel selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Редактирование", "Задача не выбрана",
                "Выберите задачу в таблице перед редактированием.");
            return;
        }
        openEditDialog(selected);
    }

    private void openEditDialog(TaskViewModel selected) {
        if (selected == null) {
            return;
        }
        Task updated = showTaskDialog(selected.getTask(), null);
        if (updated == null) {
            return;
        }
        taskManager.updateTask(updated);
        selected.updateFromModel();
        applySortMode();
        refreshAllViews();
        taskTable.refresh();
    }

    @FXML
    private void handleDeleteTask() {
        List<TaskViewModel> selected = new ArrayList<>(taskTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Удаление", "Задача не выбрана",
                "Выберите задачу в таблице перед удалением.");
            return;
        }
        deleteTasks(selected);
    }

    @FXML
    private void handleBulkComplete() {
        List<TaskViewModel> selected = new ArrayList<>(taskTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        for (TaskViewModel task : selected) {
            task.setCompleted(true);
            taskManager.markAsCompleted(task.getId(), true);
        }
        reloadTasksFromManager();
    }

    @FXML
    private void handleBulkPriority() {
        List<TaskViewModel> selected = new ArrayList<>(taskTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        Priority chosen = showPriorityDialog();
        if (chosen == null) {
            return;
        }
        for (TaskViewModel task : selected) {
            task.setPriority(chosen);
            taskManager.updateTask(task.getTask());
        }
        refreshAllViews();
        taskTable.refresh();
    }

    @FXML
    private void handleBulkCategory() {
        List<TaskViewModel> selected = new ArrayList<>(taskTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        Category chosen = showCategoryDialog();
        if (chosen == null) {
            return;
        }
        for (TaskViewModel task : selected) {
            task.setCategory(chosen);
            taskManager.updateTask(task.getTask());
        }
        refreshAllViews();
        taskTable.refresh();
    }

    @FXML
    private void handleBulkDelete() {
        List<TaskViewModel> selected = new ArrayList<>(taskTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        deleteTasks(selected);
    }

    @FXML
    private void handleFocusMode() {
        List<Task> focusTasks = taskManager.getFocusTasks(3);
        if (focusTasks.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Фокус дня", "Все задачи выполнены",
                "Можно отдыхать или добавить новые цели.");
            return;
        }

        String content = focusTasks.stream()
            .map(task -> String.format("• %s (%s, до %s)",
                task.getTitle(),
                task.getPriority().getDisplayName(),
                task.getFormattedEndTime()))
            .collect(Collectors.joining("\n"));

        showAlert(Alert.AlertType.INFORMATION, "Фокус дня", "Топ-3 задачи на ближайшее время", content);
    }

    @FXML
    private void handleExportCalendar() {
        Path exportPath = PathResolver.getDataDir().resolve("todolist_calendar.ics");
        try {
            Files.createDirectories(exportPath.getParent());
            Files.writeString(exportPath, buildIcs());
            showAlert(Alert.AlertType.INFORMATION, "Экспорт календаря", "Файл создан",
                "Сохранено в: " + exportPath.toAbsolutePath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Экспорт календаря", "Не удалось создать файл", e.getMessage());
        }
    }

    @FXML
    private void handleCalendarDateChange() {
        updateCalendarView();
    }

    @FXML
    private void handleSaveNotificationSettings() {
        if (notifyEmailCheck.isSelected() && notifyEmailField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Уведомления", "Email не заполнен",
                "Укажите email или отключите уведомления по email.");
            return;
        }
        int minutes = toMinutes(reminderValueSpinner.getValue(), reminderUnitChoice.getValue());

        dbManager.saveSetting("notify_popup", String.valueOf(notifyPopupCheck.isSelected()));
        dbManager.saveSetting("notify_sound", String.valueOf(notifySoundCheck.isSelected()));
        dbManager.saveSetting("notify_email", String.valueOf(notifyEmailCheck.isSelected()));
        dbManager.saveSetting("notify_email_to", notifyEmailField.getText().trim());
        dbManager.saveSetting("reminder_minutes", String.valueOf(minutes));

        showAlert(Alert.AlertType.INFORMATION, "Уведомления", "Настройки сохранены",
            "Напоминания будут обновлены автоматически.");
    }

    @FXML
    private void handleSaveCustomTheme() {
        Color bg = customBgColorPicker.getValue() == null ? Color.web("#8fd3ff") : customBgColorPicker.getValue();
        Color accent = customAccentColorPicker.getValue() == null ? Color.web("#ffb84e") : customAccentColorPicker.getValue();

        String css = buildCustomThemeCss(bg, accent);
        Path customFile = ThemeManager.getCustomThemePath();
        try {
            Files.createDirectories(customFile.getParent());
            Files.writeString(customFile, css);
            ThemeManager.setTheme("Custom");
            themeCombo.setValue("Custom");
            applySelectedTheme();
            showAlert(Alert.AlertType.INFORMATION, "Тема", "Кастомная тема сохранена",
                "Файл: " + customFile.toAbsolutePath());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Тема", "Не удалось сохранить тему", e.getMessage());
        }
    }

    @FXML
    private void handleToggleWidget() {
        if (widgetEnabledCheck.isSelected()) {
            showWidget();
        } else {
            hideWidget();
        }
    }

    @FXML
    private void handleRefreshWidget() {
        updateWidget();
    }

    private void applySelectedTheme() {
        String theme = themeCombo.getValue();
        ThemeManager.setTheme(theme);
        Scene scene = taskTable.getScene();
        if (scene != null) {
            ThemeManager.applyTheme(scene, theme);
        }
        if (widgetStage != null && widgetStage.getScene() != null) {
            ThemeManager.applyTheme(widgetStage.getScene(), theme);
        }
    }

    private void refreshAllViews() {
        updateStatusLabel();
        updateInsightLabel();
        updateStatisticsCharts();
        updateCalendarView();
        updateWidget();
    }

    private void updateStatusLabel() {
        int total = taskManager.getAllTasks().size();
        int completed = taskManager.getTaskCount(true);
        int overdue = taskManager.getOverdueTaskCount();
        statusLabel.setText(String.format("Всего: %d | Выполнено: %d | Просрочено: %d", total, completed, overdue));
    }

    private void updateInsightLabel() {
        List<Task> next = taskManager.getUpcomingTasks(180);
        if (next.isEmpty()) {
            insightLabel.setText("Сегодня свободное окно: ближайших стартов нет.");
            return;
        }

        Task nearest = next.get(0);
        insightLabel.setText("Ближайший старт: " + nearest.getTitle() + " в " + nearest.getFormattedStartTime());
    }

    private void updateStatisticsCharts() {
        List<Task> tasks = taskManager.getAllTasks();

        long completed = tasks.stream().filter(Task::isCompleted).count();
        long overdue = tasks.stream().filter(task -> !task.isCompleted() && task.isOverdue()).count();
        long active = tasks.size() - completed - overdue;

        completionChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Выполнено", completed),
            new PieChart.Data("Активные", active),
            new PieChart.Data("Просрочено", overdue)
        ));

        List<PieChart.Data> categoryData = new ArrayList<>();
        for (Category category : Category.values()) {
            long count = tasks.stream().filter(task -> task.getCategory() == category).count();
            if (count > 0) {
                categoryData.add(new PieChart.Data(category.getDisplayName(), count));
            }
        }
        categoryChart.setData(FXCollections.observableArrayList(categoryData));

        XYChart.Series<String, Number> prioritySeries = new XYChart.Series<>();
        for (Priority priority : Priority.values()) {
            long count = tasks.stream().filter(task -> task.getPriority() == priority && !task.isCompleted()).count();
            prioritySeries.getData().add(new XYChart.Data<>(priority.getDisplayName(), count));
        }
        priorityChart.getData().setAll(prioritySeries);

        XYChart.Series<String, Number> timelineSeries = new XYChart.Series<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            long count = tasks.stream()
                .filter(task -> task.getEndTime().toLocalDate().equals(date))
                .count();
            timelineSeries.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("dd.MM")), count));
        }
        timelineChart.getData().setAll(timelineSeries);
    }

    private void updateCalendarView() {
        if (calendarPicker.getValue() == null) {
            return;
        }
        LocalDate date = calendarPicker.getValue();
        List<TaskViewModel> matches = taskList.stream()
            .filter(task -> task.getStartTime().toLocalDate().equals(date)
                || task.getEndTime().toLocalDate().equals(date))
            .sorted(Comparator.comparing(TaskViewModel::getStartTime))
            .collect(Collectors.toList());

        calendarTaskList.setItems(FXCollections.observableArrayList(matches));
        calendarSummaryLabel.setText("Задач: " + matches.size());
    }

    private void reloadTasksFromManager() {
        taskList.clear();
        for (Task task : taskManager.getAllTasks()) {
            taskList.add(new TaskViewModel(task));
        }
        applySortMode();
        refreshAllViews();
        taskTable.refresh();
    }

    private void persistTask(TaskViewModel task) {
        taskManager.updateTask(task.getTask());
        refreshAllViews();
    }

    private void persistOrder() {
        sortMode = SortMode.MANUAL;
        List<Task> ordered = taskList.stream().map(TaskViewModel::getTask).collect(Collectors.toList());
        taskManager.updateSortOrder(ordered);
    }

    private void duplicateTask(TaskViewModel original) {
        if (original == null) {
            return;
        }
        Task copy = new Task(
            original.getTitle() + " (копия)",
            original.getDescription(),
            original.getStartTime(),
            original.getEndTime(),
            original.getPriority(),
            original.getCategory()
        );
        copy.setRecurrenceRule(original.getTask().getRecurrenceRule());
        copy.setReminderOffsetMinutes(original.getTask().getReminderOffsetMinutes());
        copy.setRecurrenceEnd(original.getTask().getRecurrenceEnd());
        taskManager.addTask(copy);
        taskList.add(new TaskViewModel(copy));
        applySortMode();
        refreshAllViews();
    }

    private void adjustReminder(TaskViewModel task) {
        if (task == null) {
            return;
        }
        int current = task.getTask().getReminderOffsetMinutes();
        Optional<Integer> result = showReminderDialog(current);
        if (result.isEmpty()) {
            return;
        }
        task.getTask().setReminderOffsetMinutes(result.get());
        taskManager.updateTask(task.getTask());
        refreshAllViews();
    }

    private Optional<Integer> showReminderDialog(int currentMinutes) {
        javafx.scene.control.Dialog<Integer> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Напоминание");
        dialog.setHeaderText("Укажите время напоминания (0 = по умолчанию)");

        javafx.scene.control.ButtonType okButton = new javafx.scene.control.ButtonType("Сохранить", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        Spinner<Integer> spinner = new Spinner<>(0, 10080, 0);
        ChoiceBox<String> unitChoice = new ChoiceBox<>(FXCollections.observableArrayList("мин", "час", "день"));

        int value = currentMinutes;
        String unit = "мин";
        if (value > 0 && value % (24 * 60) == 0) {
            value = value / (24 * 60);
            unit = "день";
        } else if (value > 0 && value % 60 == 0) {
            value = value / 60;
            unit = "час";
        }

        spinner.getValueFactory().setValue(value);
        unitChoice.setValue(unit);

        VBox content = new VBox(10,
            new Label("Смещение:"),
            new VBox(6, spinner, unitChoice)
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                return toMinutes(spinner.getValue(), unitChoice.getValue());
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();
        return result.map(val -> Math.max(0, val));
    }

    private Priority showPriorityDialog() {
        List<String> options = new ArrayList<>();
        for (Priority priority : Priority.values()) {
            options.add(priority.getDisplayName());
        }
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Приоритет");
        dialog.setHeaderText("Выберите приоритет");
        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) {
            return null;
        }
        return Priority.fromDisplayName(choice.get());
    }

    private Category showCategoryDialog() {
        List<String> options = new ArrayList<>();
        for (Category category : Category.values()) {
            options.add(category.getDisplayName());
        }
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Категория");
        dialog.setHeaderText("Выберите категорию");
        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) {
            return null;
        }
        return Category.fromDisplayName(choice.get());
    }

    private void deleteTasks(List<TaskViewModel> selected) {
        if (selected == null || selected.isEmpty()) {
            return;
        }
        String header = selected.size() == 1 ? "Удалить задачу?" : "Удалить выбранные задачи?";
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Подтверждение");
        confirmation.setHeaderText(header);
        confirmation.setContentText(selected.size() == 1 ? selected.get(0).getTitle() : "Количество: " + selected.size());

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        for (TaskViewModel task : selected) {
            taskManager.removeTask(task.getId());
        }
        taskList.removeAll(selected);
        refreshAllViews();
    }

    private Task showTaskDialog(Task editingTask, LocalDate prefillDate) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/NewTaskDialog.fxml"));
            Parent root = loader.load();
            NewTaskController controller = loader.getController();
            if (editingTask != null) {
                controller.setEditingTask(editingTask);
            }
            if (prefillDate != null) {
                controller.setPrefillDate(prefillDate);
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle(editingTask == null ? "Новая задача" : "Редактировать задачу");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            if (controller.isTaskCreated()) {
                return controller.getCreatedTask();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть окно", e.getMessage());
        }
        return null;
    }

    private void showWidget() {
        if (widgetStage != null && widgetStage.isShowing()) {
            updateWidget();
            return;
        }
        widgetListView = new ListView<>(widgetItems);
        widgetListView.setPrefSize(280, 220);

        Label title = new Label("Срочные задачи");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 6 6 0 6;");

        VBox root = new VBox(8, title, widgetListView);
        root.setPrefSize(280, 240);

        widgetStage = new Stage(StageStyle.UTILITY);
        widgetStage.setTitle("ToDoList — Виджет");
        widgetStage.setAlwaysOnTop(true);
        widgetStage.setResizable(false);
        widgetStage.setScene(new Scene(root));
        ThemeManager.applyTheme(widgetStage.getScene(), ThemeManager.getCurrentTheme());
        widgetStage.setOnCloseRequest(event -> widgetEnabledCheck.setSelected(false));
        widgetStage.show();

        updateWidget();
    }

    private void hideWidget() {
        if (widgetStage != null) {
            widgetStage.close();
            widgetStage = null;
            widgetItems.clear();
        }
    }

    private void updateWidget() {
        if (widgetStage == null || !widgetStage.isShowing()) {
            return;
        }
        List<Task> tasks = taskManager.getAllTasks();
        List<Task> urgent = tasks.stream()
            .filter(task -> !task.isCompleted())
            .sorted(Comparator
                .comparing((Task t) -> priorityRank(t.getPriority()))
                .thenComparing(Task::getEndTime))
            .limit(5)
            .collect(Collectors.toList());

        widgetItems.setAll(urgent.stream()
            .map(task -> String.format("%s — %s", task.getFormattedEndTime(), task.getTitle()))
            .collect(Collectors.toList()));
    }

    private boolean isManualOrderAllowed() {
        String query = searchField.getText();
        return query == null || query.trim().isEmpty();
    }

    private TaskViewModel getRowTask(TableCell<TaskViewModel, ?> cell) {
        if (cell == null) {
            return null;
        }
        int idx = cell.getIndex();
        if (idx < 0 || idx >= cell.getTableView().getItems().size()) {
            return null;
        }
        return cell.getTableView().getItems().get(idx);
    }

    private int priorityRank(Priority priority) {
        return switch (priority) {
            case URGENT -> 0;
            case IMPORTANT -> 1;
            case NORMAL -> 2;
        };
    }

    private int toMinutes(Integer value, String unit) {
        int amount = value == null ? 0 : value;
        if (amount <= 0) {
            return 0;
        }
        if ("час".equals(unit)) {
            return amount * 60;
        }
        if ("день".equals(unit)) {
            return amount * 24 * 60;
        }
        return amount;
    }

    private void setReminderSpinner(int minutes) {
        if (minutes <= 0) {
            reminderValueSpinner.getValueFactory().setValue(0);
            reminderUnitChoice.setValue("мин");
            return;
        }
        if (minutes % (24 * 60) == 0) {
            reminderValueSpinner.getValueFactory().setValue(minutes / (24 * 60));
            reminderUnitChoice.setValue("день");
        } else if (minutes % 60 == 0) {
            reminderValueSpinner.getValueFactory().setValue(minutes / 60);
            reminderUnitChoice.setValue("час");
        } else {
            reminderValueSpinner.getValueFactory().setValue(minutes);
            reminderUnitChoice.setValue("мин");
        }
    }

    private String buildIcs() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("VERSION:2.0\n");
        sb.append("PRODID:-//ToDoList//Calendar Export//RU\n");

        for (Task task : taskManager.getAllTasks()) {
            sb.append("BEGIN:VEVENT\n");
            sb.append("UID:").append(task.getId()).append("\n");
            sb.append("DTSTAMP:").append(LocalDateTime.now().format(formatter)).append("\n");
            sb.append("DTSTART:").append(task.getStartTime().format(formatter)).append("\n");
            sb.append("DTEND:").append(task.getEndTime().format(formatter)).append("\n");
            sb.append("SUMMARY:").append(escapeIcs(task.getTitle())).append("\n");
            sb.append("DESCRIPTION:").append(escapeIcs(task.getDescription())).append("\n");
            sb.append("END:VEVENT\n");
        }

        sb.append("END:VCALENDAR\n");
        return sb.toString();
    }

    private String escapeIcs(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n");
    }

    private String buildCustomThemeCss(Color bg, Color accent) {
        Color darker = accent.darker();
        Color lighter = accent.brighter();
        String bgHex = toHex(bg);
        String accentHex = toHex(accent);
        String accentDarkHex = toHex(darker);
        String accentLightHex = toHex(lighter);

        return """
            .root-pane {
                -fx-font-family: \"Avenir Next\", \"Verdana\", sans-serif;
                -fx-background-color: linear-gradient(to bottom, %s, %s);
            }
            .header-box {
                -fx-background-color: rgba(255, 255, 255, 0.55);
                -fx-background-radius: 16;
                -fx-border-color: rgba(255, 255, 255, 0.9);
                -fx-border-radius: 16;
            }
            .app-title { -fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #1f2b3a; }
            .insight-label { -fx-text-fill: #1f2b3a; }
            .field-label { -fx-text-fill: #1f2b3a; -fx-font-weight: 700; }
            .button {
                -fx-background-color: linear-gradient(to bottom, %s, %s);
                -fx-border-color: %s;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-font-weight: 700;
                -fx-text-fill: #10232f;
            }
            .btn-success { -fx-background-color: linear-gradient(to bottom, %s, %s); -fx-text-fill: #0f1a1f; }
            .btn-danger { -fx-background-color: linear-gradient(to bottom, #ff9c8e, #ff7668); -fx-text-fill: white; }
            .btn-accent { -fx-background-color: linear-gradient(to bottom, %s, %s); -fx-text-fill: #0f1a1f; }
            .table-box { -fx-background-color: rgba(255, 255, 255, 0.75); -fx-background-radius: 16; }
            .tab-pane .tab { -fx-background-color: rgba(255, 255, 255, 0.7); -fx-background-radius: 10 10 0 0; -fx-font-weight: 700; }
            .tab-pane .tab:selected { -fx-background-color: %s; }
            .titled-pane > .title { -fx-background-color: rgba(255, 255, 255, 0.75); -fx-background-radius: 10; }
            .titled-pane > *.content { -fx-background-color: rgba(255, 255, 255, 0.55); -fx-background-radius: 10; }
            """.formatted(bgHex, accentLightHex, accentLightHex, accentHex, accentDarkHex,
            accentLightHex, accentHex, accentLightHex, accentHex, accentHex);
    }

    private String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        Stage stage = (Stage) taskTable.getScene().getWindow();
        stage.close();
    }
}
