package com.aman.facerecognitionmlapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.aman.facerecognitionmlapp.camerax.CameraManager
import com.aman.facerecognitionmlapp.databinding.ActivityFaceDetectionBinding

class FaceRecognitionActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager

    private val binding: ActivityFaceDetectionBinding by lazy {
        ActivityFaceDetectionBinding.inflate(layoutInflater)
    }
    private val TAG = FaceRecognitionActivity::class.java.canonicalName
    var savedFloatArray = floatArrayOf()
    val prefs: PreferenceHelper by lazy {
        PreferenceHelper(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        var string = prefs.getString(SharedConstants.KEY_FACE_ARRAY)
        if (string.isNullOrEmpty() == false) {
            savedFloatArray = string.split(",")
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
        }
        checkForCameraPermissions{isCameraPermission->
            if(isCameraPermission){
                checkForStoragePermissions{
                    initCamera()
                }
            }
        }
        binding.btnAddFace.setText(resources.getString(R.string.add_face))

        binding.btnAddFace.setOnClickListener {
            startActivity(Intent(this, FaceDetectionActivity::class.java))
        }
    }

    private fun checkFaces(bitmap: Bitmap) {
        cameraManager.addNewFace(bitmap) { isSaved, faces, floatArray ->
            if (isSaved) {
                cameraManager.recogniseFace(floatArray, savedFloatArray, function = {
                    if (it == FaceStatus.RECOGNISED) {
                        binding.llOuterFrame.setBackgroundResource(R.drawable.bg_two_circle_green)

                        binding.ivCheck.visibility = View.VISIBLE
                        binding.mirrorCurve.pauseSweep()
                        // Bounce animation: Y translation
                        val translateAnimator =
                            ObjectAnimator.ofFloat(binding.ivCheck, "translationX", 0f, 50f, 0f)
                                .apply {
                                    duration = 1000
                                    interpolator = LinearInterpolator() // Smooth and constant
                                    repeatCount = 0 // No repeat
                                }
                        translateAnimator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                runOnUiThread {
                                    Toast.makeText(
                                        this@FaceRecognitionActivity,
                                        resources.getString(R.string.verified_user),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })

                        translateAnimator.start()

                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            capturePhoto()
                        }, 2000)

                    }
                }
                )
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    capturePhoto()
                }, 2000)
            }
        }
    }

    private fun initCamera() {
        cameraManager = CameraManager(
            this,
            binding.previewViewFinder,
            this,
        )
        cameraManager.startCamera()

        Handler(Looper.getMainLooper()).postDelayed({
            capturePhoto()
        }, 2000)
    }

    private fun capturePhoto() {
        getBitmapFromPreviewView(binding.previewViewFinder) { bitmap ->
            if (bitmap != null) {
                checkFaces(bitmap)
            } else {
//                Toast.makeText(this, resources.getString(R.string.failed_to_capture_image), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromPreviewView(previewView: PreviewView, onBitmapReady: (Bitmap?) -> Unit) {
        val surfaceView = previewView.getChildAt(0) as? SurfaceView
        if (surfaceView == null) {
            onBitmapReady(null)
            return
        }

        val bitmap =
            Bitmap.createBitmap(previewView.width, previewView.height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())

        try {
            PixelCopy.request(surfaceView, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    onBitmapReady(bitmap) // Successfully copied the preview
                } else {
                    onBitmapReady(null) // Failed to capture the preview
                }
            }, handler)
        } catch (e: Exception) {
            e.printStackTrace()
            onBitmapReady(null) // Handle errors
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stopCamera()
    }
}