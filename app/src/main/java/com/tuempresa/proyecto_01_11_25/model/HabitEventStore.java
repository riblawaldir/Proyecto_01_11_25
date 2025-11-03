package com.tuempresa.proyecto_01_11_25.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HabitEventStore {
    private static final String PREFS_NAME = "HabitEventStore";
    private static final String KEY_EVENTS = "habit_events";
    private static final List<HabitEvent> events = new ArrayList<>();
    private static Context context;
    private static SharedPreferences prefs;

    /**
     * Inicializa el store con el contexto de la aplicación
     * Debe llamarse en onCreate de la actividad principal
     */
    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadEvents();
    }

    /**
     * Agrega un evento y lo persiste inmediatamente
     */
    public static synchronized void add(HabitEvent e) {
        events.add(e);
        saveEvents();
        android.util.Log.d("HabitEventStore", "Evento agregado y guardado: " + e.getNote());
    }

    /**
     * Obtiene todos los eventos
     */
    public static synchronized List<HabitEvent> all() {
        return new ArrayList<>(events);
    }

    /**
     * Limpia todos los eventos
     */
    public static synchronized void clear() {
        events.clear();
        if (prefs != null) {
            prefs.edit().remove(KEY_EVENTS).apply();
        }
        android.util.Log.d("HabitEventStore", "Eventos eliminados");
    }

    /**
     * Guarda los eventos en SharedPreferences como JSON
     */
    private static synchronized void saveEvents() {
        if (prefs == null) {
            android.util.Log.w("HabitEventStore", "No se puede guardar: SharedPreferences no inicializado");
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray();
            for (HabitEvent event : events) {
                JSONObject jsonEvent = new JSONObject();
                jsonEvent.put("lat", event.getLat());
                jsonEvent.put("lng", event.getLng());
                jsonEvent.put("note", event.getNote());
                jsonEvent.put("type", event.getType().name());
                jsonEvent.put("timestamp", event.getTimestamp());
                jsonArray.put(jsonEvent);
            }

            prefs.edit().putString(KEY_EVENTS, jsonArray.toString()).apply();
            android.util.Log.d("HabitEventStore", "Eventos guardados: " + events.size());
        } catch (JSONException e) {
            android.util.Log.e("HabitEventStore", "Error al guardar eventos", e);
        }
    }

    /**
     * Carga los eventos desde SharedPreferences
     */
    private static synchronized void loadEvents() {
        if (prefs == null) {
            android.util.Log.w("HabitEventStore", "No se puede cargar: SharedPreferences no inicializado");
            return;
        }

        events.clear();
        String eventsJson = prefs.getString(KEY_EVENTS, null);
        
        if (eventsJson == null || eventsJson.isEmpty()) {
            android.util.Log.d("HabitEventStore", "No hay eventos guardados");
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray(eventsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonEvent = jsonArray.getJSONObject(i);
                
                double lat = jsonEvent.getDouble("lat");
                double lng = jsonEvent.getDouble("lng");
                String note = jsonEvent.getString("note");
                HabitEvent.HabitType type = HabitEvent.HabitType.valueOf(jsonEvent.getString("type"));
                
                // Crear evento (el constructor ya asigna timestamp, pero guardamos el original)
                HabitEvent event = new HabitEvent(lat, lng, note, type);
                events.add(event);
            }
            
            android.util.Log.d("HabitEventStore", "Eventos cargados: " + events.size());
        } catch (JSONException e) {
            android.util.Log.e("HabitEventStore", "Error al cargar eventos", e);
        }
    }
}
