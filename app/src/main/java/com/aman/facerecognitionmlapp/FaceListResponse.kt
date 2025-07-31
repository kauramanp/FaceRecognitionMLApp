package com.aman.facerecognitionmlapp

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.mlkit.vision.face.Face

/**
 * @Author: Amanpreet Kaur
 * @Date: 28-07-2025 15:14
 */

@Keep
data class FaceListResponse(
    @SerializedName("facedata")
    var facedata: List<FaceData> = arrayListOf()
//    var facedata: FaceData? = null
)

@Keep
data class FaceData(
    @SerializedName("id")
    @Expose
    var id: Int? = null, // 1

    @SerializedName("user_id")
    @Expose
    var user_id: Int? = -1, // 0

    @SerializedName("face_data")
    @Expose
    var face_data: String? = null, // Security

    @SerializedName("name")
    @Expose
    var name: String? = null, // uploads/category-icon/D4ZMtD4X1724843691.png

    @SerializedName("face")
    @Expose
    var face: List<Face>? = null,

    var faceVector: FloatArray? = null // local non-serialized field (can be excluded or marked @Expose if needed)
)
