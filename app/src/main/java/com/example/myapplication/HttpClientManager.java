package com.example.myapplication;


import android.util.Log;
import okhttp3.*;
import okhttp3.MediaType;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient;

import java.io.IOException;

public class HttpClientManager {
    private static HttpClientManager instance;
    private OkHttpClient client;

    private HttpClientManager() {
        client = new OkHttpClient();
    }

    public static synchronized HttpClientManager getInstance() {
        if (instance == null) {
            instance = new HttpClientManager();
        }
        return instance;
    }

    public void postBoolean(String url, boolean value, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String json = "{\"blink_detected\":" + value + "}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }
}