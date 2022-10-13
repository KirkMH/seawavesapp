package com.asu.seawavesapp.util;

public class DecimalFormatter {
    private int places;

    public DecimalFormatter() {
        places = 0;
    }

    public DecimalFormatter(int places) {
        this.places = places;
    }

    public String format(Float number) {
        return format(number, this.places);
    }

    public String format(Float number, int places) {
        String pattern = "%." + places + "f";
        if (isZero(number, pattern)) number = 0f;
        return String.format(pattern, number);
    }

    private boolean isZero(Float number, String pattern) {
        float value = Float.parseFloat(String.format(pattern, number));
        return  (value == 0f);
    }
}
