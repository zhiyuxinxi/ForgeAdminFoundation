package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.task.api.PeriodicTaskEngine.Calendar;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Cycle;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Occurrence;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Calendar arithmetic stays anchored to the first date (Jan 31 -> Feb 28 -> Mar 31). */
final class TaskCalendar {
    private TaskCalendar() { }

    static void validate(Calendar rule) {
        if (rule == null || rule.cycle() == null || rule.firstDate() == null || rule.zone() == null
                || rule.frequency() < 1 || rule.frequency() > 366
                || rule.advanceDays() < 0 || rule.advanceDays() > 366
                || rule.windowDays() < 1 || rule.windowDays() > 366
                || (rule.endCount() != null && (rule.endCount() < 1 || rule.endCount() > 100000))
                || (rule.endDate() != null && rule.endDate().isBefore(rule.firstDate()))
                || (rule.endCount() != null && rule.endDate() != null)
                || (rule.cycle() == Cycle.ONCE && rule.frequency() != 1)) {
            throw TaskMessages.error(400, "period.invalid");
        }
        try {
            ZoneId.of(rule.zone());
        } catch (DateTimeException exception) {
            throw TaskMessages.error(400, "period.invalid");
        }
    }

    static List<Occurrence> preview(Calendar rule, int from, int count) {
        validate(rule);
        if (from < 0 || from > 100000 || count < 1 || count > 60) {
            throw TaskMessages.error(400, "period.invalid");
        }
        List<Occurrence> result = new ArrayList<>();
        for (int index = from; index < from + count; index++) {
            Occurrence occurrence = at(rule, index);
            if (occurrence == null) {
                break;
            }
            result.add(occurrence);
        }
        return List.copyOf(result);
    }

    static Occurrence at(Calendar rule, int index) {
        if ((rule.cycle() == Cycle.ONCE && index > 0)
                || (rule.endCount() != null && index >= rule.endCount())) {
            return null;
        }
        try {
            LocalDate date = date(rule, index);
            if (rule.endDate() != null && date.isAfter(rule.endDate())) {
                return null;
            }
            ZoneId zone = ZoneId.of(rule.zone());
            return new Occurrence(index, date, utc(date.minusDays(rule.advanceDays()), zone),
                    utc(date, zone), utc(date.plusDays(rule.windowDays()), zone));
        } catch (DateTimeException | ArithmeticException exception) {
            throw TaskMessages.error(400, "period.invalid");
        }
    }

    private static LocalDate date(Calendar rule, int index) {
        long distance = Math.multiplyExact((long) index, rule.frequency());
        return switch (rule.cycle()) {
            case ONCE -> rule.firstDate();
            case DAILY, CUSTOM_DAYS -> rule.firstDate().plusDays(distance);
            case WEEKLY -> rule.firstDate().plusWeeks(distance);
            case MONTHLY -> rule.firstDate().plusMonths(distance);
            case QUARTERLY -> rule.firstDate().plusMonths(distance * 3);
            case HALF_YEARLY -> rule.firstDate().plusMonths(distance * 6);
            case YEARLY -> rule.firstDate().plusYears(distance);
        };
    }

    private static LocalDateTime utc(LocalDate date, ZoneId zone) {
        return LocalDateTime.ofInstant(date.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
    }
}
