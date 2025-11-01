package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.Habit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreateHabitActivity extends AppCompatActivity {

    private EditText etGoal, etHabitName;
    private Spinner spPeriod, spType;
    private Button btnCreate;
    private SharedPreferences prefs;
    private List<Habit> tempHabits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);

        etGoal = findViewById(R.id.etGoal);
        etHabitName = findViewById(R.id.etHabitName);
        spPeriod = findViewById(R.id.spPeriod);
        spType = findViewById(R.id.spType);
        btnCreate = findViewById(R.id.btnCreate);

        prefs = getSharedPreferences("HabitusPrefs", Context.MODE_PRIVATE);

        // Cargar opciones
        ArrayAdapter<String> adapterPeriod = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"7 days", "14 days", "1 month", "3 months"});
        adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPeriod.setAdapter(adapterPeriod);

        ArrayAdapter<String> adapterType = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"Everyday", "3 times/week", "Weekend only"});
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapterType);

        btnCreate.setOnClickListener(v -> saveHabit());
    }

    private void saveHabit() {
        String goal = etGoal.getText().toString().trim();
        String name = etHabitName.getText().toString().trim();
        String period = spPeriod.getSelectedItem().toString();
        String type = spType.getSelectedItem().toString();

        if (goal.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Habit newHabit = new Habit(name, goal, period, type, false);
        tempHabits.add(newHabit);
        saveHabitsToPrefs(tempHabits);

        Toast.makeText(this, "Habit created successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveHabitsToPrefs(List<Habit> habits) {
        JSONArray array = new JSONArray();
        for (Habit h : habits) {
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
}
