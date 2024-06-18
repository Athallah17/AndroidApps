package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder holder;
    private static final String TAG = "opencv";
    private Thread thread;
    private boolean isRunning = false;
    private String streamUrl;
    private int displayedWidth = 0;
    private int displayedHeight = 0;

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
    }

    public void startStream(String url) {
        this.streamUrl = url;
        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stopStream() {
        isRunning = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Do nothing here
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing here
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void run() {
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            URL url = new URL(streamUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            inputStream = connection.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream);
            Log.d(TAG, "Connected to stream: " + streamUrl);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            boolean startFrame = false;

            while (isRunning) {
                bytesRead = bufferedInputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }

                for (int i = 0; i < bytesRead; i++) {
                    if (buffer[i] == (byte) 0xFF && buffer[i + 1] == (byte) 0xD8) {
                        startFrame = true;
                        byteArrayOutputStream.reset();
                    }

                    if (startFrame) {
                        byteArrayOutputStream.write(buffer[i]);
                    }

                    if (buffer[i] == (byte) 0xFF && buffer[i + 1] == (byte) 0xD9) {
                        startFrame = false;
                        byte[] frameBytes = byteArrayOutputStream.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.length);
                        if (bitmap != null) {
                            Log.d(TAG, "Frame received");
                            Canvas canvas = holder.lockCanvas();
                            if (canvas != null) {
                                float canvasWidth = canvas.getWidth();
                                float canvasHeight = canvas.getHeight();

                                float videoWidth = bitmap.getWidth();
                                float videoHeight = bitmap.getHeight();

                                float scale = Math.min(canvasWidth / videoWidth, canvasHeight / videoHeight);

                                displayedWidth = (int) (videoWidth * scale);
                                displayedHeight = (int) (videoHeight * scale);

                                float offsetX = (canvasWidth - displayedWidth) / 2;
                                float offsetY = (canvasHeight - displayedHeight) / 2;

                                Matrix matrix = new Matrix();
                                matrix.postScale(scale, scale);
                                matrix.postTranslate(offsetX, offsetY);

                                canvas.drawBitmap(bitmap, matrix, null);

                                // Draw the grid
                                drawGrid(canvas, offsetX, offsetY, displayedWidth, displayedHeight);

                                holder.unlockCanvasAndPost(canvas);
                            }
                        } else {
                            Log.e(TAG, "Failed to decode frame");
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in run: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    Log.d(TAG, "Input stream closed");
                } catch (IOException e) {
                    Log.e(TAG, "IOException while closing input stream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void drawGrid(Canvas canvas, float offsetX, float offsetY, int width, int height) {
        Paint paint = new Paint();
        paint.setColor(0xFFFFFFFF); // White color for grid lines
        paint.setStrokeWidth(1);

        int numColumns = 5; // Number of vertical lines
        int numRows = 5; // Number of horizontal lines

        float colWidth = width / (float) numColumns;
        float rowHeight = height / (float) numRows;

        for (int i = 1; i < numColumns; i++) {
            float x = offsetX + i * colWidth;
            canvas.drawLine(x, offsetY, x, offsetY + height, paint);
        }

        for (int i = 1; i < numRows; i++) {
            float y = offsetY + i * rowHeight;
            canvas.drawLine(offsetX, y, offsetX + width, y, paint);
        }
    }
}
