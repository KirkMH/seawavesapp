package com.asu.seawavesapp.util;

/**
 * DecimalFormatter is a utility class for formatting floats.
 */
public class DecimalFormatter {
    private final int places;

    /**
     * Creates an instance of DecimalFormatter class to have 0 decimal place.
     */
    public DecimalFormatter() {
        places = 0;
    }

    /**
     * Creates an instance of DecimalFormatter class to have
     * <code>places</code> decimal place(s).
     *
     * @param places - number of decimal places
     */
    public DecimalFormatter(int places) {
        this.places = places;
    }

    /**
     * Formats the <code>number</code> to a given decimal places.
     *
     * @param number - number to format
     * @return formatted number
     */
    public String format(Float number) {
        return format(number, this.places);
    }

    /**
     * Format the <code>number</code> to <code>places</code> decimal place(s).
     *
     * @param number - number to format
     * @param places - number of decimal places
     * @return formatted number
     */
    public String format(Float number, int places) {
        String pattern = "%." + places + "f";
        if (isZero(number, pattern)) number = 0f;
        return String.format(pattern, number);
    }

    /**
     * Checks whether the <code>number</code> is zero or not.
     *
     * @param number  - number to check
     * @param pattern - pattern to use
     * @return <code>true</code> when the number is zero; <code>false</code> otherwise
     */
    private boolean isZero(Float number, String pattern) {
        float value = Float.parseFloat(String.format(pattern, number));
        return (value == 0f);
    }
}
