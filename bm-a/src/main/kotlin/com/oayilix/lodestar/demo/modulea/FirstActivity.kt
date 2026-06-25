package com.oayilix.lodestar.demo.modulea

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oayilix.lodestar.annotations.Destination
import com.oayilix.lodestar.api.navigateTo

@Destination(url = "lodestar://example.com/app/first", description = "first page")
class FirstActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        findViewById<android.widget.Button>(R.id.button1).setOnClickListener {
            navigateTo("lodestar://example.com/app/second")
        }

        findViewById<android.widget.Button>(R.id.button2).setOnClickListener {
            navigateTo("lodestar://example.com/app/third")
        }
    }
}
