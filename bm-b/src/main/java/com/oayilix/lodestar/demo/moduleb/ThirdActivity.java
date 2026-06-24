package com.oayilix.lodestar.demo.moduleb;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.oayilix.lodestar.annotations.Destination;

@Destination(url = "router://example.com/app/third")
public class ThirdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);
    }
}