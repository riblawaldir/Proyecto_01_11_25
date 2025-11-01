package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.Habit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private final List<Habit> habitList;
    private final Context context;

    public HabitAdapter(Context context ,List<Habit> habitList) {
        this.context = context;
        this.habitList = habitList;

    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit_card, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit h = habitList.get(position);
        holder.txtHabitName.setText(h.getName());
        holder.txtGoal.setText("Goal: " + h.getGoal());
        holder.txtType.setText(h.getType());
        int startProgress = holder.progressHabit.getProgress();
        int endProgress = h.isDone() ? 100 : 40;


        int color = h.isDone() ? Color.parseColor("#4CAF50") : Color.parseColor("#FF9800");
        holder.progressHabit.getProgressDrawable().setTint(color);


        new Thread(() -> {
            int step = (endProgress > startProgress) ? 1 : -1;
            for (int i = startProgress; i != endProgress; i += step) {
                int progress = i;
                holder.progressHabit.post(() -> holder.progressHabit.setProgress(progress));
                try {
                    Thread.sleep(10); // velocidad de animación
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        holder.itemView.setOnClickListener(view -> {
            h.setDone((!h.isDone()));
            notifyItemChanged(position);
            saveHabitsToPrefs();
            Toast.makeText(context,h.isDone() ? "Marked as done " : "Marked as pending",Toast.LENGTH_SHORT).show();
        });
    }

    private void saveHabitsToPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("HabitusPrefs", Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (Habit h : habitList) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", h.getName());
                obj.put("goal", h.getGoal());
                obj.put("period", h.getPeriod());
                obj.put("type", h.getType());
                obj.put("done", h.isDone());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString("habits", array.toString()).apply();
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView txtHabitName, txtGoal, txtType;
        ProgressBar progressHabit;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            txtHabitName = itemView.findViewById(R.id.txtHabitName);
            txtGoal = itemView.findViewById(R.id.txtGoal);
            txtType = itemView.findViewById(R.id.txtType);
            progressHabit = itemView.findViewById(R.id.progressHabit);
        }
    }
}
