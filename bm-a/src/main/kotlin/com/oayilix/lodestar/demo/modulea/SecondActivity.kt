package com.oayilix.lodestar.demo.modulea

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oayilix.lodestar.annotations.Destination

@Destination(url = "lodestar://example.com/app/second", description = "second page")
class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
    }
}
