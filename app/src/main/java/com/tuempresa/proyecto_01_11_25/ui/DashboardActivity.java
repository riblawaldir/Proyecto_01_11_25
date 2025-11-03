package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private static final String KEY_LAST_RECREATION_TIME = "last_recreation_time";
    private static final long SENSOR_DELAY_MS = 5000; // 5 segundos antes de activar sensores (evitar loops)
    private static final long LIGHT_DEBOUNCE_MS = 5000; // 5 segundos debounce (aumentado para evitar parpadeos)
    private static final long RECREATION_COOLDOWN_MS = 8000; // 8 segundos de cooldown después de recrear

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
    private long lastRecreationTime = 0; // Tiempo de la última recreación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar handler PRIMERO para poder usarlo en el cooldown
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Cargar estado persistente con valores por defecto: modo claro y foco desactivado
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Guardar tiempo de creación para delay inicial
        activityCreateTime = System.currentTimeMillis();
        
        // Cargar tiempo de última recreación desde SharedPreferences
        lastRecreationTime = prefs.getLong(KEY_LAST_RECREATION_TIME, 0);
        
        // Verificar si acabamos de recrear (evitar loops)
        long timeSinceLastRecreation = lastRecreationTime > 0 ? 
            System.currentTimeMillis() - lastRecreationTime : Long.MAX_VALUE;
        
        // Si acabamos de recrear, bloquear completamente cualquier otra recreación
        boolean justRecreated = timeSinceLastRecreation < RECREATION_COOLDOWN_MS;
        
        if (justRecreated) {
            android.util.Log.d("Dashboard", "⚠️ Recreación reciente, bloqueando cambios: " + timeSinceLastRecreation + "ms");
            isRecreating = true;
            // Mantener el flag por un tiempo adicional
            mainHandler.postDelayed(() -> {
                isRecreating = false;
                android.util.Log.d("Dashboard", "✅ Cooldown completado, sensores activos");
            }, RECREATION_COOLDOWN_MS - timeSinceLastRecreation);
        } else {
            isRecreating = false;
        }
        
        focusMode = prefs.getBoolean(KEY_FOCUS_MODE, false); // Por defecto: false (desactivado)
        isNight = prefs.getBoolean(KEY_NIGHT_MODE, false); // Por defecto: false (modo claro)
        
        // Configurar modo nocturno según el estado guardado
        // Si acabamos de recrear, NO cambiar AppCompatDelegate para evitar loops
        // El tema ya se aplicó con setTheme() en applyTheme()
        if (!justRecreated) {
            // Solo sincronizar AppCompatDelegate si no acabamos de recrear
            int currentNightMode = AppCompatDelegate.getDefaultNightMode();
            int desiredNightMode = isNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            
            if (currentNightMode != desiredNightMode) {
                android.util.Log.d("Dashboard", "Sincronizando modo nocturno: " + desiredNightMode);
                AppCompatDelegate.setDefaultNightMode(desiredNightMode);
            }
        } else {
            android.util.Log.d("Dashboard", "⚠️ Saltando sincronización de modo nocturno (en cooldown, tema ya aplicado)");
        }
        
        android.util.Log.d("Dashboard", "onCreate - focusMode: " + focusMode + ", isNight: " + isNight);
        
        // Aplicar tema ANTES de setContentView (crítico)
        applyTheme();
        
        setContentView(R.layout.activity_dashboard);

        fused = LocationServices.getFusedLocationProviderClient(this);

        // 🔥 Inicializar HabitEventStore para cargar eventos guardados
        HabitEventStore.init(this);

        // 🔥 Cargar hábitos (predeterminados o con estados guardados)
        habits = loadHabitsWithState();

        rv = findViewById(R.id.rvHabits);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HabitAdapter(habits, this::completeDemoHabit);
        rv.setAdapter(adapter);

        btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        FloatingActionButton btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> {
            // Verificar permiso de cámara
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.CAMERA}, 100);
            } else {
                startActivityForResult(new Intent(this, CameraActivity.class), 200);
            }
        });

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
        
        // Iniciar sensores después de un delay para evitar loops
        long delayBeforeStartingSensors = SENSOR_DELAY_MS;
        
        if (justRecreated) {
            // Si acabamos de recrear, esperar el cooldown completo + delay adicional
            long remainingCooldown = RECREATION_COOLDOWN_MS - timeSinceLastRecreation;
            delayBeforeStartingSensors = remainingCooldown + SENSOR_DELAY_MS + 2000; // +2s extra de seguridad
            android.util.Log.d("Dashboard", "⚠️ Sensores en espera: cooldown restante " + remainingCooldown + "ms, delay total: " + delayBeforeStartingSensors + "ms");
        }
        
        // Crear variable final para usar en lambda
        final long finalDelay = delayBeforeStartingSensors;
        
        // Los sensores DEBEN activarse después del delay, incluso si justRecreated era true
        // porque el delay ya incluye el tiempo de cooldown
        mainHandler.postDelayed(() -> {
            // Verificar nuevamente si estamos en cooldown (por si acaso)
            long currentTimeSinceRecreation = lastRecreationTime > 0 ? 
                System.currentTimeMillis() - lastRecreationTime : Long.MAX_VALUE;
            
            // Limpiar lastRecreationTime si ya pasó mucho tiempo (más de 2x cooldown)
            // Esto evita que se quede bloqueado si la app se reinició
            if (currentTimeSinceRecreation > RECREATION_COOLDOWN_MS * 2) {
                lastRecreationTime = 0;
                prefs.edit().remove(KEY_LAST_RECREATION_TIME).apply();
                android.util.Log.d("Dashboard", "🧹 Limpiando lastRecreationTime (muy antiguo)");
            }
            
            if (currentTimeSinceRecreation >= RECREATION_COOLDOWN_MS && !isRecreating && !isFinishing() && !isDestroyed()) {
                lightSensor.start();
                accelerometerSensor.start();
                gyroSensor.start();
                
                // Reiniciar lastLightChange para permitir cambios inmediatos después de activar sensores
                lastLightChange = 0;
                
                android.util.Log.d("Dashboard", "✅ Sensores activados después de delay: " + finalDelay + "ms (cooldown: " + currentTimeSinceRecreation + "ms)");
            } else {
                android.util.Log.d("Dashboard", "⚠️ Sensores NO activados: cooldown=" + currentTimeSinceRecreation + 
                    "ms, isRecreating=" + isRecreating + ", isFinishing=" + isFinishing());
            }
        }, finalDelay);
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
            // Para modo claro/oscuro, Android usa AppCompatDelegate automáticamente
            // basándose en values/ y values-night/ según el modo del sistema
            // No necesitamos hacer nada aquí, el tema se aplica automáticamente
            android.util.Log.d("Dashboard", "✅ Tema: " + (isNight ? "Modo oscuro" : "Modo claro"));
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
        
        long now = System.currentTimeMillis();
        
        // Evitar cambios durante recreación
        if (isRecreating) {
            android.util.Log.d("Dashboard", "Ignorando cambio: isRecreating=true");
            return;
        }
        
        // Evitar cambios poco tiempo después de crear la actividad
        long timeSinceCreation = now - activityCreateTime;
        if (timeSinceCreation < SENSOR_DELAY_MS) {
            android.util.Log.d("Dashboard", "Ignorando cambio: muy pronto después de crear (" + timeSinceCreation + "ms)");
            return;
        }
        
        // Evitar cambios poco tiempo después de recrear
        long timeSinceRecreation = lastRecreationTime > 0 ? now - lastRecreationTime : Long.MAX_VALUE;
        if (timeSinceRecreation < RECREATION_COOLDOWN_MS) {
            android.util.Log.d("Dashboard", "Ignorando cambio: en cooldown después de recrear (" + timeSinceRecreation + "ms < " + RECREATION_COOLDOWN_MS + "ms)");
            return;
        }

        // Debounce adicional (solo si lastLightChange > 0)
        // Si lastLightChange es 0, significa que acabamos de activar los sensores - permitir cambio inmediato
        if (lastLightChange > 0 && (now - lastLightChange < LIGHT_DEBOUNCE_MS)) {
            android.util.Log.d("Dashboard", "Ignorando cambio: debounce activo (" + (now - lastLightChange) + "ms < " + LIGHT_DEBOUNCE_MS + "ms)");
            return;
        }
        
        if (lastLightChange == 0) {
            android.util.Log.d("Dashboard", "✅ Primera detección después de activar sensores - permitiendo cambio");
        }

        // Solo cambiar si realmente cambió
        if ((isLowLight && !isNight) || (!isLowLight && isNight)) {
            lastLightChange = now;
            android.util.Log.d("Dashboard", "✅ Cambiando modo nocturno: " + isLowLight);
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
        
        // Detener sensor ANTES de cambiar para evitar eventos durante la recreación
        if (lightSensor != null) {
            android.util.Log.d("Dashboard", "Deteniendo sensor de luz antes de cambiar tema");
            lightSensor.stop();
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
            android.util.Log.d("Dashboard", "⚠️ Ya se está recreando, ignorando");
            return;
        }
        
        // Verificar cooldown antes de recrear
        long timeSinceLastRecreation = lastRecreationTime > 0 ? 
            System.currentTimeMillis() - lastRecreationTime : Long.MAX_VALUE;
        
        if (timeSinceLastRecreation < RECREATION_COOLDOWN_MS) {
            android.util.Log.d("Dashboard", "⚠️ Recreación bloqueada: en cooldown (" + timeSinceLastRecreation + "ms)");
            return;
        }
        
        android.util.Log.d("Dashboard", "✅ Iniciando recreación segura");
        isRecreating = true;
        lastRecreationTime = System.currentTimeMillis();
        
        // Guardar tiempo de recreación en SharedPreferences para persistir entre recreaciones
        prefs.edit().putLong(KEY_LAST_RECREATION_TIME, lastRecreationTime).apply();
        
        // Detener TODOS los sensores antes de recrear
        if (lightSensor != null) {
            lightSensor.stop();
            android.util.Log.d("Dashboard", "Sensor de luz detenido");
        }
        if (accelerometerSensor != null) {
            accelerometerSensor.stop();
            android.util.Log.d("Dashboard", "Sensor de acelerómetro detenido");
        }
        if (gyroSensor != null) {
            gyroSensor.stop();
            android.util.Log.d("Dashboard", "Sensor de giroscopio detenido");
        }
        
        // Limpiar handlers pendientes
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // Recrear después de un delay más largo para asegurar que los sensores se detuvieron
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) {
                android.util.Log.d("Dashboard", "Activity ya destruida, no recrear");
                isRecreating = false;
                return;
            }
            
            android.util.Log.d("Dashboard", "🔄 Ejecutando recreate()");
            try {
                recreate();
            } catch (Exception e) {
                android.util.Log.e("Dashboard", "Error al recrear", e);
                isRecreating = false;
            }
        }, 1000); // Aumentado a 1 segundo para dar más tiempo
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            String habitType = data.getStringExtra("habit_completed");
            if ("READ".equals(habitType)) {
                completeHabitByType(Habit.HabitType.READ);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(this, CameraActivity.class), 200);
        } else if (requestCode == 100) {
            Toast.makeText(this, "Se necesita permiso de cámara para leer", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Completa un hábito por su tipo (EXERCISE, WALK, READ, DEMO)
     */
    private void completeHabitByType(Habit.HabitType type) {
        for (Habit habit : habits) {
            if (habit.getType() == type && !habit.isCompleted()) {
                habit.setCompleted(true);
                
                // Guardar estado inmediatamente
                saveHabitsState();
                
                // Guardar evento en el mapa (excepto WALK que ya lo guarda StepSensorManager, READ lo guarda CameraActivity)
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
                // Nota: WALK ya guarda su evento en StepSensorManager, READ lo guarda CameraActivity, DEMO lo guarda en completeDemoHabit
                
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
