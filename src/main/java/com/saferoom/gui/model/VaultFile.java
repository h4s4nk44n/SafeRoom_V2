package com.saferoom.gui.model;

public class VaultFile {
    private final String name;
    private final String size;
    private final String date;
    private final String type; // Dosya türünü saklamak için eklendi

    public VaultFile(String name, String size, String date, String type) {
        this.name = name;
        this.size = size;
        this.date = date;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }
}
