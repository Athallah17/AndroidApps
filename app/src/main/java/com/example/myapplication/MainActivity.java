package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.JavaCameraView;
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

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    // OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;
    // GraphView
    private GraphView graph;
//    private LineGraphSeries<DataPoint> series;
    private List<DataPoint> blinkDataPoints = new ArrayList<>();
    private List<DataPoint> blinkRateDataPoints = new ArrayList<>();
    private List<BlinkData> blinkDataList;
    private boolean isSimulating = true;
    private int totalBlinks;
    private Handler handler = new Handler();
    private Handler labelHandler = new Handler();
    private Runnable labelRunnable;
    private int elapsedTime = 0;
    private static final int CAMERA_REQUEST_CODE = 1;
    private Button cameraButton;

    private int frameCounter = 0;
    private boolean tutupDetected = false;
    private int consecutiveBukaCount = 0;
    private int microCount =0;
    private int blink = 0;
    private List<Long> blinkTimestamps = new ArrayList<>();
    private long startTime;
    private String currentLabel = "buka"; // Default label
    private String drowsinessStatus;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d("opencv", "OpenCV is Loaded");
        } else {
            Log.d("opencv", "OpenCV failed to load");
        }
    }

    @Override
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
                startSimulation(); // Call startSimulation method when ImageView is clicked
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

        // Initialize OpenCV camera view
        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(View.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView(); // Enable the camera view immediately
    }

    private void startCamera() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d("opencv", "Camera activity finished successfully");
            mOpenCvCameraView.enableView();
        } else {
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSimulation() {
        stopSimulation();
        // Set isSimulating to true
        isSimulating = true;

        // Clear existing data from graph, if any
        graph.removeAllSeries();

        // Initialize data list
        blinkDataList = new ArrayList<>();

        startTime = System.currentTimeMillis();

        handler = new Handler();
        labelRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSimulating) {
                    currentLabel = autoGenerateLabel();
                    processImage(); // Process the image with the latest label
                    labelHandler.postDelayed(this, 50); // Generate a new label every 50ms
                } else {
                    labelHandler.removeCallbacks(this);
                }
            }
        };
        labelHandler.post(labelRunnable);
    }

    private void processImage() {
        // Auto-generate labels
        String bestLabel = currentLabel;
        elapsedTime++;
        frameCounter++;
        Log.d("opencv", "Generated label: " + bestLabel);

        // Blink detection logic
        if (bestLabel.equals("tutup")) {
            tutupDetected = true;
            consecutiveBukaCount = 0;
            microCount ++;

            Log.d("openCV", "microCount :" + microCount);
        } else if (tutupDetected && bestLabel.equals("buka")) {
            microCount = 0;
            consecutiveBukaCount++;
            if (consecutiveBukaCount >= 5) {
                blink++;
                tutupDetected = false;
                consecutiveBukaCount = 0;
                // Add timestamp for the blink
                blinkTimestamps.add(System.currentTimeMillis());
                Log.d("opencv", "Blink detected. Total blinks: " + blink);

                addBlinkDataPoint(elapsedTime);

            }
        } else {
            tutupDetected = false;
            consecutiveBukaCount = 0;
        }

        // Calculate blink rate for the last minute
        long currentTime = System.currentTimeMillis();
        // Remove timestamps older than 60 seconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            blinkTimestamps.removeIf(timestamp -> currentTime - timestamp > 60000);
        }
        int blinkRate = blinkTimestamps.size();

        blinkRateDataPoints.add(new DataPoint(elapsedTime, blinkRate));

        // Check for drowsiness

        if ((currentTime - startTime) < 60000) {
            drowsinessStatus = "Calculating";
        } else if (blinkRate >= 10 && blinkRate <= 15) {
            drowsinessStatus = "Normal";
        } else if (microCount > 40){
            drowsinessStatus = "MicroSleep";
        }else {
            drowsinessStatus = "Drowsy";
        }

        Log.d("opencv", "Blink Rate: " + blinkRate + "/min, Drowsiness: " + drowsinessStatus);

        runOnUiThread(() -> {
            TextView blinkTextView = findViewById(R.id.blinktext); // Assuming you have a TextView with this ID
            blinkTextView.setText(String.format("Blink: %d\nBlink Rate: %d/min\nDrowsiness: %s", blink, blinkRate, drowsinessStatus));
        });

        // Plot the data on the graph
        plotData();
        // Update the image based on the total blinks
        updateImageView(drowsinessStatus);
    }

    private String autoGenerateLabel() {
        int framesPerSecond = 20; // Number of frames generated per second (50ms interval)
        int framesPerBlinkCycle = 4 * framesPerSecond; // Frames needed to achieve one blink every 4 seconds

        int cycleLength = framesPerBlinkCycle; // Total length of one cycle
        int frameInCycle = frameCounter % cycleLength;

        // Simulate blink by setting the first frame to 'tutup' and the next 5 frames to 'buka'
        if (frameInCycle == 0) {
            return "tutup";
        } else if (frameInCycle > 0 && frameInCycle <= 5) {
            return "buka";
        } else {
            return "buka"; // Keep the eye open for the rest of the cycle
        }
    }

    private void stopSimulation() {
        isSimulating = false;
        handler.removeCallbacksAndMessages(null);
        labelHandler.removeCallbacksAndMessages(null);
        if (blinkDataList != null) {
            blinkDataList.clear();
        }
        if (blinkTimestamps != null) {
            blinkTimestamps.clear();
        }
        frameCounter = 0;
        consecutiveBukaCount = 0;
        blink = 0;
        elapsedTime = 0;
        Log.d("opencv", "Simulation stopped and data reset.");
    }

    private void updateImageView(String drowsinessStatus) {

        // Update the image based on the drowsiness status
        ImageView drowsyImageView = findViewById(R.id.drowsy);
        if ("Normal".equals(drowsinessStatus)) {
            drowsyImageView.setImageResource(R.drawable.drowsyg);
        } else if ("Drowsy".equals(drowsinessStatus)) {
            drowsyImageView.setImageResource(R.drawable.drowsyc);
        } else if("MicroSleep".equals(drowsinessStatus)){ // Calculating or any other status
            drowsyImageView.setImageResource(R.drawable.drowsyr);
        }
    }

    private void addBlinkDataPoint(int time) {
        blinkDataPoints.add(new DataPoint(time, 1));
        blinkDataPoints.add(new DataPoint(time + 1, 0)); // Immediately down
        plotData();
    }

    private void plotData() {
        // Clear existing series from the graph
        graph.removeAllSeries();

        // Ensure data points are in ascending order and fill gaps with zeroes
//        List<DataPoint> sortedBlinkDataPoints = new ArrayList<>();
        List<DataPoint> sortedBlinkRateDataPoints = new ArrayList<>();
        for (int i = 0; i <= elapsedTime; i++) {
//            sortedBlinkDataPoints.add(new DataPoint(i, 0)); // Fill with zeroes by default
            sortedBlinkRateDataPoints.add(new DataPoint(i, 0)); // Fill with zeroes by default
        }

//        for (DataPoint dp : blinkDataPoints) {
//            int index = (int) dp.getX();
//            if (index >= 0 && index < sortedBlinkDataPoints.size()) {
//                sortedBlinkDataPoints.set(index, dp);
//            }
//        }

        for (DataPoint dp : blinkRateDataPoints) {
            int index = (int) dp.getX();
            if (index >= 0 && index < sortedBlinkRateDataPoints.size()) {
                sortedBlinkRateDataPoints.set(index, dp);
            }
        }
        // Create new series with blink data points and blink rate data points
//        LineGraphSeries<DataPoint> blinkSeries = new LineGraphSeries<>(sortedBlinkDataPoints.toArray(new DataPoint[0]));
        LineGraphSeries<DataPoint> blinkRateSeries = new LineGraphSeries<>(sortedBlinkRateDataPoints.toArray(new DataPoint[0]));

        // Add the new series to the graph
//        graph.addSeries(blinkSeries);
        graph.addSeries(blinkRateSeries);

        // Customize graph viewport, labels, etc.
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0); // Start from the beginning
        graph.getViewport().setMaxX(elapsedTime); // Show up to the current time
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(20); // Adjust the max Y value as needed to accommodate blink rate
    }


    // 60 Detik Terakhir
