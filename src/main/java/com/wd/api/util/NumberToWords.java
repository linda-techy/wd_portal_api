package com.wd.api.util;

import java.math.BigDecimal;

public class NumberToWords {

    private static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Rupees Only";
        }

        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal("100")).intValue();

        StringBuilder result = new StringBuilder();
        if (rupees > 0) {
            result.append(convertNumber(rupees)).append(" Rupees");
        }

        if (paise > 0) {
            if (result.length() > 0) {
                result.append(" and ");
            }
            result.append(convertNumber(paise)).append(" Paise");
        }

        result.append(" Only");
        return result.toString();
    }

    private static String convertNumber(long n) {
        if (n < 0)
            return "Minus " + convertNumber(-n);
        if (n <= 19)
            return units[(int) n];
        if (n <= 99)
            return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + units[(int) (n % 10)] : "");
        if (n <= 999)
            return units[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " and " + convertNumber(n % 100) : "");
        if (n <= 99999)
            return convertNumber(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + convertNumber(n % 1000) : "");
        if (n <= 9999999)
            return convertNumber(n / 100000) + " Lakh" + (n % 100000 != 0 ? " " + convertNumber(n % 100000) : "");
        return convertNumber(n / 10000000) + " Crore" + (n % 10000000 != 0 ? " " + convertNumber(n % 10000000) : "");
    }
}
