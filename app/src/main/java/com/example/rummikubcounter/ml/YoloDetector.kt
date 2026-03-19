package com.example.rummikubcounter.ml

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class YoloDetector(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("rummikub_yolo.onnx").readBytes()
        val options = OrtSession.SessionOptions()
        session = env.createSession(modelBytes, options)
    }

    /**
     * Runs YOLO inference on a preprocessed input tensor.
     *
     * @param inputArray Float array of shape [1, 3, 640, 640] in CHW format, normalized to 0..1
     * @return Transposed output: Array of [8400] predictions, each with 18 values
     *         (cx, cy, w, h, 14 class confidences)
     */
    fun detect(inputArray: FloatArray): Array<FloatArray> {
        val shape = longArrayOf(1, 3, 640, 640)
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputArray), shape)

        val results = session.run(mapOf("images" to tensor))

        // Output shape: [1, 18, 8400]
        @Suppress("UNCHECKED_CAST")
        val rawOutput = (results[0].value as Array<Array<FloatArray>>)[0] // [18, 8400]

        tensor.close()
        results.close()

        // Transpose: [18, 8400] → [8400, 18]
        return transpose(rawOutput)
    }

    private fun transpose(matrix: Array<FloatArray>): Array<FloatArray> {
        val rows = matrix.size     // 18
        val cols = matrix[0].size  // 8400
        return Array(cols) { j -> FloatArray(rows) { i -> matrix[i][j] } }
    }

    fun close() {
        session.close()
        env.close()
    }
}
