package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.task.api.PeriodicTaskEngine.Calendar;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Cycle;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskCalendarTest {
    @Test
    void calendarMonthsAndLeapYearsStayAnchoredAndEndDateIsInclusive() {
        var rule = new Calendar(Cycle.MONTHLY, 1, LocalDate.of(2024, 1, 31), "Asia/Shanghai", 3, 7,
                null, LocalDate.of(2024, 3, 31));
        var dates = TaskCalendar.preview(rule, 0, 60);
        assertThat(dates).extracting(value -> value.startDate().toString())
                .containsExactly("2024-01-31", "2024-02-29", "2024-03-31");
        assertThat(dates.get(0).generateAtUtc().toString()).isEqualTo("2024-01-27T16:00");
        var yearly = new Calendar(Cycle.YEARLY, 1, LocalDate.of(2024, 2, 29), "UTC", 0, 1, 5, null);
        assertThat(TaskCalendar.preview(yearly, 4, 1).get(0).startDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void dateWindowsRespectDstAndAllCyclesUseTheDeclaredFrequency() {
        var dst = new Calendar(Cycle.DAILY, 1, LocalDate.of(2026, 3, 8), "America/New_York", 0, 1, null, null);
        var day = TaskCalendar.preview(dst, 0, 1).get(0);
        assertThat(Duration.between(day.startsAtUtc(), day.dueAtUtc()).toHours()).isEqualTo(23);
        int[] days = { 0, 2, 14, 59, 181, 365, 730, 2 };
        for (Cycle cycle : Cycle.values()) {
            var rule = new Calendar(cycle, cycle == Cycle.ONCE ? 1 : 2,
                    LocalDate.of(2025, 1, 1), "UTC", 0, 1, 2, null);
            var dates = TaskCalendar.preview(rule, 0, 3);
            assertThat(dates).hasSize(cycle == Cycle.ONCE ? 1 : 2);
            if (cycle != Cycle.ONCE) {
                assertThat(dates.get(1).startDate()).isEqualTo(rule.firstDate().plusDays(days[cycle.ordinal()]));
            }
        }
    }

    @Test
    void rejectsUnboundedPreviewInvalidZonesAndAmbiguousEndRules() {
        var rule = new Calendar(Cycle.WEEKLY, 1, LocalDate.of(2026, 1, 1), "UTC", 0, 7, null, null);
        assertThatThrownBy(() -> TaskCalendar.preview(rule, 0, 61)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> TaskCalendar.preview(rule, -1, 1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> TaskCalendar.preview(new Calendar(Cycle.DAILY, 0,
                rule.firstDate(), "bad-zone", 0, 7, null, null), 0, 1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> TaskCalendar.preview(new Calendar(Cycle.DAILY, 1,
                rule.firstDate(), "UTC", 0, 7, 2, rule.firstDate()), 0, 1)).isInstanceOf(BusinessException.class);
    }
}
