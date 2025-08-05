package com.aman.facerecognitionmlapp


fun AppCompatActivity.checkForCameraPermissions(onPermissionResult: (Boolean) -> Unit) {
    val launcher = activityResultRegistry.register(
        "Result",
        ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult(it)
    }
    when {
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED -> {
            onPermissionResult(true)
        }

        shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
            showPermissionRequiredDialog(resources.getString(R.string.camera_permission_required),
                resources.getString(R.string.camera_is_required_to_click_picture),
                resources.getString(R.string.go_to_settings),
                ""
            ) {
                if (it) {
                    launcher.launch(Manifest.permission.CAMERA)
                } else {
                    onPermissionResult(false)
                }
            }
        }

        else -> {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}


private fun AppCompatActivity.showPermissionRequiredDialog(
    title: String,
    message: String,
    doneButton: String,
    cancelButton: String,
    onAction: (Boolean) -> Unit) {
    val builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    builder.create()
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(doneButton) { _,_->
        onAction(true)
    }
    builder.setNegativeButton (cancelButton) { _,_->
        onAction(false)
    }
    builder?.show()
}

fun AppCompatActivity.checkForStoragePermissions(proceed: () -> Unit) {
    val launcher = activityResultRegistry.register(
        "Result",
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            proceed()
        } else {
            toast(getString(R.string.permission_denied))
        }
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                proceed()
            }

            else -> {
                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    } else {
        proceed()
    }
}


fun AppCompatActivity.navigateToAppSettings(onResult: () -> Unit) {
    val launcher = activityResultRegistry.register(
        "Result",
        ActivityResultContracts.StartActivityForResult()
    ) {
        onResult()
    }
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }.also {
        launcher.launch(it)
    }
}