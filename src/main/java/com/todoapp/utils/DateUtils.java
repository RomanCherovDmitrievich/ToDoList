package com.todoapp.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATETIME_FORMATTER);
    }

    public static LocalDate getStartOfWeek(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    public static LocalDate getEndOfWeek(LocalDate date) {
        return date.plusDays(7 - date.getDayOfWeek().getValue());
    }

    public static LocalDate getStartOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    public static LocalDate getEndOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null || date2 == null) return false;
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    public static boolean isThisWeek(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        LocalDate startOfWeek = getStartOfWeek(LocalDate.now());
        LocalDate endOfWeek = getEndOfWeek(LocalDate.now());
        LocalDate taskDate = dateTime.toLocalDate();
        return !taskDate.isBefore(startOfWeek) && !taskDate.isAfter(endOfWeek);
    }

    public static String getRelativeTimeString(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        
        if (dateTime.isBefore(now)) {
            long days = now.toLocalDate().toEpochDay() - dateTime.toLocalDate().toEpochDay();
            if (days == 0) return "сегодня";
            if (days == 1) return "вчера";
            if (days < 7) return days + " дня назад";
            if (days < 30) return (days / 7) + " недели назад";
            return (days / 30) + " месяца назад";
        } else {
            long days = dateTime.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay();
            if (days == 0) return "сегодня";
            if (days == 1) return "завтра";
            if (days < 7) return "через " + days + " дня";
            if (days < 30) return "через " + (days / 7) + " недели";
            return "через " + (days / 30) + " месяца";
        }
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; // 6 = суббота, 7 = воскресенье
    }
}