//    private void plotData() {
//        // Clear existing series from the graph
//        graph.removeAllSeries();
//
//        // Ensure data points are in ascending order and fill gaps with zeroes
//        List<DataPoint> sortedDataPoints = new ArrayList<>();
//        int lastTime = elapsedTime > 60 ? elapsedTime - 60 : 0;
//
//        for (int i = lastTime; i <= elapsedTime; i++) {
//            sortedDataPoints.add(new DataPoint(i, 0)); // Fill with zeroes by default
//        }
//
//        for (DataPoint dp : blinkDataPoints) {
//            int index = (int) dp.getX() - lastTime;
//            if (index >= 0 && index < sortedDataPoints.size()) {
//                sortedDataPoints.set(index, dp);
//            }
//        }
//
//        // Create a new series with blink data points
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(sortedDataPoints.toArray(new DataPoint[0]));
//
//        // Add the new series to the graph
//        graph.addSeries(series);
//
//        // Customize graph viewport, labels, etc.
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(Math.max(0, elapsedTime - 60)); // Show the last 30 seconds
//        graph.getViewport().setMaxX(elapsedTime); // Show up to the current time
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(0);
//        graph.getViewport().setMaxY(1);
//    }


//    //FullTime
//    private void plotData() {
//        // Clear existing series from the graph
//        graph.removeAllSeries();
//
//        // Ensure data points are in ascending order and fill gaps with zeroes
//        List<DataPoint> sortedDataPoints = new ArrayList<>();
//        for (int i = 0; i <= elapsedTime; i++) {
//            sortedDataPoints.add(new DataPoint(i, 0)); // Fill with zeroes by default
//        }
//
//        for (DataPoint dp : blinkDataPoints) {
//            int index = (int) dp.getX();
//            if (index >= 0 && index < sortedDataPoints.size()) {
//                sortedDataPoints.set(index, dp);
//            }
//        }
//
//        // Create a new series with blink data points
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(sortedDataPoints.toArray(new DataPoint[0]));
//
//        // Add the new series to the graph
//        graph.addSeries(series);
//
//        // Customize graph viewport, labels, etc.
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(0); // Start from the beginning
//        graph.getViewport().setMaxX(elapsedTime); // Show up to the current time
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(0);
//        graph.getViewport().setMaxY(1);
//    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // Optionally implement this method if you need to do something when the camera view starts
    }

    @Override
    public void onCameraViewStopped() {
        // Optionally implement this method if you need to do something when the camera view stops
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
}
