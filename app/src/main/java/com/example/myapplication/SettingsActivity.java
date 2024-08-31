package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_page); // Ensure this matches your settings layout file name
        // Find the button
        ImageView backToHomeButton = findViewById(R.id.home_back);

        // Set click listener for the button
        backToHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start MainActivity
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                // Add a flag to clear the stack and start a new task
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                // Add extra to indicate that the homepage method should be called
                intent.putExtra("open_homepage", true);
                startActivity(intent);
                // Finish SettingsActivity so it's removed from the back stack
                finish();
            }
        });
    }
}