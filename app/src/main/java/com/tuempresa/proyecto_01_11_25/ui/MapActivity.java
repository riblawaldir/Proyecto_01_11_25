package com.tuempresa.proyecto_01_11_25.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.HabitEvent;
import com.tuempresa.proyecto_01_11_25.model.HabitEventStore;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment frag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (frag != null) frag.getMapAsync(this);

        FloatingActionButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        drawEvents();
    }

    private void drawEvents() {
        List<HabitEvent> list = HabitEventStore.all();
        if (list.isEmpty()) return;

        LatLng focus = null;
        for (HabitEvent e : list) {
            LatLng p = new LatLng(e.getLat(), e.getLng());
            
            // Usar iconos personalizados según el tipo de evento
            BitmapDescriptor icon = getIconForEventType(e.getType());

            map.addMarker(new MarkerOptions()
                    .position(p)
                    .title(e.getNote())
                    .icon(icon));
            focus = p;
        }
        if (focus != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16f));
        }
    }

    /**
     * Obtiene el icono vectorial correspondiente al tipo de evento
     */
    private BitmapDescriptor getIconForEventType(HabitEvent.HabitType type) {
        int drawableId;
        switch (type) {
            case EXERCISE:
                drawableId = R.drawable.ic_fitness_center_24;
                break;
            case WALK:
                drawableId = R.drawable.ic_directions_walk_24;
                break;
            case FOCUS:
                drawableId = R.drawable.ic_do_not_disturb_on_24;
                break;
            case READ:
                drawableId = R.drawable.ic_menu_book_24;
                break;
            case DEMO:
            default:
                // Para DEMO usamos el icono de ejercicio como fallback
                drawableId = R.drawable.ic_fitness_center_24;
                break;
        }
        
        return bitmapDescriptorFromVector(this, drawableId);
    }

    /**
     * Convierte un drawable vectorial en BitmapDescriptor para Google Maps
     */
    private BitmapDescriptor bitmapDescriptorFromVector(android.content.Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
        
        int width = 96; // 24dp * 4 para mejor calidad
        int height = 96;
        vectorDrawable.setBounds(0, 0, width, height);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
