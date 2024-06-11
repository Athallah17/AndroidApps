package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;



public class MainActivity extends AppCompatActivity {
    //opencv
    private CameraBridgeViewBase mOpenCvCameraView;
    //GraphView
    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private List<BlinkData> blinkDataList;
    private boolean isSimulating = true;
    private int totalBlinks;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int elapsedTime = 0;
    private Button cameraButton;


    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("opencv", "OpenCV is Loaded");
        } else {
            Log.d("opencv", "OpenCV failed to load");
        }
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing_page); // Change to the landing page layout

        // Find the button
        Button getStartedButton = findViewById(R.id.getstarted);

        // Set click listener for the button
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the next screen
                homepage();
            }
        });
    }

    private void homepage() {  //Dashboard Graphic Drowsy
        setContentView(R.layout.homepage);

        // Find the ImageView
        ImageView drowsyImageView = findViewById(R.id.drowsy);

        // Set click listener for the ImageView
        drowsyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimulation(); // Panggil metode startSimulation saat ImageView diklik
            }
        });

        // Find the GraphView
        graph = findViewById(R.id.graph);

        // Clear existing data from graph
        graph.removeAllSeries();

        // Button to launch CameraActivity
        Button cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the camera activity
                startCamera();
            }
        });
    }

    private void startCamera() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivity(intent);
    }
    private void startSimulation() {
        // Set isSimulating to true
        isSimulating = true;

        // Clear existing data from graph, if any
        graph.removeAllSeries();

        // Initialize data list
        blinkDataList = new ArrayList<>();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // Generate new data for the graph every second
                if (isSimulating) {
                    // Generate new dummy data (either 1 or 0)
                    int newBlinkCount = new Random().nextBoolean() ? 1 : 0;

                    // Add new data point to the list
                    blinkDataList.add(new BlinkData(elapsedTime, newBlinkCount));

                    // Remove oldest data if the total number of data points exceeds 30
                    if (blinkDataList.size() > 30) {
                        blinkDataList.remove(0); // Remove the oldest data point
                    }

                    // Plot the data on the graph
                    plotData();

                    // Update the image based on the total blinks
                    updateImageView();

                    // Increment elapsed time
                    elapsedTime++;
                } else {
                    // If not simulating, stop the handler
                    handler.removeCallbacks(this);
                }

                // Schedule the next update
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(runnable);
    }

    private void updateImageView() {
        // Calculate the total blinks in the last 30 seconds
        int totalBlinksIn30Seconds = 0;
        for (BlinkData blinkData : blinkDataList) {
            totalBlinksIn30Seconds += blinkData.getBlinkCount();
        }

        // Log the total blinks in 30 seconds
        Log.d("data", "Total blinks in 30 seconds: " + totalBlinksIn30Seconds);

        // Update the image based on the total blinks
        ImageView drowsyImageView = findViewById(R.id.drowsy);
        if (totalBlinksIn30Seconds <= 5) {
            drowsyImageView.setImageResource(R.drawable.drowsyg);
        } else if (totalBlinksIn30Seconds > 5 && totalBlinksIn30Seconds <= 10) {
            drowsyImageView.setImageResource(R.drawable.drowsyc);
        } else if (totalBlinksIn30Seconds > 10) {
            drowsyImageView.setImageResource(R.drawable.drowsyr);
        }
    }

    private void plotData() {
        // Clear existing series from the graph
        graph.removeAllSeries();

        // Calculate the start time for the 30-second window
        long startTime = Math.max(0, elapsedTime - 30); // Ensure startTime is not negative

        // Calculate the end time (current time or 30 seconds, whichever is smaller)
        long endTime = Math.min(elapsedTime, startTime + 30);

        // Create a new series for the 30-second window
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(getDataPoints(startTime, endTime));

        // Add the new series to the graph
        graph.addSeries(series);

        // Customize graph viewport, labels, etc.
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(startTime); // Set min X to start time
        graph.getViewport().setMaxX(startTime + 30); // Set max X to start time + 30 seconds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(1);
    }

    private DataPoint[] getDataPoints(long startTime, long endTime) {
        List<DataPoint> dataPoints = new ArrayList<>();

        // Iterate through blink data and add data points within the specified time window
        for (BlinkData data : blinkDataList) {
            if (data.getSecond() >= startTime && data.getSecond() <= endTime) {
                DataPoint dataPoint = new DataPoint(data.getSecond(), data.getBlinkCount());
                dataPoints.add(dataPoint);
                // Log the second and blink data for each data point
                Log.d("DataPoints", "Second: " + data.getSecond() + ", Blink count: " + data.getBlinkCount());
            }
        }

        return dataPoints.toArray(new DataPoint[0]);
    }
}