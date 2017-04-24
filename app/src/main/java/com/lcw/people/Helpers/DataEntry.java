package com.lcw.people.Helpers;

public class DataEntry {
    private String value;
    private int type;

    public DataEntry() {
        value = "";
        type = 0;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
