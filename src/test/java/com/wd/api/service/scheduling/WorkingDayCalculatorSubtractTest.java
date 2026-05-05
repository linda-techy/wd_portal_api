package com.wd.api.service.scheduling;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkingDayCalculatorSubtractTest {

    private static final Set<LocalDate> NO_HOLIDAYS = Set.of();

    @Test
    void subtract_zero_returnsStart() {
        LocalDate d = LocalDate.of(2026, 5, 18); // Monday
        assertThat(WorkingDayCalculator.subtractWorkingDays(d, 0, NO_HOLIDAYS, false)).isEqualTo(d);
    }

    @Test
    void subtract_acrossSunday_skipsIt() {
        // Mon 2026-05-18 minus 2 working days = Fri 2026-05-15 (Sunday 17th skipped, Saturday 16th counts)
        // (Saturdays are working days per WorkingDayCalculator semantics; only Sundays
        // are excluded by default.)
        LocalDate mon = LocalDate.of(2026, 5, 18);
        assertThat(WorkingDayCalculator.subtractWorkingDays(mon, 2, NO_HOLIDAYS, false))
                .isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    void subtract_acrossHoliday_skipsIt() {
        LocalDate aug20 = LocalDate.of(2026, 8, 20); // Thu
        Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 8, 19)); // Wed
        // Thu minus 1 working day skips Wed-holiday → Tue 2026-08-18
        assertThat(WorkingDayCalculator.subtractWorkingDays(aug20, 1, holidays, false))
                .isEqualTo(LocalDate.of(2026, 8, 18));
    }

    @Test
    void subtract_sundayWorking_includesSunday() {
        LocalDate mon = LocalDate.of(2026, 5, 18);
        assertThat(WorkingDayCalculator.subtractWorkingDays(mon, 1, NO_HOLIDAYS, true))
                .isEqualTo(LocalDate.of(2026, 5, 17)); // Sunday counted
    }

    @Test
    void subtract_negative_throws() {
        assertThatThrownBy(() ->
                WorkingDayCalculator.subtractWorkingDays(LocalDate.of(2026, 5, 18), -1, NO_HOLIDAYS, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTrip_addThenSubtract_returnsStart_2024Leap() {
        // Property: addWorkingDays(d, n) then subtractWorkingDays(result, n) == d
        LocalDate d = LocalDate.of(2024, 2, 26); // crosses Feb 29 leap day
        for (int n : new int[]{1, 5, 20, 60}) {
            LocalDate forward = WorkingDayCalculator.addWorkingDays(d, n, NO_HOLIDAYS, false);
            LocalDate back = WorkingDayCalculator.subtractWorkingDays(forward, n, NO_HOLIDAYS, false);
            assertThat(back).as("round-trip n=%d in 2024", n).isEqualTo(d);
        }
    }

    @Test
    void roundTrip_addThenSubtract_returnsStart_2028Leap() {
        LocalDate d = LocalDate.of(2028, 2, 25); // crosses Feb 29 leap day
        for (int n : new int[]{1, 5, 20, 60}) {
            LocalDate forward = WorkingDayCalculator.addWorkingDays(d, n, NO_HOLIDAYS, false);
            LocalDate back = WorkingDayCalculator.subtractWorkingDays(forward, n, NO_HOLIDAYS, false);
            assertThat(back).as("round-trip n=%d in 2028", n).isEqualTo(d);
        }
    }
}
