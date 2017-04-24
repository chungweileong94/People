package com.lcw.people.Helpers;

public enum PermissionRequestCode {
    READ_CONTACTS(0),
    WRITE_CONTACTS(1),
    CALL_PHONE(2),
    SEND_SMS(3),
    ACCESS_FINE_LOCATION(4),
    READ_CALL_LOG(5),
    READ_EXTERNAL_STORAGE(6),
    WRITE_EXTERNAL_STORAGE(7);

    private final int value;

    PermissionRequestCode(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
