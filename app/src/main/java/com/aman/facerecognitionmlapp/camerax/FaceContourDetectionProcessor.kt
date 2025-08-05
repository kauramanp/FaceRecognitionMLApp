package com.aman.facerecognitionmlapp.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.util.Pair
import com.aman.facerecognitionmlapp.FaceStatus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

/**
 * @Author: Amanpreet Kaur
 * @Date: 20-03-2025 12:17
 */

class FaceContourDetectionProcessor(
    private val context: Context,
    private val interpreter: Interpreter, ) {
    var lastFace : Face?= null
    private val TAG = "FaceContourDetectionPro"
    var lastBlinkTime = System.currentTimeMillis()
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .enableTracking()
        .build()

    private val faceNetImageProcessor = ImageProcessor.Builder()
        .add(
            ResizeOp(
                FACENET_INPUT_IMAGE_SIZE,
                FACENET_INPUT_IMAGE_SIZE,
                ResizeOp.ResizeMethod.BILINEAR
            )
        )
        .add(NormalizeOp(0f, 255f))
        .build()


    private val detector = FaceDetection.getClient(realTimeOpts)

    fun addNewFace(bitmap: Bitmap, onFaceProcessed: (Boolean, MutableList<Face>, FloatArray, FaceStatus) -> Unit) {
        val inputImg = InputImage.fromBitmap(bitmap, 0)

        detector.process(inputImg).addOnSuccessListener { result ->
            Log.e(TAG, "addNewFace: result $result", )
            if (result.isNotEmpty()) {
                result.forEach {
                    var blinkDetected = false
                    var movementDetected = false
                    // Check eye blink
                    if (it.leftEyeOpenProbability != null && it.rightEyeOpenProbability != null) {
                        val leftEye = it.leftEyeOpenProbability!!
                        val rightEye = it.rightEyeOpenProbability!!

                        if (leftEye < 0.5 && rightEye < 0.5) {
                            // Eyes closed
                            lastBlinkTime = System.currentTimeMillis()
                        } else if (leftEye > 0.8 && rightEye > 0.8 && System.currentTimeMillis() - lastBlinkTime < 800) {
                            // Blink completed
                            blinkDetected = true
                            Log.d("Liveness", "Blink detected")
                        }
                    }

                    // Check head movement
                    if (lastFace != null) {
                        val move = Math.abs(lastFace!!.headEulerAngleY - it.headEulerAngleY) +
                                Math.abs(lastFace!!.headEulerAngleZ - it.headEulerAngleZ)
                        if (move > 10f) {
                            movementDetected = true
                            Log.d("Liveness", "Head movement detected")
                        }
                    }

                    // Now determine liveness
                    val isLive = blinkDetected || movementDetected
                    if(isLive) {
                        val faceBitmap = cropAndResizeFace(bitmap, it.boundingBox)
                        val tensorImg = TensorImage.fromBitmap(faceBitmap)
                        val faceOutputArray = Array(1) {
                            FloatArray(
                                192
                            )
                        }
                        val faceNetByteBuffer = faceNetImageProcessor.process(tensorImg).buffer
                        interpreter.run(faceNetByteBuffer, faceOutputArray)
                        Log.e(TAG, "faceOutputArray $faceOutputArray")
                        onFaceProcessed.invoke(true, result, faceOutputArray[0], FaceStatus.VALID)
                    } else {

                    }
                }

            } else{
                onFaceProcessed.invoke(false, mutableListOf(), floatArrayOf())
            }
        } .addOnFailureListener {
            onFaceProcessed.invoke(false, mutableListOf(), floatArrayOf())

        }
    }


    // Extract embeddings for a detected face
    private fun extractEmbeddings(bitmap: Bitmap, inputImg: InputImage, face: MutableList<Face>, onFaceProcessed: (FloatArray) -> Unit ){
        Log.e(TAG, "extractEmbeddings: ${face.isNotEmpty()}" )
        if (face.isNotEmpty()) {
            face.forEach {
                Log.e(TAG, "extractEmbeddings: $face", )
                val faceBitmap =
                    cropToBox(bitmap, it.boundingBox, inputImg.rotationDegrees)
                if(faceBitmap != null) {
                    val tensorImg = TensorImage.fromBitmap(faceBitmap)
                    val faceOutputArray = Array(1) {
                        FloatArray(
                            192
                        )
                    }
                    val faceNetByteBuffer = faceNetImageProcessor.process(tensorImg).buffer
                    interpreter.run(faceNetByteBuffer, faceOutputArray)
                    Log.e(TAG, "extractEmbeddings: ${faceOutputArray[0]}")
                    onFaceProcessed.invoke(faceOutputArray[0])
                }
            }
        }

//        return faceOutputArray[0]
    }


    private fun cropToBox(image: Bitmap, boundingBox: Rect, rotation: Int): Bitmap? {
        Log.e(TAG, "cropToBox: $rotation", )
        var image = image
        var returnBitmap : Bitmap ?= null
        val shift = 0
//        for (i in 0..<360 step 90) {
//            var rotation = i
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                image = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
            }
            returnBitmap =
                if (boundingBox.top >= 0 && boundingBox.bottom <= image.width && boundingBox.top + boundingBox.height() <= image.height && boundingBox.left >= 0 && boundingBox.left + boundingBox.width() <= image.width) {
                    Bitmap.createBitmap(
                        image,
                        boundingBox.left,
                        boundingBox.top + shift,
                        boundingBox.width(),
                        boundingBox.height()
                    )
                } else null
//        }

        return returnBitmap
    }

    // Crop and resize the face to 160x160 pixels
    private fun cropAndResizeFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        try{
            val faceBitmap = Bitmap.createBitmap(
                bitmap,
                boundingBox.left.coerceAtLeast(0),
                boundingBox.top.coerceAtLeast(0),
                boundingBox.width().coerceAtMost(bitmap.width),
                boundingBox.height().coerceAtMost(bitmap.height)
            )
            return Bitmap.createScaledBitmap(faceBitmap, 160, 160, true)
        } catch (exception: Exception){
            return bitmap
        }
    }


    fun findNearestFace(vector: FloatArray, savedFaceFloat: FloatArray): FaceStatus {
        Log.e(TAG, "findNearestFace: Input Vector: ${vector.joinToString()}")

        var nearestName: Pair<String, Float>? = null
        var nearestFaceDistance = Float.POSITIVE_INFINITY // Keep it as Float for consistency

        if (savedFaceFloat.isEmpty() == false) {
                val knownVector = savedFaceFloat ?: return FaceStatus.NO_FACE// Skip if null
//                if (knownVector.size != vector.size) continue // Ensure same dimensions

                var distance = 0.0f
                for (i in vector.indices) {
                    val diff = vector[i] - knownVector[i]
                    distance += diff * diff
                }

                val finalDistance = sqrt(distance)
                Log.e(TAG, "Comparing with : Distance = $finalDistance, Current Nearest = $nearestFaceDistance")

                if (finalDistance < nearestFaceDistance) { // Keep the best match
//                    nearestName = Pair(recognisedFaceList.name, finalDistance)
                    nearestFaceDistance = finalDistance
                }

        }

        return if (nearestFaceDistance < 0.7f) FaceStatus.RECOGNISED else FaceStatus.NOT_RECOGNISED // Adjust threshold based on testing
    }



    companion object{
        private const val FACENET_INPUT_IMAGE_SIZE = 112
    }
}