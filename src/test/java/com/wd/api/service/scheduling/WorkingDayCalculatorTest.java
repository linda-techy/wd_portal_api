package com.wd.api.service.scheduling;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkingDayCalculatorTest {

    private static final Set<LocalDate> NO_HOLIDAYS = Set.of();

    // Onam 2026: Aug 26, 27, 28, 29 (Wed–Sat). Source: Kerala State Government
    // gazette holiday list 2026 — confirmed manually against the seed YAML.
    private static final Set<LocalDate> ONAM_2026 = Set.of(
            LocalDate.of(2026, 8, 26),
            LocalDate.of(2026, 8, 27),
            LocalDate.of(2026, 8, 28),
            LocalDate.of(2026, 8, 29));

    @Test
    void addWorkingDays_zeroDays_returnsStartDate() {
        LocalDate start = LocalDate.of(2026, 5, 4); // Mon
        assertThat(WorkingDayCalculator.addWorkingDays(start, 0, NO_HOLIDAYS, false))
                .isEqualTo(start);
    }

    @Test
    void addWorkingDays_skipsSundayWhenSundayWorkingFalse() {
        LocalDate friday = LocalDate.of(2026, 5, 1); // Fri
        // +1 working day → Sat May 2 (Sun May 3 skipped)
        assertThat(WorkingDayCalculator.addWorkingDays(friday, 1, NO_HOLIDAYS, false))
                .isEqualTo(LocalDate.of(2026, 5, 2));
        // +2 working days → Mon May 4
        assertThat(WorkingDayCalculator.addWorkingDays(friday, 2, NO_HOLIDAYS, false))
                .isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void addWorkingDays_doesNotSkipSundayWhenSundayWorkingTrue() {
        LocalDate friday = LocalDate.of(2026, 5, 1);
        // +2 → Sun May 3
        assertThat(WorkingDayCalculator.addWorkingDays(friday, 2, NO_HOLIDAYS, true))
                .isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    void addWorkingDays_skipsHoliday() {
        LocalDate start = LocalDate.of(2026, 5, 4); // Mon
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 5, 5));
        // +1 working day from Mon: Tue is a holiday → Wed May 6
        assertThat(WorkingDayCalculator.addWorkingDays(start, 1, holidays, false))
                .isEqualTo(LocalDate.of(2026, 5, 6));
    }

    @Test
    void addWorkingDays_acrossOnamWeek_skipsAllFourHolidays() {
        // Start Mon Aug 24, 2026. +5 working days.
        // Aug 24 (Mon) +1 = 25 (Tue) +1 = 30 (Sun, skipped...) wait recompute:
        // Mon 24 -> step1 Tue 25 -> step2 Wed 26 = HOLIDAY skip
        //                          step2 Thu 27 = HOLIDAY skip
        //                          step2 Fri 28 = HOLIDAY skip
        //                          step2 Sat 29 = HOLIDAY skip
        //                          step2 Sun 30 = SUNDAY skip
        //                          step2 Mon 31 -> step3 Tue Sep 1 -> step4 Wed Sep 2 -> step5 Thu Sep 3
        LocalDate start = LocalDate.of(2026, 8, 24);
        assertThat(WorkingDayCalculator.addWorkingDays(start, 5, ONAM_2026, false))
                .isEqualTo(LocalDate.of(2026, 9, 3));
    }

    @Test
    void addWorkingDays_negativeDays_throws() {
        assertThatThrownBy(() -> WorkingDayCalculator.addWorkingDays(
                LocalDate.of(2026, 5, 4), -1, NO_HOLIDAYS, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addWorkingDays_acrossLeapDay_2028FebToMarch() {
        // 2028 is a leap year. Feb 28 2028 = Mon. +3 working days.
        // Mon 28 -> step1 Tue 29 (leap) -> step2 Wed Mar 1 -> step3 Thu Mar 2.
        LocalDate start = LocalDate.of(2028, 2, 28);
        assertThat(WorkingDayCalculator.addWorkingDays(start, 3, NO_HOLIDAYS, false))
                .isEqualTo(LocalDate.of(2028, 3, 2));
    }

    @Test
    void workingDaysBetween_sameDay_returnsZero() {
        LocalDate d = LocalDate.of(2026, 5, 4);
        assertThat(WorkingDayCalculator.workingDaysBetween(d, d, NO_HOLIDAYS, false))
                .isEqualTo(0);
    }

    @Test
    void workingDaysBetween_monToFri_returnsFour() {
        // Mon May 4 .. Fri May 8 → 4 increments (Mon-Tue, Tue-Wed, Wed-Thu, Thu-Fri)
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 8),
                NO_HOLIDAYS, false)).isEqualTo(4);
    }

    @Test
    void workingDaysBetween_acrossSunday_skipsIt() {
        // Fri May 1 .. Mon May 4 → Fri-Sat (1), Sat-Sun(skip), Sun-Mon(skip) → 1
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                NO_HOLIDAYS, false)).isEqualTo(2);
    }

    @Test
    void workingDaysBetween_overMonthBoundary() {
        // Mon Apr 27 .. Mon May 11 (two weeks). Sundays excluded.
        // Working days: Apr 27,28,29,30, May 1,2, May 4,5,6,7,8,9, May 11 (Sun May 10 excluded)
        // Days from start to end excluding Sundays = 12 increments.
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 4, 27),
                LocalDate.of(2026, 5, 11),
                NO_HOLIDAYS, false)).isEqualTo(12);
    }

    @Test
    void workingDaysBetween_endBeforeStart_throws() {
        assertThatThrownBy(() -> WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 5),
                LocalDate.of(2026, 5, 4),
                NO_HOLIDAYS, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void workingDaysBetween_sundayWorkingTrue_includesSundays() {
        // Fri May 1 .. Mon May 4 → 3 calendar increments, Sundays count → 3
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4),
                NO_HOLIDAYS, true)).isEqualTo(3);
    }
}
