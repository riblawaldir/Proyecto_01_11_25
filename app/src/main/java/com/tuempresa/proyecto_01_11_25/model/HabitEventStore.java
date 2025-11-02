package com.tuempresa.proyecto_01_11_25.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HabitEventStore {
    private static final List<HabitEvent> events = new ArrayList<>();

    public static synchronized void add(HabitEvent e) { events.add(e); }

    public static synchronized List<HabitEvent> all() {
        return new ArrayList<>(events);
    }

    public static synchronized void clear() { events.clear(); }
}
