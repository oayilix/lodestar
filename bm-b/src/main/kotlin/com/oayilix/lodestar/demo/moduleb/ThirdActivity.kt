package com.oayilix.lodestar.demo.moduleb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oayilix.lodestar.annotations.Destination

@Destination(url = "router://example.com/app/third")
class ThirdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
    }
}
