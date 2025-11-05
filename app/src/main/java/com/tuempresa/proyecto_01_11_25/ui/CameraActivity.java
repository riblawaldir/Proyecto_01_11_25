package com.tuempresa.proyecto_01_11_25.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.HabitEvent;
import com.tuempresa.proyecto_01_11_25.model.HabitEventStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView txtStatus;
    private TextView txtInstructions;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private TextRecognizer textRecognizer;
    private ExecutorService analysisExecutor;
    private FusedLocationProviderClient fused;

    private boolean isReadingDetected = false;
    private static final int MIN_TEXT_LENGTH = 50; // Mínimo de caracteres para considerar una página
    private static final int MIN_LINES = 5; // Mínimo de líneas de texto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        txtStatus = findViewById(R.id.txtStatus);
        txtInstructions = findViewById(R.id.txtInstructions);
        
        fused = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar ML Kit Text Recognition
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        analysisExecutor = Executors.newSingleThreadExecutor();

        // Botón cerrar
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Iniciar cámara
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image Analysis para ML Kit
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);

                // Selector de cámara (trasera)
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind use cases antes de rebinding
                cameraProvider.unbindAll();

                // Bind use cases
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                android.util.Log.d("CameraActivity", "Cámara iniciada correctamente");

            } catch (Exception e) {
                android.util.Log.e("CameraActivity", "Error al iniciar cámara", e);
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy image) {
        if (isReadingDetected) {
            image.close();
            return; // Ya detectamos lectura, no procesar más
        }

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        textRecognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    if (isReadingDetected) {
                        image.close();
                        return;
                    }

                    String fullText = text.getText();
                    int lineCount = text.getTextBlocks().size();
                    int totalChars = fullText.length();

                    android.util.Log.d("CameraActivity", 
                        String.format("Texto detectado: %d caracteres, %d bloques", 
                            totalChars, lineCount));

                    // Verificar si es una página de libro
                    if (totalChars >= MIN_TEXT_LENGTH && lineCount >= MIN_LINES) {
                        // ¡Página de libro detectada!
                        isReadingDetected = true;
                        runOnUiThread(() -> {
                            txtStatus.setText("✅ ¡Página detectada!");
                            txtStatus.setTextColor(Color.parseColor("#4CAF50"));
                            txtInstructions.setText("Marcando hábito como completado...");
                            
                            // Completar hábito y guardar en mapa
                            completeReadingHabit();
                            
                            // Cerrar después de 2 segundos
                            previewView.postDelayed(() -> finish(), 2000);
                        });
                    } else {
                        runOnUiThread(() -> {
                            txtStatus.setText("Buscando texto... (" + totalChars + " caracteres)");
                            txtStatus.setTextColor(Color.WHITE);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CameraActivity", "Error en reconocimiento de texto", e);
                    runOnUiThread(() -> {
                        txtStatus.setText("Error al analizar imagen");
                        txtStatus.setTextColor(Color.RED);
                    });
                })
                .addOnCompleteListener(task -> image.close());
    }

    private void completeReadingHabit() {
        // Obtener ubicación actual

        fused.getLastLocation().addOnSuccessListener(location -> {
            double lat = 0;
            double lng = 0;

            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }

            // Guardar evento en el mapa
            HabitEventStore.add(new HabitEvent(
                    lat,
                    lng,
                    "Leer ✅ Página detectada",
                    HabitEvent.HabitType.READ
            ));

            android.util.Log.d("CameraActivity", "Evento de lectura guardado en mapa");

            // Notificar a DashboardActivity que debe actualizar el hábito
            Intent resultIntent = new Intent();
            resultIntent.putExtra("habit_completed", "READ");
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "✅ ¡Hábito de leer completado!", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}

