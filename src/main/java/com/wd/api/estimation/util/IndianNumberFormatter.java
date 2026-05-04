package com.wd.api.estimation.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats numbers in the Indian numbering system (lakhs/crores), where the
 * thousands separator appears after the first 3 digits from the right and then
 * every 2 digits thereafter. e.g. 6902624.76 → "69,02,624.76".
 *
 * Uses the en-IN locale's decimal grouping, which already follows this rule.
 */
public final class IndianNumberFormatter {

    private static final DecimalFormat WITH_PAISA;
    private static final DecimalFormat WHOLE_RUPEE;

    static {
        Locale enIN = Locale.forLanguageTag("en-IN");
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(enIN);
        WITH_PAISA = new DecimalFormat("#,##,##0.00", sym);
        WHOLE_RUPEE = new DecimalFormat("#,##,##0", sym);
    }

    private IndianNumberFormatter() {}

    /** "6902624.76" → "69,02,624.76". Returns "0.00" for null. */
    public static String formatWithPaisa(BigDecimal n) {
        if (n == null) return "0.00";
        return WITH_PAISA.format(n);
    }

    /** "6902624.76" → "69,02,625" (rounded). Returns "0" for null. */
    public static String formatWholeRupee(BigDecimal n) {
        if (n == null) return "0";
        return WHOLE_RUPEE.format(n);
    }
}
