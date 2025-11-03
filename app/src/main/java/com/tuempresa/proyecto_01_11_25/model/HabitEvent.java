package com.tuempresa.proyecto_01_11_25.model;

public class HabitEvent {
    public enum HabitType { EXERCISE, WALK, DEMO, FOCUS, READ }

    private final double lat;
    private final double lng;
    private final String note;
    private final HabitType type;
    private final long timestamp;

    public HabitEvent(double lat, double lng, String note, HabitType type) {
        this.lat = lat;
        this.lng = lng;
        this.note = note;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getNote() { return note; }
    public HabitType getType() { return type; }
    public long getTimestamp() { return timestamp; }
}
