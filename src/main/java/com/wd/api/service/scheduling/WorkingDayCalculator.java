package com.wd.api.service.scheduling;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Pure-Java working-day utility. No Spring, no DB, no static state.
 *
 * <p>A "working day" is any calendar day that is NOT in the supplied
 * {@code holidays} set, AND NOT a Sunday — unless {@code sundayWorking}
 * is true, in which case Sundays count.
 */
public final class WorkingDayCalculator {

    private WorkingDayCalculator() { /* static utility */ }

    /**
     * Returns {@code start} plus {@code days} working days (forward).
     *
     * @param start          first day; counts as day 0 (returned when {@code days == 0})
     * @param days           non-negative count of working days to advance
     * @param holidays       set of dates to skip
     * @param sundayWorking  if true, Sundays are working days
     * @return the date that is {@code days} working days after {@code start}
     * @throws IllegalArgumentException if {@code days < 0}
     */
    public static LocalDate addWorkingDays(
            LocalDate start, int days, Set<LocalDate> holidays, boolean sundayWorking) {
        if (days < 0) {
            throw new IllegalArgumentException("days must be >= 0, got " + days);
        }
        LocalDate cursor = start;
        int advanced = 0;
        while (advanced < days) {
            cursor = cursor.plusDays(1);
            if (isWorkingDay(cursor, holidays, sundayWorking)) {
                advanced++;
            }
        }
        return cursor;
    }

    /**
     * Returns the count of working-day increments between {@code start} (inclusive)
     * and {@code end} (inclusive). For two consecutive working days the result is 1.
     *
     * @throws IllegalArgumentException if {@code end} is before {@code start}
     */
    public static int workingDaysBetween(
            LocalDate start, LocalDate end, Set<LocalDate> holidays, boolean sundayWorking) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end (" + end + ") is before start (" + start + ")");
        }
        int count = 0;
        LocalDate cursor = start;
        while (cursor.isBefore(end)) {
            cursor = cursor.plusDays(1);
            if (isWorkingDay(cursor, holidays, sundayWorking)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isWorkingDay(LocalDate d, Set<LocalDate> holidays, boolean sundayWorking) {
        if (!sundayWorking && d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidays.contains(d);
    }
}
