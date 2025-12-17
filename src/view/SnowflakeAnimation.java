package view;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @class SnowflakeAnimation
 * @brief Класс для создания и управления анимацией падающих снежинок
 * 
 * @details Обеспечивает визуальный эффект падающего снега в фоне приложения.
 *          Создает множество снежинок с различными параметрами (размер, скорость,
 *          прозрачность) и анимирует их движение по экрану.
 * 
 * @author Чернов
 * @version 1.0
 * @date 2025-11-4
 * 
 * @note Используется в MainController для создания новогодней атмосферы
 * @warning Для корректной работы требуется JavaFX AnimationTimer
 * 
 * @see MainController
 * @see javafx.animation.AnimationTimer
 * 
 */
public class SnowflakeAnimation {
    /** @brief Панель, на которой отображаются снежинки */
    private Pane snowPane;
    
    /** @brief Список всех снежинок в анимации */
    private List<Snowflake> snowflakes;
    
    /** @brief Генератор случайных чисел для параметров снежинок */
    private Random random;
    
    /** @brief Таймер анимации, управляющий движением снежинок */
    private AnimationTimer animationTimer;
    
    /**
     * @class Snowflake
     * @brief Внутренний класс для хранения данных одной снежинки
     * 
     * @details Содержит графический элемент снежинки и её физические параметры
     *          для анимации (скорость падения, амплитуда и скорость покачивания).
     * 
     * @note Используется только внутри SnowflakeAnimation
     * @private
     */
    private class Snowflake {
        /** @brief Графический элемент снежинки (круг) */
        Circle circle;
        
        /** @brief Скорость падения снежинки в пикселях в секунду */
        double speed;
        
        /** @brief Начальное смещение для анимации покачивания */
        double sway;
        
        /** @brief Скорость покачивания снежинки из стороны в сторону */
        double swaySpeed;
        
        /**
         * @brief Конструктор снежинки
         * @param circle Графический элемент снежинки
         * @param speed Скорость падения (пикселей/секунду)
         * @param sway Начальное смещение для покачивания
         * @param swaySpeed Скорость покачивания
         */
        Snowflake(Circle circle, double speed, double sway, double swaySpeed) {
            this.circle = circle;
            this.speed = speed;
            this.sway = sway;
            this.swaySpeed = swaySpeed;
        }
    }
    
    /**
     * @brief Конструктор класса SnowflakeAnimation
     * @param pane Панель, на которой будет отображаться анимация снежинок
     * 
     * @details Инициализирует все необходимые компоненты для анимации.
     *          Создает пустой список снежинок и инициализирует генератор случайных чисел.
     * 
     * @param pane Панель JavaFX для отображения снежинок
     * @throws NullPointerException если pane равен null
     * 
     * @note Панель должна быть добавлена в сцену перед запуском анимации
     */
    public SnowflakeAnimation(Pane pane) {
        this.snowPane = pane;
        this.snowflakes = new ArrayList<>();
        this.random = new Random();
    }
    
    /**
     * @brief Запускает анимацию снежинок
     * @details Создает заданное количество снежинок и запускает таймер анимации.
     *          Количество снежинок выбирается случайно в диапазоне 80-100.
     * 
     * @post Созданы снежинки и запущен таймер анимации
     * @note Метод можно вызывать повторно после stop() для перезапуска анимации
     * 
     * @see #stop()
     * @see #createSnowflake()
     * @see AnimationTimer
     */
    public void start() {
        // Создаем случайное количество снежинок (80-100)
        int snowflakeCount = 80 + random.nextInt(20);
        
        // Создаем каждую снежинку и добавляем на панель
        for (int i = 0; i < snowflakeCount; i++) {
            Snowflake snowflake = createSnowflake();
            snowflakes.add(snowflake);
            snowPane.getChildren().add(snowflake.circle);
        }
        
        // Создаем и запускаем таймер анимации
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            
            /**
             * @brief Обработчик кадра анимации
             * @param now Текущее время в наносекундах
             * 
             * @details Вызывается каждый кадр анимации, обновляет позиции
             *          всех снежинок на основе прошедшего времени.
             * 
             * @param now Текущее время System.nanoTime()
             */
            @Override
            public void handle(long now) {
                // Первый вызов - инициализируем время
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                // Вычисляем время, прошедшее с предыдущего кадра
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                
                // Обновляем позиции всех снежинок
                updateSnowflakes(elapsedSeconds);
                
                // Сохраняем время текущего кадра
                lastUpdate = now;
            }
        };
        
