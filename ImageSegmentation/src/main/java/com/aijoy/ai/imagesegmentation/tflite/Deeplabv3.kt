package com.aijoy.ai.imagesegmentation.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * deeplabv3
 * input: [{'name': 'sub_7', 'index': 183, 'shape': array([  1, 257, 257,   3], dtype=int32), 'shape_signature': array([  1, 257, 257,   3], dtype=int32), 'dtype': <class 'numpy.float32'>, 'quantization': (0.0, 0), 'quantization_parameters': {'scales': array([], dtype=float32), 'zero_points': array([], dtype=int32), 'quantized_dimension': 0}, 'sparsity_parameters': {}}]
 * output: [{'name': 'ResizeBilinear_3', 'index': 168, 'shape': array([  1, 257, 257,  21], dtype=int32), 'shape_signature': array([  1, 257, 257,  21], dtype=int32), 'dtype': <class 'numpy.float32'>, 'quantization': (0.0, 0), 'quantization_parameters': {'scales': array([], dtype=float32), 'zero_points': array([], dtype=int32), 'quantized_dimension': 0}, 'sparsity_parameters': {}}]
 */
class Deeplabv3(private val context: Context) {
    private val tflite: Interpreter

    init {
        fun loadModelFile(): ByteBuffer {
            val assetFileDescriptor = context.assets.openFd("deeplabv3.tflite")
            val inputStream = assetFileDescriptor.createInputStream()
            val modelBytes = inputStream.readBytes()
            inputStream.close()
            return ByteBuffer.allocateDirect(modelBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(modelBytes)
                rewind()
            }
        }
        tflite = Interpreter(loadModelFile())
    }

    fun getSegmentationResult(bitmap: Bitmap): Bitmap {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))  // normalization, 0~1
            .build()

        // 1: Process the image and convert it to a TensorBuffer
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val byteBuffer: ByteBuffer = processedImage.buffer

        // 2. Create output buffer
        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 21),
            DataType.FLOAT32
        )

        // 3. run
        tflite.run(byteBuffer, outputBuffer.buffer.rewind())

        val results: FloatArray = outputBuffer.floatArray

        // 4. Get the predicted category (maximum probability index)
        val predictedClasses = IntArray(INPUT_SIZE * INPUT_SIZE)
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            var maxProbability = -1f
            var classIdx = -1
            for (j in 0 until 21) {  // 假设每个像素有 21 个类别
                val probability = results[i * 21 + j]
                if (probability > maxProbability) {
                    maxProbability = probability
                    classIdx = j
                }
            }
            predictedClasses[i] = classIdx
        }
        /*val output = outputBuffer.buffer
        for (i in 0 until INPUT_SIZE * INPUT_SIZE) {
            var maxProbability = -1f
            var classIdx = -1
            for (j in 0 until 21) {  // 假设每个像素有 21 个类别
                val probability = output.getFloat((i * 21 + j) * 4)
                if (probability > maxProbability) {
                    maxProbability = probability
                    classIdx = j
                }
            }
            predictedClasses[i] = classIdx
        }*/

        // 5. resize the prediction result back to the original image size
        return resizeOutputToOriginal(predictedClasses, bitmap.width, bitmap.height)
    }

    private fun resizeOutputToOriginal(
        predictedClasses: IntArray,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap {
        val outputBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)

        // Get the segmentation color map
        val colorMap = generateColorMap(21)

        // Convert to bitmap
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val classIdx = predictedClasses[y * INPUT_SIZE + x]
                val color = colorMap[classIdx]
                outputBitmap.setPixel(x, y, color)
            }
        }
        return resizeBitmap(outputBitmap, originalWidth, originalHeight)
    }

    /**
     * Resize a bitmap, scaling the received bitmap with the new dimensions.
     *
     * @param bitmap The bitmap to resize.
     * @param newWidth The width which the resized bitmap must have.
     * @param newHeight The height which the resized bitmap must have.
     * @return The resized bitmap.
     */
    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val matrix = Matrix()
        matrix.setScale(newWidth / bitmap.width.toFloat(), newHeight / bitmap.height.toFloat())

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    private fun generateColorMap(size: Int): IntArray {
        val colorMap = IntArray(size)
        for (i in 0 until size) {
            colorMap[i] = android.graphics.Color.rgb(
                (Math.random() * 255).toInt(),
                (Math.random() * 255).toInt(),
                (Math.random() * 255).toInt()
            )
        }
        return colorMap
    }

    companion object {
        private const val INPUT_SIZE = 257
    }
}