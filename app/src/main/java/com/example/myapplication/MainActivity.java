package com.example.myapplication;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.se.omapi.SEService;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import okhttp3.*;
import okhttp3.MediaType;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.os.Bundle;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;


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
    private static final int CAMERA_REQUEST_CODE = 1;
    private Button cameraButton;
    private MjpegView mjpegView;
    private String POST_URL = "192.168.194.39";
    private int serverPort = 1024;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private boolean isConnected = false;
    private TextView textView;
    private Handler labelHandler = new Handler();
    private int frameCounter = 0;
    private boolean tutupDetected = false;
    private int consecutiveBukaCount = 0;
    private int microCount = 0;
    private int blink = 0;
    private List<Long> blinkTimestamps = new ArrayList<>();
    private long startTime;
    private String currentLabel = "buka"; // Default label
    private String drowsinessStatus;
    private List<DataPoint> blinkRateDataPoints = new ArrayList<>();
    private List<DataPoint> blinkDataPoints = new ArrayList<>();
    private Runnable labelRunnable;
    private Handler sendDataHandler = new Handler(Looper.getMainLooper());
    private Runnable sendDataRunnable;
    private EditText ipAddressInput;
    private String ipAddress;
    private String receivedLabel = "buka";


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

        View textView = findViewById(R.id.text);

        ipAddressInput = findViewById(R.id.ipinput);
        Button connectButton = findViewById(R.id.ip_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress = ipAddressInput.getText().toString().trim();
                if (!ipAddress.isEmpty()) {
                    POST_URL = ipAddress;
                    // Start the communication task
                    new ConnectTask(new OnConnectedListener() {
                        @Override
                        public void onConnected() {
                            new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                            new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }

                        @Override
                        public void onConnectionFailed() {
                            Log.e("HTTP", "Connect failed");
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    Log.d("HTTP", "Connect Task initiated with IP: " + POST_URL);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Set click listener for the ImageView
        drowsyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the communication task
//                if (isConnected) {
//
//                    Log.d("HTTP", "Click and running send data");
//                } else {
//                    Log.e("HTTP", "Connect failed");
//                }
                startSimulation(); // Panggil metode startSimulation saat ImageView diklik
            }
        });

        // Find the GraphView
        graph = findViewById(R.id.graph);

        // Clear existing data from graph
        graph.removeAllSeries();

        // Button to launch CameraActivity
//        Button cameraButton = findViewById(R.id.camera_button);
//        cameraButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start the camera activity
//                startCamera();
//            }
//        });
        // Initialize OpenCV camera view
//        mjpegView = findViewById(R.id.camera_view);

        ImageView settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startCamera() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String streamUrl = data.getStringExtra("STREAM_URL");
                if (streamUrl != null) {
                    updateCameraView(streamUrl);
                }
            }
        } else {
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCameraView(String streamUrl) {
        mjpegView.startStream(streamUrl);
    }


    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        private OnConnectedListener onConnectedListener;

        public ConnectTask(OnConnectedListener onConnectedListener) {
            this.onConnectedListener = onConnectedListener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.d("HTTP", "Trying to connect socket");
            try {
                socket = new Socket(POST_URL, serverPort);
                Log.d("socket", "socket: " + socket);
                output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                Log.d("output", "output: " + output);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d("input", "input: " + input);
                isConnected = true;

                Log.d("HTTP", "Connected to server");

                // Continuously listen for data from the server
                new Thread(new ReceiveDataTask()).start();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("HTTP", "Error: " + e.getMessage(), e);
                return false;

            }
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                onConnectedListener.onConnected();
            } else {
                onConnectedListener.onConnectionFailed();
            }
        }
    }

    public interface OnConnectedListener {
        void onConnected();

        void onConnectionFailed();
    }

    private class ReceiveDataTask implements Runnable {
        @Override
        public void run() {
            try {
                while (isConnected) {
                    String receivedData;
                    while ((receivedData = input.readLine()) != null) {
                        try {
                            JSONObject jsonReceived = new JSONObject(receivedData);
//                            uiHandler.post(() -> textView.setText(jsonReceived.toString()))'
                            // Handle received JSON data
                            handleReceivedData(jsonReceived);
                        } catch (JSONException e) {
                            Log.e("HTTP", "JSON Error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("HTTP", "Error receiving data: " + e.getMessage(), e);
            }
        }
    }

    private void handleReceivedData(final JSONObject jsonReceived) {
        // Update UI based on received data
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Example: Update a TextView with received data
//                Log.d("HTTP", "Received: " + jsonReceived.toString());
                TextView textView = findViewById(R.id.text);
                try {
                    String label = jsonReceived.getString("label");
                    double confidence = jsonReceived.getDouble("confidence");
                    receivedLabel = label;
                    textView.setText("Label: " + label + ", Confidence: " + confidence);
                    Log.d("HTTP", "Received: " + jsonReceived.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


//        public class SendDataParams {
//        String param1;
//        int param2;
//
//        public SendDataParams(String param1, int param2) {
//            this.param1 = param1;
//            this.param2 = param2;
//        }
//    }


    private class SendDataTask extends AsyncTask<Void, Void, Void> {
        private boolean running = true; // Flag to control continuous sending

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("HTTP", "Executing SendDataTask");

            while (running) { // Continuous loop
                try {
                    // Check if the socket and output stream are initialized
                    if (socket == null || socket.isClosed() || output == null) {
                        Log.e("HTTP", "Socket or output stream is not initialized or closed. Attempting to reconnect...");
                        reconnect();
                        return null; // Exit the current send attempt
                    }

                    // Fetch the latest drowsinessStatus
                    String currentDrowsinessStatus = drowsinessStatus;

                    if (currentDrowsinessStatus != null && !currentDrowsinessStatus.isEmpty()) {
                        JSONObject jsonToSend = new JSONObject();
                        jsonToSend.put("drowsinessStatus", currentDrowsinessStatus);
                        String jsonString = jsonToSend.toString();

                        Log.d("Sent", "Trying to send: " + jsonString);

                        output.println(jsonString);
                        output.flush();

                        Log.d("Sent", "Sent: " + jsonString);
                    }

                    // Sleep for a specified interval before sending the next message
                    Thread.sleep(1000); // Adjust the interval as needed
                } catch (JSONException e) {
                    Log.e("HTTP", "JSONException: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e("HTTP", "Error sending data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return null;
        }

        public void stopSending() {
            running = false; // Method to stop continuous sending
        }

        private void reconnect() {
            new ConnectTask(new OnConnectedListener() {
                @Override
                public void onConnected() {
                    Log.d("HTTP", "Reconnected to server.");
                    new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                @Override
                public void onConnectionFailed() {
                    Log.e("HTTP", "Reconnection failed.");
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    private void startSimulation() {
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
                // Generate new data for the graph every second
                if (isSimulating) {
//                    currentLabel = autoGenerateLabel();
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
        String bestLabel = receivedLabel;
        elapsedTime++;
        frameCounter++;
        Log.d("opencv", "Using data label: " + bestLabel);

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
            drowsinessStatus = "Unhealthy";
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
        } else if ("Unhealthy".equals(drowsinessStatus)) {
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

//    private void sendBlinkData(boolean blinkDetected) {
//        HttpClientManager.getInstance().postBoolean(POST_URL, blinkDetected, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.e("HTTP", "Request failed", e);
//            }
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    String responseBody = response.body().string();
//                    Log.d("HTTP", "Request successful: " + responseBody);
//                } else {
//                    Log.e("HTTP", "Request failed with code: " + response.code());
//                }
//            }
//        });
//    }

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
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0); // Start from the beginning
        graph.getViewport().setMaxX(elapsedTime); // Show up to the current time
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(20); // Adjust the max Y value as needed to accommodate blink rate
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

    @Override
    protected void onPause() {
        super.onPause();
        if (mjpegView != null) {
            mjpegView.stopStream();
        }
    }

}
