package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tuempresa.proyecto_01_11_25.R;
import com.tuempresa.proyecto_01_11_25.model.Habit;
import com.tuempresa.proyecto_01_11_25.sensors.StepSensorManager;
import com.tuempresa.proyecto_01_11_25.sensors.LightSensorManager;
import com.tuempresa.proyecto_01_11_25.sensors.GyroSensorManager;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvHabits;
    private FloatingActionButton fabAddHabit;
    private List<Habit> habitList = new ArrayList<>();
    private SharedPreferences prefs;
    private StepSensorManager stepSensor;
    private LightSensorManager lightSensor;
    private GyroSensorManager gyroSensor;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        lightSensor = new LightSensorManager(this);
        stepSensor = new StepSensorManager(this);
        gyroSensor = new GyroSensorManager(this);



        rvHabits = findViewById(R.id.rvHabits);
        fabAddHabit = findViewById(R.id.fabAddHabit);
        prefs = getSharedPreferences("HabitusPrefs", MODE_PRIVATE);

        fabAddHabit.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, CreateHabitActivity.class));
        });

        rvHabits.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        stepSensor.start();
        lightSensor.start();
        gyroSensor.start();
        loadHabits();
    }
    protected void onPause(){
        super.onPause();
        stepSensor.stop();
        lightSensor.stop();
        gyroSensor.start();
    }

    public void loadHabits() {
        habitList.clear();
        String json = prefs.getString("habits", null);

        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    habitList.add(new Habit(
                            obj.getString("name"),
                            obj.getString("goal"),
                            obj.getString("period"),
                            obj.getString("type"),
                            obj.getBoolean("done")
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        rvHabits.setAdapter(new HabitAdapter(this,habitList));
    }
}