        // Запускаем анимацию
        animationTimer.start();
    }
    
    /**
     * @brief Останавливает анимацию снежинок
     * @details Останавливает таймер анимации и удаляет все снежинки с панели.
     *          Освобождает ресурсы, связанные с анимацией.
     * 
     * @post Таймер анимации остановлен, список снежинок очищен
     * @note После остановки анимацию можно перезапустить с помощью start()
     * 
     * @see #start()
     */
    public void stop() {
        // Останавливаем таймер анимации если он запущен
        if (animationTimer != null) {
            animationTimer.stop();
        }
        
        // Очищаем список снежинок
        snowflakes.clear();
        
        // Удаляем все круги (снежинки) с панели
        snowPane.getChildren().removeIf(node -> node instanceof Circle);
    }
    
    /**
     * @brief Создает одну снежинку со случайными параметрами
     * @return Объект Snowflake с заданными параметрами
     * 
     * @details Создает графический элемент снежинки (круг) со случайными:
     *          - Размером (1-3 пикселя)
     *          - Начальной позицией (вверху экрана)
     *          - Прозрачностью (30-100%)
     *          - Цветом (белый, AliceBlue или LightBlue)
     *          - Скоростью падения (30-100 пикселей/секунду)
     *          - Параметрами покачивания
     * 
     * @note Снежинка создается выше видимой области экрана для плавного появления
     * 
     * @see javafx.scene.shape.Circle
     * @see javafx.scene.effect.Glow
     * @private
     */
    private Snowflake createSnowflake() {
        Circle circle = new Circle();
        
        // ОЧЕНЬ маленькие снежинки (1-3 пикселя)
        double size = 1 + random.nextDouble() * 2;
        circle.setRadius(size);
        
        // Случайная начальная позиция (вверху экрана)
        // Начинаем выше экрана для плавного появления
        double x = random.nextDouble() * snowPane.getWidth();
        double y = -size - random.nextDouble() * 100;
        
        circle.setTranslateX(x);
        circle.setTranslateY(y);
        
        // Разная прозрачность (от легкой до более заметной)
        double opacity = 0.3 + random.nextDouble() * 0.7;
        circle.setOpacity(opacity);
        
        // Разные оттенки белого/голубого для разнообразия
        int colorChoice = random.nextInt(3);
        switch (colorChoice) {
            case 0:
                circle.setFill(javafx.scene.paint.Color.WHITE); // Чистый белый
                break;
            case 1:
                // AliceBlue - очень светлый голубой (#F0F8FF)
                circle.setFill(javafx.scene.paint.Color.rgb(240, 248, 255));
                break;
            case 2:
                // LightBlue - светлый голубой (#ADD8E6)
                circle.setFill(javafx.scene.paint.Color.rgb(173, 216, 230));
                break;
        }
        
        // Добавляем легкое свечение для эффекта "сияния"
        circle.setEffect(new javafx.scene.effect.Glow(0.3));
        
        // Разная скорость падения (от медленной до быстрой)
        double speed = 30 + random.nextDouble() * 70; // пикселей в секунду
        
        // Параметры для покачивания из стороны в сторону
        double sway = random.nextDouble() * 2 - 1; // начальное смещение в диапазоне [-1, 1]
        double swaySpeed = 0.5 + random.nextDouble() * 2; // скорость покачивания
        
        return new Snowflake(circle, speed, sway, swaySpeed);
    }
    
    /**
     * @brief Обновляет позиции всех снежинок
     * @param deltaTime Время, прошедшее с предыдущего обновления (в секундах)
     * 
     * @details Для каждой снежинки:
     *          1. Двигает её вниз с заданной скоростью
     *          2. Применяет покачивание из стороны в сторону
     *          3. Проверяет выход за границы и перераспределяет снежинку
     *          4. Обновляет прозрачность для эффекта мерцания
     * 
     * @param deltaTime Дельта времени между кадрами анимации
     * 
     * @note Если снежинка упала за нижнюю границу, она перемещается наверх
     * @note Если снежинка вышла за боковые границы, она появляется с противоположной стороны
     * 
     * @see #createSnowflake()
     * @private
     */
    private void updateSnowflakes(double deltaTime) {
        double paneHeight = snowPane.getHeight();
        double paneWidth = snowPane.getWidth();
        long time = System.currentTimeMillis(); // Текущее время для анимации
        
        for (Snowflake snowflake : snowflakes) {
            Circle circle = snowflake.circle;
            
            // Двигаем снежинку вниз с её скоростью
            double newY = circle.getTranslateY() + snowflake.speed * deltaTime;
            
            // Покачивание из стороны в сторону по синусоидальному закону
            double currentX = circle.getTranslateX();
            double swayOffset = Math.sin(time * 0.001 * snowflake.swaySpeed + snowflake.sway) * 15;
            double newX = currentX + swayOffset * deltaTime;
            
            // Если снежинка упала за нижнюю границу, перемещаем наверх
            if (newY > paneHeight + circle.getRadius()) {
                newY = -circle.getRadius(); // Начинаем выше экрана
                newX = random.nextDouble() * paneWidth; // Случайная позиция по X
                
                // Немного меняем параметры при перерождении для разнообразия
                snowflake.speed = 30 + random.nextDouble() * 70;
                snowflake.swaySpeed = 0.5 + random.nextDouble() * 2;
            }
            
            // Если снежинка вышла за боковые границы, возвращаем её
            if (newX < -circle.getRadius()) {
                newX = paneWidth + circle.getRadius(); // Появляется справа
            } else if (newX > paneWidth + circle.getRadius()) {
                newX = -circle.getRadius(); // Появляется слева
            }
            
            // Применяем новые координаты
            circle.setTranslateX(newX);
            circle.setTranslateY(newY);
            
            // Легкое мерцание для реалистичности
            double flicker = 0.7 + 0.3 * Math.sin(time * 0.003 + snowflake.sway);
            circle.setOpacity(flicker * (0.3 + random.nextDouble() * 0.4));
        }
    }
    
    /**
     * @brief Изменяет количество снежинок в анимации
     * @param count Новое количество снежинок
     * 
     * @details Останавливает текущую анимацию и запускает новую
     *          с заданным количеством снежинок.
     * 
     * @param count Количество снежинок (должно быть положительным)
     * 
     * @note Если count <= 0, будет использовано значение по умолчанию
     * @warning Метод перезапускает всю анимацию, что может вызвать кратковременную задержку
     * 
     * @see #stop()
     * @see #start()
     */
    public void setSnowflakeCount(int count) {
        stop();
        start();
    }
}