package com.aijoy.ai.imagesegmentation.activities

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.aijoy.ai.imagesegmentation.R

open class TwoImagesActivity : AppCompatActivity() {
    val imageView1: ImageView by lazy { findViewById(R.id.image_1) }
    val imageView2: ImageView by lazy { findViewById(R.id.image_2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_images)
    }
}