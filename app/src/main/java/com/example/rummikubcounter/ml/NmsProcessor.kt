package com.example.rummikubcounter.ml

import com.example.rummikubcounter.model.DetectedTile

object NmsProcessor {

    /**
     * Post-processes raw YOLO output: filters by confidence, maps to original image
     * coordinates, and applies Non-Maximum Suppression.
     *
     * @param predictions  Transposed output [8400, 18] from YoloDetector
     * @param letterboxInfo Letterbox scaling/padding info for coordinate back-mapping
     * @param origWidth    Original image width
     * @param origHeight   Original image height
     * @param confThreshold Minimum confidence to keep a detection
     * @param iouThreshold  IoU threshold for NMS
     * @return List of detected tiles, sorted left-to-right
     */
    fun postProcess(
        predictions: Array<FloatArray>,
        letterboxInfo: LetterboxInfo,
        origWidth: Int,
        origHeight: Int,
        confThreshold: Float = 0.25f,
        iouThreshold: Float = 0.45f
    ): List<DetectedTile> {
        val candidates = mutableListOf<DetectedTile>()

        for (pred in predictions) {
            // pred[0..3] = cx, cy, w, h  (relative to 640x640)
            // pred[4..17] = class confidences for 14 classes

            // Find best class
            var bestClassIdx = 0
            var bestConf = pred[4]
            for (c in 1..13) {
                if (pred[4 + c] > bestConf) {
                    bestConf = pred[4 + c]
                    bestClassIdx = c
                }
            }

            if (bestConf < confThreshold) continue

            // BBox: center → corner format
            val cx = pred[0]; val cy = pred[1]; val w = pred[2]; val h = pred[3]
            var x1 = cx - w / 2f
            var y1 = cy - h / 2f
            var x2 = cx + w / 2f
            var y2 = cy + h / 2f

            // Map back from letterboxed 640x640 to original image coordinates
            x1 = ((x1 - letterboxInfo.padX) / letterboxInfo.scale).coerceIn(0f, origWidth.toFloat())
            y1 = ((y1 - letterboxInfo.padY) / letterboxInfo.scale).coerceIn(0f, origHeight.toFloat())
            x2 = ((x2 - letterboxInfo.padX) / letterboxInfo.scale).coerceIn(0f, origWidth.toFloat())
            y2 = ((y2 - letterboxInfo.padY) / letterboxInfo.scale).coerceIn(0f, origHeight.toFloat())

            // Class index 0–12 = tile values 1–13, index 13 = Joker
            val isJoker = bestClassIdx == 13
            val number = if (isJoker) null else bestClassIdx + 1

            candidates.add(
                DetectedTile(
                    number = number,
                    confidence = bestConf,
                    isJoker = isJoker,
                    x = x1.toInt(),
                    y = y1.toInt(),
                    width = (x2 - x1).toInt(),
                    height = (y2 - y1).toInt()
                )
            )
        }

        return nms(candidates, iouThreshold)
    }

    /**
     * Non-Maximum Suppression: greedily removes overlapping boxes.
     */
    private fun nms(boxes: List<DetectedTile>, iouThreshold: Float): List<DetectedTile> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<DetectedTile>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeFirst()
            result.add(best)
            sorted.removeAll { iou(best, it) > iouThreshold }
        }

        return result.sortedBy { it.x }
    }

    private fun iou(a: DetectedTile, b: DetectedTile): Float {
        val x1 = maxOf(a.x, b.x)
        val y1 = maxOf(a.y, b.y)
        val x2 = minOf(a.x + a.width, b.x + b.width)
        val y2 = minOf(a.y + a.height, b.y + b.height)

        val intersection = maxOf(0, x2 - x1) * maxOf(0, y2 - y1)
        val areaA = a.width * a.height
        val areaB = b.width * b.height
        val union = areaA + areaB - intersection

        return if (union > 0) intersection.toFloat() / union else 0f
    }
}
