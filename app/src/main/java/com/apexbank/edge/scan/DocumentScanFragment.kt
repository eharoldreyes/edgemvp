package com.apexbank.edge.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.apexbank.edge.R
import com.apexbank.edge.views.Corners
import com.github.florent37.runtimepermission.RuntimePermission.askPermission
import kotlinx.android.synthetic.main.fragment_document_scan.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.OpenCVLoader.initAsync

class DocumentScanFragment : Fragment(),
	DocumentScanContract.View {

	private lateinit var presenter: DocumentScanPresenter
	private var previewRatio: Float = 0f

	override fun showLoadingIndicator(visible: Boolean) {
	}

	override fun showError(message: String) {
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
		inflater.inflate(R.layout.fragment_document_scan, container, false)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		presenter = DocumentScanPresenter(this)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)
		initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, activity, object : BaseLoaderCallback(activity) {
			override fun onManagerConnected(status: Int) {
				when (status) {
					LoaderCallbackInterface.SUCCESS -> {
						loadCamera()
					}
					else -> {
						super.onManagerConnected(status)
					}
				}
			}
		})
	}

	override fun onStart() {
		super.onStart()
		presenter.start()
	}

	private fun loadCamera() {
		askPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA).onAccepted {
			try {
				val camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
				presenter.openCamera(camera)
			} catch (e: RuntimeException) {
				showError("Camera failed: " + e.message)
			}
		}.onDenied { e ->
			if (e.hasDenied()){
				context?.let {
					AlertDialog.Builder(it)
						.setMessage("Please accept our permissions")
						.setPositiveButton("yes") { dialog, which ->
							e.askAgain()
						}
						.setNegativeButton("no") { dialog, which ->
							dialog.dismiss()
						}
						.show()
				}
			}

			if (e.hasForeverDenied()) {
				e.goToSettings()
			}
		}
	}

	override fun hasCameraAutoFocus(): Boolean =
		context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS) ?: false

	override fun setSurfaceViewParamsLayout(size: Camera.Size?) {
		activity?.windowManager?.defaultDisplay?.let { display ->
			val point = Point()
			display.getRealSize(point)

			val displayWidth = minOf(point.x, point.y)
			val displayHeight = maxOf(point.x, point.y)
			val displayRatio = displayWidth.div(displayHeight.toFloat())
			previewRatio = size?.height?.toFloat()?.div(size.width.toFloat()) ?: displayRatio
			if (displayRatio > previewRatio) {
				val surfaceParams = surfaceView.layoutParams
				surfaceParams.height = (displayHeight / displayRatio * previewRatio).toInt()
				surfaceView.layoutParams = surfaceParams
			}
		}
	}

	override fun getPreviewRatio() = previewRatio

	override fun getSurfaceViewHolder(): SurfaceHolder = surfaceView.holder

	override fun setCorners(corners: Corners) {
		paperRectangle.onCornersDetected(corners)
	}

	override fun clearCorners() {
		paperRectangle.onCornersNotDetected()
	}

	override fun onStop() {
		super.onStop()
		presenter.stop()
	}

	companion object {

		const val TAG = "DocumentScanFragment"
		const val REQUEST_CAMERA_PERMISSION = 1992

	}

}