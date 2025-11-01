package com.tuempresa.proyecto_01_11_25.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tuempresa.proyecto_01_11_25.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CreateHabitActivity extends AppCompatActivity {

    private EditText edtName, edtGoal, edtPeriod, edtType;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);


        edtName = findViewById(R.id.edtName);
        edtGoal = findViewById(R.id.edtGoal);
        edtPeriod = findViewById(R.id.edtPeriod);
        edtType = findViewById(R.id.edtType);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> guardarHabito());
    }

    private void guardarHabito() {
        String name = edtName.getText().toString().trim();
        String goal = edtGoal.getText().toString().trim();
        String period = edtPeriod.getText().toString().trim();
        String type = edtType.getText().toString().trim();

        if (name.isEmpty() || goal.isEmpty() || period.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("HabitusPrefs", Context.MODE_PRIVATE);
        String existing = prefs.getString("habits", null);
        JSONArray array = new JSONArray();

        try {

            if (existing != null) {
                array = new JSONArray(existing);
            }


            for (int i = 0; i < array.length(); i++) {
                JSONObject existingHabit = array.getJSONObject(i);
                if (existingHabit.getString("name").equalsIgnoreCase(name)) {
                    Toast.makeText(this, "⚠️ Ya existe un hábito con ese nombre", Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            JSONObject habitObj = new JSONObject();
            habitObj.put("name", name);
            habitObj.put("goal", goal);
            habitObj.put("period", period);
            habitObj.put("type", type);
            habitObj.put("done", false);


            array.put(habitObj);


            prefs.edit().putString("habits", array.toString()).apply();

            Toast.makeText(this, "✅ Hábito guardado correctamente", Toast.LENGTH_SHORT).show();
            finish();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Error al guardar el hábito", Toast.LENGTH_SHORT).show();
        }
    }
}
