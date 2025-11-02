package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.Habit;
import com.tuempresa.proyecto_01_11_25.model.HabitEvent;
import com.tuempresa.proyecto_01_11_25.model.HabitEventStore;
import com.tuempresa.proyecto_01_11_25.sensors.AccelerometerSensorManager;
import com.tuempresa.proyecto_01_11_25.sensors.GyroSensorManager;
import com.tuempresa.proyecto_01_11_25.sensors.LightSensorManager;
import com.tuempresa.proyecto_01_11_25.sensors.StepSensorManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "DashboardPrefs";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_FOCUS_MODE = "focus_mode";
    private static final String KEY_HABITS_STATE = "habits_completed_state";
    private static final long SENSOR_DELAY_MS = 3000; // 3 segundos antes de activar sensores
    private static final long LIGHT_DEBOUNCE_MS = 2500; // 2.5 segundos debounce

    private RecyclerView rv;
    private FloatingActionButton btnMap;
    private HabitAdapter adapter;

    private LightSensorManager lightSensor;
    private StepSensorManager walkSensor;
    private GyroSensorManager gyroSensor;
    private AccelerometerSensorManager accelerometerSensor;

    private List<Habit> habits;
    private FusedLocationProviderClient fused;
    private Handler mainHandler;
    private SharedPreferences prefs;

    private boolean focusMode = false;
    private boolean isNight = false;
    private boolean isRecreating = false; // Flag para evitar múltiples recreaciones
    private long lastLightChange = 0;
    private long activityCreateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Resetear flag de recreación
        isRecreating = false;
        
        // Guardar tiempo de creación para delay inicial
        activityCreateTime = System.currentTimeMillis();
        
        // Cargar estado persistente
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        focusMode = prefs.getBoolean(KEY_FOCUS_MODE, false);
        isNight = prefs.getBoolean(KEY_NIGHT_MODE, false);
        
        android.util.Log.d("Dashboard", "onCreate - focusMode: " + focusMode + ", isNight: " + isNight);
        
        // Aplicar tema ANTES de setContentView (crítico)
        applyTheme();
        
        setContentView(R.layout.activity_dashboard);
        
        mainHandler = new Handler(Looper.getMainLooper());
        fused = LocationServices.getFusedLocationProviderClient(this);

        // 🔥 Cargar hábitos (predeterminados o con estados guardados)
        habits = loadHabitsWithState();

        rv = findViewById(R.id.rvHabits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(habits, this::completeDemoHabit);
        rv.setAdapter(adapter);

        btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        
        // Botón temporal para resetear estado (solo para debugging - remover en producción)
        FloatingActionButton fabAddHabit = findViewById(R.id.fabAddHabit);
        if (fabAddHabit != null) {
            fabAddHabit.setOnClickListener(v -> {
                // Resetear estados para testing
                prefs.edit().clear().apply();
                Toast.makeText(this, "Estados reseteados", Toast.LENGTH_SHORT).show();
                recreate();
            });
        }

        // 🚶 Sensor de caminar - iniciar inmediatamente (no afecta UI)
        walkSensor = new StepSensorManager(this, () -> {
            // Callback cuando se completa la caminata
            completeHabitByType(Habit.HabitType.WALK);
        });
        walkSensor.start();

        // 🔦 Sensor de luz — modo nocturno dinámico
        lightSensor = new LightSensorManager(this, new LightSensorManager.OnLowLightListener() {
            @Override
            public void onLowLight() {
                handleLightChange(true);
            }

            @Override
            public void onNormalLight() {
                handleLightChange(false);
            }
        });
        
        // 🏋️ Sensor de acelerómetro — detección de ejercicio/movimiento
        accelerometerSensor = new AccelerometerSensorManager(this, () -> {
            // Callback cuando se detecta ejercicio (movimiento continuo)
            completeHabitByType(Habit.HabitType.EXERCISE);
        });
        
        // 🧘 Sensor de giros — modo foco azul
        gyroSensor = new GyroSensorManager(this, this::activateFocusMode);
        
        // Delay inicial antes de activar sensores que pueden cambiar el tema
        mainHandler.postDelayed(() -> {
            if (!isRecreating) {
                android.util.Log.d("Dashboard", "Activando sensores después de delay");
                lightSensor.start();
                accelerometerSensor.start();
                gyroSensor.start();
                Toast.makeText(this, "Sensores activados", Toast.LENGTH_SHORT).show();
            }
        }, SENSOR_DELAY_MS);
    }

    /**
     * Aplicar tema según el estado actual
     */
    private void applyTheme() {
        if (focusMode) {
            // Tema FOCUS tiene prioridad - sobrescribe modo nocturno
            setTheme(R.style.Theme_Proyecto_01_11_25_Focus);
            android.util.Log.d("Dashboard", "✅ Aplicando tema FOCUS (azul)");
        } else {
            // Aplicar modo nocturno/claro según sensor de luz
            int nightMode = isNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nightMode);
            android.util.Log.d("Dashboard", "✅ Aplicando modo nocturno: " + (isNight ? "YES (oscuro)" : "NO (claro)"));
        }
        
        // Forzar recreación del ActionBar si existe
        if (getSupportActionBar() != null) {
            getSupportActionBar().invalidateOptionsMenu();
        }
    }

    /**
     * Maneja cambios de luz con debounce y validación
     */
    private void handleLightChange(boolean isLowLight) {
        android.util.Log.d("Dashboard", "handleLightChange: isLowLight=" + isLowLight + ", isNight=" + isNight);
        
        // Evitar cambios durante recreación o poco tiempo después de crear
        if (isRecreating || (System.currentTimeMillis() - activityCreateTime) < SENSOR_DELAY_MS) {
            android.util.Log.d("Dashboard", "Ignorando cambio: isRecreating=" + isRecreating);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastLightChange < LIGHT_DEBOUNCE_MS) {
            android.util.Log.d("Dashboard", "Ignorando cambio: debounce activo");
            return;
        }

        // Solo cambiar si realmente cambió
        if ((isLowLight && !isNight) || (!isLowLight && isNight)) {
            lastLightChange = now;
            android.util.Log.d("Dashboard", "Cambiando modo nocturno: " + isLowLight);
            changeNightMode(isLowLight);
        } else {
            android.util.Log.d("Dashboard", "No hay cambio real de estado");
        }
    }

    /**
     * Cambia el modo nocturno sin causar loops
     * NOTA: El sensor de luz puede sobrescribir el modo foco si detecta cambios significativos
     */
    private void changeNightMode(boolean enableNight) {
        if (isRecreating) {
            android.util.Log.d("Dashboard", "No se puede cambiar: isRecreating=" + isRecreating);
            return;
        }
        
        // Si está en modo foco, salir de él primero para permitir cambio de tema
        if (focusMode) {
            android.util.Log.d("Dashboard", "Saliendo del modo foco para cambiar tema");
            focusMode = false;
            prefs.edit().putBoolean(KEY_FOCUS_MODE, false).apply();
        }
        
        android.util.Log.d("Dashboard", "Cambiando a modo nocturno: " + enableNight);
        isNight = enableNight;
        prefs.edit().putBoolean(KEY_NIGHT_MODE, isNight).apply();
        
        AppCompatDelegate.setDefaultNightMode(
            enableNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        
        Toast.makeText(this, enableNight ? "🌙 Modo oscuro activado" : "☀️ Modo claro activado", Toast.LENGTH_SHORT).show();
        safeRecreate();
    }

    /**
     * Activa el modo foco (azul) con giroscopio
     */
    private void activateFocusMode() {
        android.util.Log.d("Dashboard", "activateFocusMode llamado: isRecreating=" + isRecreating + ", focusMode=" + focusMode);
        
        if (isRecreating) {
            android.util.Log.d("Dashboard", "Recreando, ignorando activación");
            return;
        }
        
        // Permitir activar/desactivar modo foco con giros
        if (focusMode) {
            android.util.Log.d("Dashboard", "Desactivando modo foco (ya estaba activo)");
            focusMode = false;
            prefs.edit().putBoolean(KEY_FOCUS_MODE, false).apply();
            // Restaurar modo nocturno guardado
            AppCompatDelegate.setDefaultNightMode(
                isNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Toast.makeText(this, "💙 Modo Foco Desactivado", Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.d("Dashboard", "Activando modo foco");
            focusMode = true;
            prefs.edit().putBoolean(KEY_FOCUS_MODE, true).apply();
            Toast.makeText(this, "💙 Modo Foco Activado!", Toast.LENGTH_SHORT).show();

            // Registrar evento en mapa
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    HabitEventStore.add(new HabitEvent(
                            loc.getLatitude(),
                            loc.getLongitude(),
                            "Modo Foco 🧘 Activado",
                            HabitEvent.HabitType.FOCUS
                    ));
                }
            });
        }
        
        safeRecreate();
    }

    /**
     * Recrea la Activity de forma segura sin loops
     */
    private void safeRecreate() {
        if (isRecreating) {
            android.util.Log.d("Dashboard", "Ya se está recreando, ignorando");
            return;
        }
        
        android.util.Log.d("Dashboard", "Iniciando recreación segura");
        isRecreating = true;
        
        // Detener sensores antes de recrear
        if (lightSensor != null) lightSensor.stop();
        if (accelerometerSensor != null) accelerometerSensor.stop();
        if (gyroSensor != null) gyroSensor.stop();
        
        // Recrear después de un pequeño delay
        mainHandler.postDelayed(() -> {
            android.util.Log.d("Dashboard", "Ejecutando recreate()");
            recreate();
        }, 300);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Limpiar handlers pendientes
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // Detener sensores
        if (walkSensor != null) walkSensor.stop();
        if (lightSensor != null) lightSensor.stop();
        if (accelerometerSensor != null) accelerometerSensor.stop();
        if (gyroSensor != null) gyroSensor.stop();
    }

    /**
     * Carga los hábitos predeterminados y restaura sus estados de completado
     */
    private List<Habit> loadHabitsWithState() {
        List<Habit> defaultHabits = Habit.defaultHabits();
        
        // Cargar estados guardados
        String habitsStateJson = prefs.getString(KEY_HABITS_STATE, null);
        if (habitsStateJson != null) {
            try {
                JSONObject stateJson = new JSONObject(habitsStateJson);
                
                // Restaurar estado de cada hábito
                for (Habit habit : defaultHabits) {
                    String habitKey = habit.getTitle(); // Usar título como key
                    if (stateJson.has(habitKey)) {
                        boolean completed = stateJson.getBoolean(habitKey);
                        habit.setCompleted(completed);
                        android.util.Log.d("Dashboard", "Restaurado estado de " + habitKey + ": " + completed);
                    }
                }
            } catch (JSONException e) {
                android.util.Log.e("Dashboard", "Error al cargar estados de hábitos", e);
            }
        }
        
        return defaultHabits;
    }

    /**
     * Guarda los estados de completado de todos los hábitos
     */
    private void saveHabitsState() {
        try {
            JSONObject stateJson = new JSONObject();
            
            for (Habit habit : habits) {
                stateJson.put(habit.getTitle(), habit.isCompleted());
            }
            
            prefs.edit().putString(KEY_HABITS_STATE, stateJson.toString()).apply();
            android.util.Log.d("Dashboard", "Estados de hábitos guardados");
        } catch (JSONException e) {
            android.util.Log.e("Dashboard", "Error al guardar estados de hábitos", e);
        }
    }

    /**
     * Completa un hábito por su tipo (EXERCISE, WALK, DEMO)
     */
    private void completeHabitByType(Habit.HabitType type) {
        for (Habit habit : habits) {
            if (habit.getType() == type && !habit.isCompleted()) {
                habit.setCompleted(true);
                
                // Guardar estado inmediatamente
                saveHabitsState();
                
                // Guardar evento en el mapa (excepto WALK que ya lo guarda StepSensorManager)
                if (type == Habit.HabitType.EXERCISE) {
                    fused.getLastLocation().addOnSuccessListener(loc -> {
                        if (loc != null) {
                            HabitEventStore.add(new HabitEvent(
                                    loc.getLatitude(),
                                    loc.getLongitude(),
                                    "Ejercicio ✅ Completado",
                                    HabitEvent.HabitType.EXERCISE
                            ));
                            android.util.Log.d("Dashboard", "Evento de ejercicio guardado en mapa");
                        }
                    });
                }
                // Nota: WALK ya guarda su evento en StepSensorManager, DEMO lo guarda en completeDemoHabit
                
                // Actualizar UI
                int position = habits.indexOf(habit);
                if (position >= 0) {
                    adapter.notifyItemChanged(position);
                } else {
                    adapter.notifyDataSetChanged();
                }
                
                android.util.Log.d("Dashboard", "Hábito completado: " + habit.getTitle());
                Toast.makeText(this, "✅ " + habit.getTitle() + " completado", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    /** ✅ Toggle hábito con click - puede marcar/desmarcar */
    private void completeDemoHabit(Habit h) {
        // Si ya está completado, desmarcarlo (toggle)
        if (h.isCompleted()) {
            h.setCompleted(false);
            
            // Guardar estado inmediatamente
            saveHabitsState();
            
            // Actualizar UI
            int position = habits.indexOf(h);
            if (position >= 0) {
                adapter.notifyItemChanged(position);
            } else {
                adapter.notifyDataSetChanged();
            }
            
            Toast.makeText(this, "Hábito desmarcado", Toast.LENGTH_SHORT).show();
            android.util.Log.d("Dashboard", "Hábito desmarcado: " + h.getTitle());
            return;
        }

        // Si no está completado, solo DEMO puede marcarse manualmente
        if (h.getType() == Habit.HabitType.DEMO) {
            h.setCompleted(true);
            
            // Guardar estado inmediatamente
            saveHabitsState();
            
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    HabitEventStore.add(new HabitEvent(
                            loc.getLatitude(),
                            loc.getLongitude(),
                            "Demo ✅ Completado",
                            HabitEvent.HabitType.DEMO
                    ));
                }
            });
            // Actualizar solo el item específico para mejor rendimiento
            int position = habits.indexOf(h);
            if (position >= 0) {
                adapter.notifyItemChanged(position);
            } else {
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this,
                    "Esto se completa automáticamente con sensores ✅",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
