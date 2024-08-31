package com.example.myapplication;

import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private Mat ipCameraFrame;
    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private VideoCapture mVideoCapture;
    int externalCameraIndex = 0; // Assuming the index of your external camera is 0
    private static final String ipCameraUrl = "http://172.20.10.2:81/stream";
    private MjpegView mjpegView;
    private Handler mainHandler;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(    this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d("opencv", "OpenCV is Loaded");
                    //TCP Cam
                    initializeCamera();
                    // CameraExternal
//                    mOpenCvCameraView.setCameraIndex(externalCameraIndex);
//                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        // Camera External
//        mOpenCvCameraView = findViewById(R.id.camera_view);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setCvCameraViewListener(this);

        mjpegView = findViewById(R.id.camera_view);
        mjpegView.setVisibility(View.VISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initializeOpenCV();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }

        Button finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("STREAM_URL", ipCameraUrl);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }

    private void initializeOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Log.d("opencv", "OpenCV initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d("opencv", "OpenCV is not loaded. Try again.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //initializeCamera((JavaCameraView)mOpenCvCameraView, externalCameraIndex);
                initializeOpenCV();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
//    private void initializeCamera(JavaCameraView javaCameraView, int externalCameraIndex) {
//        if (OpenCVLoader.initDebug()) {
//            Log.d("opencv", "OpenCV initialization is done");
//            javaCameraView.setCameraPermissionGranted();
//            javaCameraView.setCameraIndex(externalCameraIndex);
//            javaCameraView.enableView();
//        } else {
//            Log.d("opencv", "OpenCV is not loaded. try again");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
//        }
//    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mjpegView != null) {
            mjpegView.stopStream();
        }
//        if (mOpenCvCameraView != null) {
//            mOpenCvCameraView.disableView();
//        }
//        if (mVideoCapture != null) {
//            mVideoCapture.release();
//            mVideoCapture = null;
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mjpegView != null) {
            mjpegView.stopStream();
        }

//        if (mOpenCvCameraView != null) {
//            mOpenCvCameraView.disableView();
//        }
//        if (mVideoCapture != null) {
//            mVideoCapture.release();
//        }
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        ipCameraFrame = new Mat(); // Initialize the Mat object
    }

    @Override
    public void onCameraViewStopped() {
        if (ipCameraFrame != null) {
            ipCameraFrame.release();
        }
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        synchronized (this) {
            if (ipCameraFrame != null) {
                return ipCameraFrame; // Return the IP camera frame
            } else {
                return inputFrame.rgba(); // Fallback to the default camera frame
            }
        }
    }

    private void initializeCamera() {
        try {
            Log.d(TAG, "Starting MJPEG stream from URL: " + ipCameraUrl);
            mjpegView.startStream(ipCameraUrl);
            Log.d(TAG, "MJPEG stream started successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MJPEG stream", e);
            Toast.makeText(this, "Failed to start MJPEG stream. Check URL and network.", Toast.LENGTH_LONG).show();
        }

//        new Thread(() -> {
//            mVideoCapture = new VideoCapture();
//            boolean isConnected = mVideoCapture.open(ipCameraUrl);
//
//            if (isConnected) {
//                Log.d(TAG, "Connected to IP camera: " + ipCameraUrl);
//                while (mVideoCapture != null && mVideoCapture.isOpened()) {
//                    Mat frame = new Mat();
//                    if (mVideoCapture.read(frame)) {
//                        synchronized (CameraActivity.this) {
//                            ipCameraFrame = frame;
//                        }
//                        runOnUiThread(() -> mOpenCvCameraView.disableView());
//                    }
//                }
//            } else {
//                Log.e(TAG, "Failed to connect to IP camera: " + ipCameraUrl);
//                runOnUiThread(() ->
//                        Toast.makeText(CameraActivity.this, "Failed to connect to IP camera. Check URL and network.", Toast.LENGTH_LONG).show()
//                );
//            }
//        }).start();
    }

}

