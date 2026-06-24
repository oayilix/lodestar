package com.oayilix.lodestar.demo.modulea;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.oayilix.lodestar.annotations.Destination;
import com.oayilix.lodestar.api.Router;

@Destination(url = "router://example.com/app/first", description = "first page")
public class FirstActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        findViewById(R.id.button1).setOnClickListener(v ->
                Router.navigation(FirstActivity.this, "router://example.com/app/second"));

        findViewById(R.id.button2).setOnClickListener(v ->
                Router.navigation(FirstActivity.this, "router://example.com/app/third"));
    }
}