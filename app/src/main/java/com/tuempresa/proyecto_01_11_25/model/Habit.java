package com.tuempresa.proyecto_01_11_25.model;

import java.util.ArrayList;
import java.util.List;

public class Habit {

    public enum HabitType {
        EXERCISE,   // acelerómetro
        WALK,       // distancia
        DEMO,       // botón
        READ        // cámara + ML Kit
    }

    private String title;
    private String goal;
    private String category;
    private HabitType type;
    private boolean completed;

    public Habit(String title, String goal, String category, HabitType type) {
        this.title = title;
        this.goal = goal;
        this.category = category;
        this.type = type;
        this.completed = false;
    }

    public String getTitle() { return title; }
    public String getGoal() { return goal; }
    public String getCategory() { return category; }
    public HabitType getType() { return type; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    /** 4 hábitos por defecto al abrir la app */
    public static List<Habit> defaultHabits() {
        List<Habit> list = new ArrayList<>();
        list.add(new Habit("Ejercicio", "Goal: movimiento detectado", "salud", HabitType.EXERCISE));
        list.add(new Habit("Caminar", "Goal: 150 metros", "salud", HabitType.WALK));
        list.add(new Habit("Leer", "Goal: detectar página de libro", "educación", HabitType.READ));
        list.add(new Habit("Demo", "Goal: tocar para completar", "general", HabitType.DEMO));
        return list;
    }
}
