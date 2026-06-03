package jr.brian.inindy.data.media

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class CameraCapture {

    actual suspend fun capturePhoto(): CameraResult {
        if (!UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            )
        ) {
            return CameraResult.Error("Camera not available")
        }

        val permission = ensureCameraPermission()
        if (permission != null) return permission

        val root = topViewController() ?: return CameraResult.Error("No root view controller")

        return suspendCancellableCoroutine { cont ->
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            val delegate = object : NSObject(),
                UIImagePickerControllerDelegateProtocol,
                UINavigationControllerDelegateProtocol {
                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    val image = didFinishPickingMediaWithInfo[
                        UIImagePickerControllerOriginalImage
                    ] as? UIImage
                    picker.dismissViewControllerAnimated(true) {
                        if (image == null) {
                            if (cont.isActive) cont.resume(CameraResult.Cancelled)
                            return@dismissViewControllerAnimated
                        }
                        val savedUri = ImageFileWriter.writeJpeg(image)
                        if (cont.isActive) {
                            cont.resume(
                                if (savedUri != null) CameraResult.Success(savedUri)
                                else CameraResult.Error("Failed to save photo")
                            )
                        }
                    }
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    picker.dismissViewControllerAnimated(true) {
                        if (cont.isActive) cont.resume(CameraResult.Cancelled)
                    }
                }
            }
            picker.delegate = delegate
            cont.invokeOnCancellation {
                picker.dismissViewControllerAnimated(true, null)
            }
            root.presentViewController(picker, animated = true, completion = null)
        }
    }

    private suspend fun ensureCameraPermission(): CameraResult? {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        return when (status) {
            AVAuthorizationStatusAuthorized -> null
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted -> CameraResult.PermissionPermanentlyDenied
            AVAuthorizationStatusNotDetermined -> {
                val granted = requestCameraAccess()
                if (granted) null else CameraResult.PermissionDenied
            }
            else -> CameraResult.PermissionDenied
        }
    }

    private suspend fun requestCameraAccess(): Boolean =
        withContext(Dispatchers.Default) {
            suspendCancellableCoroutine<Boolean> { cont ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (cont.isActive) cont.resume(granted)
                }
            }
        }

    private fun topViewController() =
        UIApplication.sharedApplication.keyWindow?.rootViewController
}
