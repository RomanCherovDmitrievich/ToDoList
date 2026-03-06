package util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecurrenceUtil {
    private RecurrenceUtil() {
    }

    public static LocalDateTime nextOccurrence(LocalDateTime start, String rule) {
        if (rule == null || rule.isBlank() || rule.equalsIgnoreCase("NONE")) {
            return null;
        }

        String normalized = rule.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DAILY" -> start.plusDays(1);
            case "WEEKLY" -> start.plusWeeks(1);
            case "WEEKDAYS" -> nextWeekday(start);
            case "MONTHLY" -> start.plusMonths(1);
            default -> parseRRule(start, normalized);
        };
    }

    private static LocalDateTime nextWeekday(LocalDateTime start) {
        LocalDateTime candidate = start.plusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private static LocalDateTime parseRRule(LocalDateTime start, String rule) {
        String cleaned = rule.startsWith("RRULE:") ? rule.substring("RRULE:".length()) : rule;
        String[] parts = cleaned.split(";");

        String freq = "";
        int interval = 1;
        List<DayOfWeek> byDays = new ArrayList<>();
        int bySetPos = 0;

        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();

            switch (key) {
                case "FREQ" -> freq = value;
                case "INTERVAL" -> {
                    try {
                        interval = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
                case "BYDAY" -> {
                    String[] days = value.split(",");
                    for (String day : days) {
                        DayOfWeek parsed = parseDay(day);
                        if (parsed != null) {
                            byDays.add(parsed);
                        }
                    }
                }
                case "BYSETPOS" -> {
                    try {
                        bySetPos = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
                default -> {
                }
            }
        }

        return switch (freq) {
            case "DAILY" -> start.plusDays(interval);
            case "WEEKLY" -> nextWeekly(start, interval, byDays);
            case "MONTHLY" -> nextMonthly(start, interval, byDays, bySetPos);
            default -> null;
        };
    }

    private static LocalDateTime nextWeekly(LocalDateTime start, int interval, List<DayOfWeek> byDays) {
        if (byDays.isEmpty()) {
            return start.plusWeeks(interval);
        }

        LocalDateTime candidate = start.plusDays(1);
        while (true) {
            if (byDays.contains(candidate.getDayOfWeek()) && !candidate.isBefore(start.plusDays(1))) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
            if (candidate.isAfter(start.plusWeeks(interval))) {
                return candidate;
            }
        }
    }

    private static LocalDateTime nextMonthly(LocalDateTime start, int interval, List<DayOfWeek> byDays, int bySetPos) {
        LocalDate base = start.toLocalDate().plusMonths(interval);

        if (byDays.isEmpty()) {
            return LocalDateTime.of(base.withDayOfMonth(Math.min(start.getDayOfMonth(), base.lengthOfMonth())), start.toLocalTime());
        }

        DayOfWeek target = byDays.get(0);
        if (bySetPos == 0) {
            bySetPos = 1;
        }

        LocalDate first = base.with(TemporalAdjusters.firstInMonth(target));
        LocalDate selected = first.plusWeeks(bySetPos - 1);
        if (selected.getMonth() != base.getMonth()) {
            selected = base.with(TemporalAdjusters.lastInMonth(target));
        }

        return LocalDateTime.of(selected, start.toLocalTime());
    }

    private static DayOfWeek parseDay(String value) {
        return switch (value) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
