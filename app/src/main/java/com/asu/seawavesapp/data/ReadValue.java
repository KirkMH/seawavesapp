package com.asu.seawavesapp.data;

public class ReadValue {
    private Integer number;
    private String name;
    private String value;

    public ReadValue(Integer number, String name, String value) {
        this.number = number;
        this.name = name;
        this.value = value;
    }

    public Integer getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }
}
