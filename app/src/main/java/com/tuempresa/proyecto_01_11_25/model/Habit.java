package com.tuempresa.proyecto_01_11_25.model;

public class Habit {
    private String name;
    private String goal;
    private String period;
    private String type;
    private boolean done;

    public Habit(String name, String goal, String period, String type, boolean done) {
        this.name = name;
        this.goal = goal;
        this.period = period;
        this.type = type;
        this.done = done;
    }

    public String getName() { return name; }
    public String getGoal() { return goal; }
    public String getPeriod() { return period; }
    public String getType() { return type; }
    public boolean isDone() { return done; }

    public void setDone(boolean done) { this.done = done; }
}
