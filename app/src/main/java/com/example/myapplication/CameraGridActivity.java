package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CameraGridActivity extends View {
    private Paint paint = new Paint();
    private int numLines = 20; // Jumlah garis
    private float lineSpacing = 0.1f; // Spasi antar garis (sebagai persentase dari lebar/lintang)
    private int width = 800; // Lebar tampilan
    private int height = 600; // Tinggi tampilan


    public CameraGridActivity(Context context) {
        super(context);
        this.width = width;
        this.height = height;
        init();
    }

    public CameraGridActivity(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraGridActivity(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Menghitung spasi antar garis
        float xSpacing = width * lineSpacing;
        float ySpacing = height * lineSpacing;

        // Menggambar garis vertikal
        for (int i = 1; i < numLines; i++) {
            float x = width * i / numLines + xSpacing * (i - 1);
            canvas.drawLine(x, 0, x, height, paint);
        }

        // Menggambar garis horizontal
        for (int i = 1; i < numLines; i++) {
            float y = height * i / numLines + ySpacing * (i - 1);
            canvas.drawLine(0, y, width, y, paint);
        }
    }

    // Setter untuk jumlah garis
    public void setNumLines(int numLines) {
        this.numLines = numLines;
        invalidate(); // Meminta tampilan untuk digambar ulang
    }

    // Setter untuk spasi antar garis (sebagai persentase dari lebar/lintang)
    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
        invalidate(); // Meminta tampilan untuk digambar ulang
    }
}