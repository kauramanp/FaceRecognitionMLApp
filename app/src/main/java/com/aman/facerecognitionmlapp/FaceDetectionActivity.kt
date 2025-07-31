package com.aman.facerecognitionmlapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.aman.facerecognitionmlapp.camerax.CameraManager
import com.aman.facerecognitionmlapp.databinding.ActivityFaceDetectionBinding
import com.aman.facerecognitionmlapp.databinding.LayoutFaceImageBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class FaceDetectionActivity : AppCompatActivity(){
    private lateinit var cameraManager: CameraManager

    private val TAG = FaceDetectionActivity::class.java.canonicalName

    private var faceUri = Uri.EMPTY
    private val binding : ActivityFaceDetectionBinding by lazy{
        ActivityFaceDetectionBinding.inflate(layoutInflater)
    }

    val prefs: PreferenceHelper by lazy {
        PreferenceHelper(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initCamera()
        
        binding.btnAddFace.setText(resources.getString(R.string.capture))
        binding.btnAddFace.setOnClickListener {
            getBitmapFromPreviewView(binding.previewViewFinder) { bitmap ->
                if (bitmap != null) {
                    var dialog = Dialog(this)
                    var dialogBinding = LayoutFaceImageBinding.inflate(layoutInflater)
                    dialog.setContentView(dialogBinding.root)
                    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    dialog.show()
                    dialog.setCancelable(false)
                    dialogBinding.btnAddFace.visibility = View.VISIBLE
                    dialogBinding.ivImage.setImageBitmap(bitmap)
                    dialogBinding.tvCross.setOnClickListener {
                        dialog.dismiss()
                    }
                    dialogBinding.btnAddFace.setOnClickListener {
                        createImageFile(bitmap,Calendar.getInstance().timeInMillis.toString())
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this, resources.getString(R.string.failed_to_capture_image), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initCamera(){
        cameraManager = CameraManager(
            this,
            binding.previewViewFinder,
            this,
        )
        cameraManager.startCamera()
    }

    private fun createImageFile(bitmap: Bitmap?, fileName: String) {
        val timeStamp = Calendar.getInstance().timeInMillis
        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        var imageFile = File.createTempFile(
            fileName,
            ".jpg",
            storageDir
        )
        faceUri = Uri.fromFile(imageFile)
        var outStream = FileOutputStream(imageFile)
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
        cameraManager.addNewFace(bitmap) { isSaved, faces, floatArray ->
            if (isSaved) {
                val string = floatArray.joinToString(separator = ",")
                prefs.putString(SharedConstants.KEY_FACE_ARRAY, string)
                startActivity(Intent(this, FaceRecognitionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else{
                Toast.makeText(this, resources.getString(R.string.sorry_face_is_not_detected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromPreviewView(previewView: PreviewView, onBitmapReady: (Bitmap?) -> Unit) {
        val surfaceView = previewView.getChildAt(0) as? SurfaceView
        if (surfaceView == null) {
            onBitmapReady(null)
            return
        }

        val bitmap = Bitmap.createBitmap(previewView.width, previewView.height, Bitmap.Config.ARGB_8888)
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
}