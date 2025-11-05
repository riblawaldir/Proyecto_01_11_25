# 📚 Guía de Estudio - Proyecto Hábitos Android

## Tabla de Contenidos
1. [Arquitectura del Proyecto](#arquitectura-del-proyecto)
2. [Sensores Android](#sensores-android)
3. [Geolocalización y Mapas](#geolocalización-y-mapas)
4. [Persistencia de Datos](#persistencia-de-datos)
5. [Cambio Dinámico de Temas](#cambio-dinámico-de-temas)
6. [ML Kit y CameraX](#ml-kit-y-camerax)
7. [Conceptos Clave de Android](#conceptos-clave-de-android)
8. [Ejercicios Prácticos](#ejercicios-prácticos)

---

## 🏗️ Arquitectura del Proyecto

### Estructura de Paquetes

```
com.tuempresa.proyecto_01_11_25/
├── ui/                    # Activities y componentes de UI
│   ├── DashboardActivity  # Pantalla principal
│   ├── MapActivity        # Visualización de mapas
│   └── CameraActivity     # Cámara y ML Kit
├── sensors/               # Gestores de sensores
│   ├── LightSensorManager
│   ├── GyroSensorManager
│   ├── AccelerometerSensorManager
│   └── StepSensorManager
├── model/                 # Modelos de datos
│   ├── Habit
│   ├── HabitEvent
│   └── HabitEventStore
└── MainActivity / SplashActivity
```

### Flujo de Datos

```
Sensores → Managers → Callbacks → DashboardActivity → Modelos → UI
                    ↓
              HabitEventStore (Persistencia)
                    ↓
              MapActivity (Visualización)
```

---

## 📱 Sensores Android

### Conceptos Fundamentales

#### 1. ¿Qué son los Sensores?
Los sensores son componentes de hardware del dispositivo Android que miden propiedades físicas del entorno o del dispositivo mismo.

#### 2. Tipos de Sensores en el Proyecto

##### **Sensor de Luz (TYPE_LIGHT)**
- **Propósito**: Detectar nivel de iluminación ambiental
- **Unidad**: Lux (lx)
- **Umbral**: 15 lux
  - < 15 lux → Modo oscuro
  - ≥ 15 lux → Modo claro

**Implementación**:
```java
// Obtener sensor
SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

// Registrar listener
sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

// Recibir eventos
@Override
public void onSensorChanged(SensorEvent event) {
    float lightValue = event.values[0]; // Valor en lux
    // Procesar...
}
```

**Características importantes**:
- **Debounce**: Esperar 2.5 segundos antes de notificar cambios (evita parpadeos)
- **Estado inicial**: Se inicializa en la primera lectura
- **Delays disponibles**:
  - `SENSOR_DELAY_FASTEST`: 0ms (máximo consumo)
  - `SENSOR_DELAY_GAME`: 20ms
  - `SENSOR_DELAY_UI`: 60ms
  - `SENSOR_DELAY_NORMAL`: 200ms (recomendado)

##### **Giroscopio (TYPE_GYROSCOPE)**
- **Propósito**: Detectar rotación del dispositivo
- **Unidad**: Radianes por segundo (rad/s)
- **Umbral**: 4.5 rad/s
- **Lógica**: 3 rotaciones consecutivas dentro de 1.2 segundos cada una

**Implementación**:
```java
// Calcular magnitud de rotación
float rotX = Math.abs(event.values[0]);
float rotY = Math.abs(event.values[1]);
float rotZ = Math.abs(event.values[2]);

double rotationMagnitude = Math.sqrt(rotX * rotX + rotY * rotY + rotZ * rotZ);

if (rotationMagnitude > ROTATION_THRESHOLD) {
    // Contar rotaciones consecutivas
    rotationCount++;
    if (rotationCount >= 3) {
        // Activar modo foco
    }
}
```

**Características**:
- **Reset automático**: Si pasa más de 1.2s entre rotaciones, se reinicia el contador
- **Flag de activación**: `isFocusModeActive` previene múltiples activaciones

##### **Acelerómetro (TYPE_ACCELEROMETER)**
- **Propósito**: Detectar movimiento/ejercicio
- **Unidad**: m/s²
- **Umbral**: 12.0 m/s²
- **Lógica**: Movimiento sostenido por 3 segundos

**Implementación**:
```java
// Calcular magnitud de aceleración
float x = event.values[0];
float y = event.values[1];
float z = event.values[2];
float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

// Restar gravedad (9.8 m/s²)
float movementMagnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);

if (movementMagnitude > MOVEMENT_THRESHOLD) {
    // Iniciar contador de duración
    if (movementStartTime == 0) {
        movementStartTime = System.currentTimeMillis();
    } else if (now - movementStartTime >= 3000) {
        // Ejercicio detectado
    }
}
```

**Características**:
- **Filtrado de gravedad**: Se resta 9.8 m/s² para obtener solo movimiento
- **Duración mínima**: Requiere 3 segundos continuos
- **Debounce**: 5 segundos entre detecciones

##### **Sensor de Pasos (Ubicación GPS)**
- **Propósito**: Medir distancia caminada
- **Método**: GPS (FusedLocationProviderClient)
- **Meta**: 150 metros acumulados

**Implementación**:
```java
// Crear LocationRequest
LocationRequest req = new LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,  // Alta precisión
    2000  // Intervalo de actualización: 2 segundos
)
.setMinUpdateIntervalMillis(1500)    // Mínimo: 1.5s
.setMinUpdateDistanceMeters(2)        // Mínimo: 2 metros
.build();

// Registrar callback
fused.requestLocationUpdates(req, callback, looper);

// En el callback:
for (Location loc : result.getLocations()) {
    if (last != null) {
        accMeters += last.distanceTo(loc);  // Acumular distancia
    }
    last = loc;
}
```

**Características**:
- **FusedLocationProviderClient**: Combina GPS, WiFi y red móvil
- **Prioridades**:
  - `PRIORITY_HIGH_ACCURACY`: GPS (más preciso, más batería)
  - `PRIORITY_BALANCED_POWER_ACCURACY`: WiFi + red
  - `PRIORITY_LOW_POWER`: Solo red
- **Cálculo de distancia**: `Location.distanceTo()` usa fórmula de Haversine

### Ciclo de Vida de Sensores

```java
// 1. Obtener SensorManager
SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

// 2. Obtener sensor específico
Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);

// 3. Registrar listener (en onResume o después de delay)
sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

// 4. Procesar eventos en onSensorChanged()
@Override
public void onSensorChanged(SensorEvent event) {
    // Procesar valores...
}

// 5. Desregistrar (en onPause o onDestroy)
sm.unregisterListener(this);
```

### ⚠️ Mejores Prácticas

1. **Siempre desregistrar sensores** en `onDestroy()` o `onPause()`
2. **Usar debounce** para evitar cambios rápidos
3. **Verificar disponibilidad** del sensor antes de usarlo
4. **Manejar permisos** (especialmente para ubicación)
5. **Optimizar delays** según necesidad (batería vs precisión)

---

## 🗺️ Geolocalización y Mapas

### FusedLocationProviderClient

**¿Qué es?**
API de Google Play Services que combina múltiples fuentes de ubicación (GPS, WiFi, red móvil) para obtener la mejor ubicación disponible.

**Inicialización**:
```java
FusedLocationProviderClient fused = 
    LocationServices.getFusedLocationProviderClient(context);
```

**Obtener ubicación actual**:
```java
fused.getLastLocation().addOnSuccessListener(location -> {
    if (location != null) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        // Usar ubicación...
    }
});
```

**Actualizaciones continuas**:
```java
LocationRequest request = new LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    2000  // Intervalo: 2 segundos
)
.setMinUpdateIntervalMillis(1500)
.setMinUpdateDistanceMeters(2)  // Solo actualizar si se movió 2m
.build();

LocationCallback callback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult result) {
        for (Location loc : result.getLocations()) {
            // Procesar cada ubicación...
        }
    }
};

fused.requestLocationUpdates(request, callback, looper);
```

**Detener actualizaciones**:
```java
fused.removeLocationUpdates(callback);
```

### Google Maps API

**Configuración**:
1. **API Key** en `res/values/strings.xml`:
```xml
<string name="Api_Key">TU_API_KEY</string>
```

2. **Manifest**:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="@string/Api_Key" />
```

3. **Dependencia**:
```gradle
implementation("com.google.android.gms:play-services-maps:18.1.0")
```

**Implementación básica**:
```java
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        // Obtener fragmento del mapa
        SupportMapFragment mapFragment = 
            (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        
        // Obtener mapa asíncronamente
        mapFragment.getMapAsync(this);
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        // Configurar mapa...
    }
}
```

**Agregar marcadores**:
```java
// Marcador simple
LatLng position = new LatLng(lat, lng);
map.addMarker(new MarkerOptions()
    .position(position)
    .title("Título del marcador")
    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
);

// Marcador con icono personalizado
BitmapDescriptor icon = bitmapDescriptorFromVector(context, R.drawable.ic_fitness_center_24);
map.addMarker(new MarkerOptions()
    .position(position)
    .title("Ejercicio completado")
    .icon(icon)
);
```

**Convertir Drawable a BitmapDescriptor**:
```java
private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
    Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
    vectorDrawable.setBounds(0, 0, 96, 96);  // 24dp * 4 = 96px
    
    Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.draw(canvas);
    
    return BitmapDescriptorFactory.fromBitmap(bitmap);
}
```

**Mover cámara**:
```java
// Zoom a una ubicación
map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));

// Zoom suave
map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
```

### Permisos de Ubicación

**En AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

**Solicitar permisos en runtime**:
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        100);
}
```

**Manejar resultado**:
```java
@Override
public void onRequestPermissionsResult(int requestCode, 
        @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (requestCode == 100 && grantResults.length > 0 
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permiso concedido - iniciar ubicación
    }
}
```

---

## 💾 Persistencia de Datos

### SharedPreferences

**¿Qué es?**
Almacenamiento clave-valor simple para datos primitivos. Ideal para configuraciones y estados pequeños.

**Uso básico**:
```java
// Obtener instancia
SharedPreferences prefs = getSharedPreferences("MiApp", Context.MODE_PRIVATE);

// Guardar
prefs.edit()
    .putBoolean("modo_noche", true)
    .putString("nombre", "Juan")
    .putInt("edad", 25)
    .apply();  // Asíncrono (recomendado)
    // .commit();  // Síncrono (más lento)

// Leer
boolean modoNoche = prefs.getBoolean("modo_noche", false);
String nombre = prefs.getString("nombre", "Default");
int edad = prefs.getInt("edad", 0);
```

**Guardar objetos complejos (JSON)**:
```java
// Guardar lista de eventos como JSON
JSONArray jsonArray = new JSONArray();
for (HabitEvent event : events) {
    JSONObject jsonEvent = new JSONObject();
    jsonEvent.put("lat", event.getLat());
    jsonEvent.put("lng", event.getLng());
    jsonEvent.put("note", event.getNote());
    jsonEvent.put("type", event.getType().name());
    jsonArray.put(jsonEvent);
}

prefs.edit()
    .putString("habit_events", jsonArray.toString())
    .apply();

// Cargar
String eventsJson = prefs.getString("habit_events", null);
if (eventsJson != null) {
    JSONArray jsonArray = new JSONArray(eventsJson);
    for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonEvent = jsonArray.getJSONObject(i);
        // Reconstruir objeto...
    }
}
```

**Ventajas**:
- ✅ Simple y rápido
- ✅ Persistente entre sesiones
- ✅ No requiere base de datos

**Limitaciones**:
- ❌ Solo tipos primitivos
- ❌ No recomendado para grandes volúmenes
- ❌ No soporta queries complejas

---

## 🎨 Cambio Dinámico de Temas

### AppCompatDelegate

**¿Qué es?**
API de AndroidX que permite cambiar el tema de la aplicación dinámicamente, especialmente para modo claro/oscuro.

**Modos disponibles**:
```java
AppCompatDelegate.MODE_NIGHT_NO      // Forzar modo claro
AppCompatDelegate.MODE_NIGHT_YES     // Forzar modo oscuro
AppCompatDelegate.MODE_NIGHT_AUTO    // Seguir sistema
AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM  // Seguir sistema (API 33+)
```

**Uso**:
```java
// Cambiar modo nocturno
AppCompatDelegate.setDefaultNightMode(
    isNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
);

// Recrear actividad para aplicar cambios
recreate();
```

**Temas personalizados**:
```xml
<!-- values/themes.xml (modo claro) -->
<style name="Theme.Proyecto_01_11_25" parent="Theme.Material3.Light.NoActionBar">
    <item name="colorPrimary">@color/teal_700</item>
    <item name="android:windowBackground">@color/cardWhite</item>
</style>

<!-- values-night/themes.xml (modo oscuro) -->
<style name="Theme.Proyecto_01_11_25" parent="Theme.Material3.Dark.NoActionBar">
    <item name="android:windowBackground">@color/black</item>
    <item name="android:textColor">@color/white</item>
</style>
```

**Tema Focus (azul)**:
```xml
<style name="Theme.Proyecto_01_11_25.Focus" 
    parent="Theme.Material3.Dark.NoActionBar">
    <item name="colorPrimary">#0047FF</item>
    <item name="android:windowBackground">#002060</item>
    <item name="android:textColorPrimary">#FFFFFFFF</item>
</style>
```

**Aplicar tema antes de setContentView**:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // IMPORTANTE: Aplicar tema ANTES de setContentView
    if (focusMode) {
        setTheme(R.style.Theme_Proyecto_01_11_25_Focus);
    } else {
        AppCompatDelegate.setDefaultNightMode(
            isNight ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
    
    setContentView(R.layout.activity_dashboard);
    // ...
}
```

### Prevención de Loops Infinitos

**Problema**: `recreate()` puede causar loops infinitos si el sensor detecta cambios continuamente.

**Solución**:
```java
// 1. Flag de recreación
private boolean isRecreating = false;

// 2. Cooldown después de recrear
private static final long RECREATION_COOLDOWN_MS = 8000;
private long lastRecreationTime = 0;

// 3. Guardar tiempo en SharedPreferences
prefs.edit().putLong("last_recreation_time", lastRecreationTime).apply();

// 4. Verificar cooldown antes de recrear
long timeSinceRecreation = System.currentTimeMillis() - lastRecreationTime;
if (timeSinceRecreation < RECREATION_COOLDOWN_MS) {
    return; // No recrear
}

// 5. Detener sensores antes de recrear
lightSensor.stop();
safeRecreate();
```

---

## 📷 ML Kit y CameraX

### CameraX

**¿Qué es?**
Biblioteca de Android Jetpack para trabajar con cámara de forma más simple y consistente.

**Componentes principales**:
1. **Preview**: Vista previa en tiempo real
2. **ImageCapture**: Capturar fotos
3. **ImageAnalysis**: Analizar frames en tiempo real

**Configuración básica**:
```java
// 1. Obtener ProcessCameraProvider
ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
    ProcessCameraProvider.getInstance(context);

// 2. Cuando esté listo
cameraProviderFuture.addListener(() -> {
    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
    
    // 3. Preview
    Preview preview = new Preview.Builder().build();
    preview.setSurfaceProvider(previewView.getSurfaceProvider());
    
    // 4. ImageAnalysis (para ML Kit)
    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build();
    
    imageAnalysis.setAnalyzer(executor, imageProxy -> {
        // Analizar frame...
        imageProxy.close();
    });
    
    // 5. Seleccionar cámara
    CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
    
    // 6. Bind al lifecycle
    camera = cameraProvider.bindToLifecycle(
        this, selector, preview, imageAnalysis
    );
}, ContextCompat.getMainExecutor(context));
```

**Estrategias de backpressure**:
- `STRATEGY_KEEP_ONLY_LATEST`: Mantener solo el último frame (más rápido)
- `STRATEGY_BLOCK_PRODUCER`: Bloquear hasta procesar (más lento pero sin pérdida)

### ML Kit Text Recognition

**¿Qué es?**
API de Google ML Kit para reconocer texto en imágenes.

**Configuración**:
```gradle
implementation("com.google.mlkit:text-recognition:16.0.1")
```

**Uso básico**:
```java
// 1. Crear TextRecognizer
TextRecognizer recognizer = TextRecognition.getClient(
    TextRecognizerOptions.DEFAULT_OPTIONS
);

// 2. Convertir ImageProxy a InputImage
InputImage inputImage = InputImage.fromMediaImage(
    imageProxy.getImage(),
    imageProxy.getImageInfo().getRotationDegrees()
);

// 3. Procesar
recognizer.process(inputImage)
    .addOnSuccessListener(text -> {
        String fullText = text.getText();
        int charCount = fullText.length();
        int blockCount = text.getTextBlocks().size();
        
        // Verificar si es página de libro
        if (charCount >= 50 && blockCount >= 5) {
            // Página detectada
        }
    })
    .addOnFailureListener(e -> {
        // Error
    })
    .addOnCompleteListener(task -> {
        imageProxy.close(); // IMPORTANTE: Cerrar siempre
    });
```

**Criterios de detección (página de libro)**:
- Mínimo 50 caracteres
- Mínimo 5 bloques de texto
- Esto filtra texto corto (etiquetas, botones, etc.)

**Limpieza de recursos**:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (textRecognizer != null) {
        textRecognizer.close();  // Liberar recursos
    }
    if (cameraProvider != null) {
        cameraProvider.unbindAll();  // Desvincular cámara
    }
}
```

---

## 🔑 Conceptos Clave de Android

### Activity Lifecycle

```
onCreate() → onStart() → onResume() → [ACTIVA]
                                    ↓
onPause() ← onStop() ← onDestroy() ← [DESTRUIDA]
```

**Momentos clave**:
- `onCreate()`: Inicializar UI, cargar datos, iniciar sensores (después de delay)
- `onResume()`: Activar sensores si es necesario
- `onPause()`: Pausar sensores para ahorrar batería
- `onDestroy()`: Detener sensores, limpiar recursos

### Handler y Looper

**¿Qué son?**
- **Handler**: Envía mensajes y Runnables a un hilo específico
- **Looper**: Mantiene un bucle de mensajes en un hilo

**Uso común**:
```java
// Handler del hilo principal
Handler mainHandler = new Handler(Looper.getMainLooper());

// Ejecutar después de delay
mainHandler.postDelayed(() -> {
    // Código a ejecutar después de 5 segundos
}, 5000);

// Limpiar todos los callbacks pendientes
mainHandler.removeCallbacksAndMessages(null);
```

**¿Por qué usar Handler?**
- Ejecutar código en el hilo principal desde otro hilo
- Retrasar ejecución (debounce, delays)
- Evitar bloquear el hilo principal

### RecyclerView y Adapter

**Conceptos**:
- **RecyclerView**: Lista eficiente de elementos
- **Adapter**: Conecta datos con vistas
- **ViewHolder**: Cache de vistas para mejor rendimiento

**Patrón básico**:
```java
public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.ViewHolder> {
    private List<Habit> data;
    private OnHabitClickListener listener;
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_habit, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Habit habit = data.get(position);
        holder.bind(habit);
    }
    
    @Override
    public int getItemCount() {
        return data.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        // Views del item
        TextView txtName;
        
        ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
        }
        
        void bind(Habit habit) {
            txtName.setText(habit.getTitle());
        }
    }
}
```

### Callbacks y Interfaces

**Patrón Callback**:
```java
// Definir interface
public interface OnLowLightListener {
    void onLowLight();
    void onNormalLight();
}

// Implementar en Activity
LightSensorManager lightSensor = new LightSensorManager(this, 
    new LightSensorManager.OnLowLightListener() {
        @Override
        public void onLowLight() {
            handleLightChange(true);
        }
        
        @Override
        public void onNormalLight() {
            handleLightChange(false);
        }
    }
);

// O con lambda (Java 8+)
LightSensorManager lightSensor = new LightSensorManager(this, 
    isLowLight -> handleLightChange(isLowLight)
);
```

**Ventajas**:
- Desacoplamiento: Sensor no conoce Activity
- Reutilización: Mismo sensor puede usarse en diferentes Activities
- Testeable: Fácil crear mocks

---

## 📊 Resumen de Sensores del Proyecto

| Sensor | Tipo | Propósito | Umbral | Debounce |
|--------|------|-----------|--------|----------|
| Luz | `TYPE_LIGHT` | Modo nocturno | 15 lux | 2.5s |
| Giroscopio | `TYPE_GYROSCOPE` | Modo foco | 4.5 rad/s | - |
| Acelerómetro | `TYPE_ACCELEROMETER` | Ejercicio | 12 m/s² | 5s |
| GPS | `FusedLocationProviderClient` | Caminar | 150m | - |

---

## 🎯 Puntos Clave para Estudiar

### 1. Gestión del Ciclo de Vida
- **Iniciar sensores**: Después de delay (evitar loops)
- **Detener sensores**: En `onDestroy()` o antes de `recreate()`
- **Limpiar recursos**: Handlers, Executors, TextRecognizers

### 2. Manejo de Permisos
- Solicitar en runtime (Android 6.0+)
- Verificar antes de usar funcionalidad
- Manejar negación de permisos

### 3. Prevención de Loops
- Cooldown después de recrear
- Debounce en cambios de sensor
- Flags de control (`isRecreating`, `isListening`)

### 4. Persistencia
- `SharedPreferences` para configuraciones
- JSON para objetos complejos
- Inicializar en `onCreate()` de Activity principal

### 5. Threading
- Sensores en hilo de UI
- ML Kit en Executor separado
- Handlers para delays y callbacks

---

## 📝 Ejercicios Prácticos

### Ejercicio 1: Crear un nuevo sensor
Crear un `ProximitySensorManager` que detecte cuando el dispositivo está cerca de una superficie (para pausar música).

**Solución**:
```java
public class ProximitySensorManager implements SensorEventListener {
    private static final float PROXIMITY_THRESHOLD = 5.0f; // cm
    
    public interface OnProximityListener {
        void onNear();
        void onFar();
    }
    
    // Implementar similar a LightSensorManager
}
```

### Ejercicio 2: Agregar un nuevo tipo de hábito
1. Agregar `MEDITATION` a `HabitType`
2. Crear sensor que detecte quietud (acelerómetro con valores bajos)
3. Guardar evento en mapa

### Ejercicio 3: Mejorar persistencia
Cambiar de `SharedPreferences` a Room Database para eventos de hábitos.

---

## 🔗 Recursos Adicionales

### Documentación Oficial
- [Android Sensors](https://developer.android.com/guide/topics/sensors)
- [Location Services](https://developer.android.com/training/location)
- [Google Maps Android](https://developers.google.com/maps/documentation/android-sdk)
- [CameraX](https://developer.android.com/training/camerax)
- [ML Kit](https://developers.google.com/ml-kit)

### Código del Proyecto
- `LightSensorManager.java`: Sensor de luz con debounce
- `StepSensorManager.java`: GPS para distancia
- `HabitEventStore.java`: Persistencia con JSON
- `MapActivity.java`: Integración con Google Maps
- `CameraActivity.java`: CameraX + ML Kit

---

## ❓ Preguntas de Repaso

1. ¿Por qué se usa debounce en los sensores?
2. ¿Cuál es la diferencia entre `apply()` y `commit()` en SharedPreferences?
3. ¿Por qué `recreate()` puede causar loops infinitos?
4. ¿Cómo se calcula la distancia entre dos ubicaciones GPS?
5. ¿Qué es un Executor y por qué se usa en ImageAnalysis?
6. ¿Cuál es la diferencia entre `SENSOR_DELAY_NORMAL` y `SENSOR_DELAY_FASTEST`?
7. ¿Por qué se debe llamar `setTheme()` antes de `setContentView()`?
8. ¿Qué hace `FusedLocationProviderClient` mejor que GPS puro?

---

## 📚 Conceptos Avanzados

### Debounce Pattern
Técnica para evitar ejecutar una función múltiples veces seguidas. Solo se ejecuta después de que pase un tiempo sin nuevos eventos.

```java
private long lastEventTime = 0;
private static final long DEBOUNCE_MS = 2000;

if (System.currentTimeMillis() - lastEventTime >= DEBOUNCE_MS) {
    // Ejecutar acción
    lastEventTime = System.currentTimeMillis();
}
```

### Singleton Pattern (HabitEventStore)
Un solo punto de acceso para los eventos:
```java
public class HabitEventStore {
    private static final List<HabitEvent> events = new ArrayList<>();
    
    public static synchronized void add(HabitEvent e) {
        events.add(e);
        saveEvents();
    }
    
    public static synchronized List<HabitEvent> all() {
        return new ArrayList<>(events);  // Copia defensiva
    }
}
```

### Observer Pattern (Callbacks)
Los sensores notifican cambios sin conocer quién los usa:
```java
// Sensor notifica cambios
listener.onLowLight();

// Activity reacciona
@Override
public void onLowLight() {
    changeNightMode(true);
}
```

---

¡Éxito en tus estudios! 🚀

