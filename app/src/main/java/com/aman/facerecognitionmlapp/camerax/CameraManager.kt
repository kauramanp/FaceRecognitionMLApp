package com.aman.facerecognitionmlapp.camerax

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aman.facerecognitionmlapp.FaceStatus
import com.google.mlkit.vision.face.Face
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

/**
 * @Author: Amanpreet Kaur
 * @Date: 20-03-2025 11:54
 */
class CameraManager(
        private val context: Context,
        private val finderView: PreviewView ?= null,
        private val lifecycleOwner: LifecycleOwner,
    ) {
    private val TAG = "CameraManager"
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var cameraSelectorOption = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null
    lateinit var faceContourDetectionProcessor: FaceContourDetectionProcessor
    private lateinit var faceNetInterpreter: Interpreter

    init {
        initialiseFaceNet()
    }

    private fun initialiseFaceNet() {
        Log.e(TAG, "createNewExecutor: " )
        try {
            Log.e(TAG, "startCamera: Step 2", )
            faceNetInterpreter = Interpreter(
                FileUtil.loadMappedFile(context, "mobile_face_net.tflite"),
                Interpreter.Options()
            )
        } catch (e: Exception) {
            Log.e(TAG, "startCamera: Step 2 Exception $e", )

            e.printStackTrace()
        }

        faceContourDetectionProcessor = FaceContourDetectionProcessor(context, faceNetInterpreter)

    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            Runnable {
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder()
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraSelectorOption)
                    .build()

                setCameraConfig(cameraProvider, cameraSelector)

            }, ContextCompat.getMainExecutor(context)
        )
    }


    private fun setCameraConfig(
        cameraProvider: ProcessCameraProvider?,
        cameraSelector: CameraSelector
    ) {
        try {
            cameraProvider?.unbindAll()
            var imageCapture = ImageCapture.Builder().build()

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                /*imageAnalyzer,*/ imageCapture
            )
            preview?.setSurfaceProvider(
                finderView?.surfaceProvider
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    fun addNewFace(bitmap: Bitmap,checkLive: Boolean = true, onFaceProcessed: (Boolean, MutableList<Face>, FloatArray, FaceStatus) -> Unit){
        if (::faceContourDetectionProcessor.isInitialized.not()) {
            faceContourDetectionProcessor = FaceContourDetectionProcessor(context, faceNetInterpreter)
        }
        faceContourDetectionProcessor.addNewFace(bitmap, { isSaved, faces, floatArray, faceStatus ->
            Log.e(TAG, "addNewFace: face saved faceContourDetectionProcessor 103 $floatArray isSaved $isSaved",)
            Log.e(TAG, "addNewFace: face saved faceContourDetectionProcessor 104 $faces",)
            onFaceProcessed.invoke(isSaved, faces, floatArray,faceStatus)
        })
    }


    fun recogniseFace(floatArray: FloatArray, savedFaces: FloatArray, function: (FaceStatus) -> Unit) {
        Log.e(TAG, "recogniseFace: floatArray $floatArray savedFaces ")
        function.invoke(faceContourDetectionProcessor.findNearestFace(floatArray, savedFaces))
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            preview = null
            camera = null
            Log.d(TAG, "Camera preview stopped and resources released.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera", e)
        }
    }

}