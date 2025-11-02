package com.tuempresa.proyecto_01_11_25.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.Habit;
import com.tuempresa.proyecto_01_11_25.model.HabitEvent;
import com.tuempresa.proyecto_01_11_25.model.HabitEventStore;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.VH> {

    public interface OnHabitClick {
        void onHabitClicked(Habit habit);
    }

    private final List<Habit> data;
    private final OnHabitClick listener;

    public HabitAdapter(List<Habit> data, OnHabitClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Habit item = data.get(pos);
        h.txtName.setText(item.getTitle());
        h.txtGoal.setText(item.getGoal());
        h.txtType.setText(item.getCategory());
        
        // Configurar progreso y color según estado
        boolean isCompleted = item.isCompleted();
        h.progress.setProgress(isCompleted ? 100 : 25);
        
        // Cambiar color de la barra: verde si está completado, naranja si no
        if (isCompleted) {
            // Barra verde para tareas completadas
            h.progress.setProgressDrawable(
                ContextCompat.getDrawable(
                    h.itemView.getContext(), 
                    R.drawable.progress_circle_drawable_green
                )
            );
        } else {
            // Barra naranja para tareas pendientes
            h.progress.setProgressDrawable(
                ContextCompat.getDrawable(
                    h.itemView.getContext(), 
                    R.drawable.progress_circle_drawable
                )
            );
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHabitClicked(item);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtGoal, txtType;
        ProgressBar progress;
        VH(@NonNull View v) {
            super(v);
            txtName  = v.findViewById(R.id.txtHabitName);
            txtGoal  = v.findViewById(R.id.txtGoal);
            txtType  = v.findViewById(R.id.txtType);
            progress = v.findViewById(R.id.progressHabit);
        }
    }
}
