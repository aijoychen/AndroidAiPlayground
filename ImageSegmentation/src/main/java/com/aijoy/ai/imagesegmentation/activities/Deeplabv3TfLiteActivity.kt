package com.aijoy.ai.imagesegmentation.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.aijoy.ai.imagesegmentation.R
import com.aijoy.ai.imagesegmentation.tflite.Deeplabv3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Deeplabv3TfLiteActivity : TwoImagesActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val inputStream = resources.openRawResource(R.raw.portrait_man_1)
            inputStream.use {
                val bitmap =
                    withContext(Dispatchers.Default) {
                        BitmapFactory.decodeStream(inputStream)
                    }
                imageView1.setImageBitmap(bitmap)
                val result = withContext(Dispatchers.Default) {
                    Deeplabv3(applicationContext).getSegmentationResult(bitmap)
                }
                imageView2.setImageBitmap(result)
            }
        }
    }
}