package org.info.infobaza.constants;

public enum Button {
    ALL("Все"),
    CURRENT("Текущий"),
    HISTORICAL("Исторический");

    private final String value;

    Button(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
