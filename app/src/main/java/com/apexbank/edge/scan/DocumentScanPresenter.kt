package com.apexbank.edge.scan

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import com.apexbank.edge.views.Corners
import com.apexbank.edge.views.processPicture
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors

class DocumentScanPresenter(
	private var view: DocumentScanContract.View?
) : DocumentScanContract.Presenter,
	SurfaceHolder.Callback,
	Camera.PreviewCallback {

	private var camera: Camera? = null
	private var busy: Boolean = false

	private val compositeDisposable: CompositeDisposable = CompositeDisposable()
	private val proxySchedule: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

	override fun start() {
		view?.getSurfaceViewHolder()?.addCallback(this)
	}

	// SurfaceHolder.Callback
	override fun surfaceCreated(holder: SurfaceHolder?) {
		camera?.let {
			openCamera(it)
		}
	}

	override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
		updateCamera()
	}

	override fun surfaceDestroyed(holder: SurfaceHolder?) {
		closeCamera()
	}

	override fun openCamera(camera: Camera) {

		this.camera = camera

		view?.let { view ->

			val param = camera.parameters
			val size = param.supportedPreviewSizes.maxBy {
				it.width
			}
			param.setPreviewSize(
				size?.width ?: 1920,
				size?.height ?: 1080
			)

			view.setSurfaceViewParamsLayout(size)

			val supportPicSize = camera.parameters.supportedPictureSizes
			supportPicSize.sortByDescending {
				it.width.times(it.height)
			}

			var pictureSize = supportPicSize.find {
				it.height.toFloat().div(it.width.toFloat()) - view.getPreviewRatio() < 0.01
			}
			if (null == pictureSize) {
				pictureSize = supportPicSize[0]
			}
			if (null == pictureSize) {
				Log.e(TAG, "Can not get picture size")
			} else {
				param.setPictureSize(pictureSize.width, pictureSize.height)
			}

			if (view.hasCameraAutoFocus()) {
				param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
			}

			param.flashMode = Camera.Parameters.FLASH_MODE_AUTO

			camera.parameters = param
			camera.setDisplayOrientation(90)
		}
	}

	override fun updateCamera() {
		camera?.apply {
			stopPreview()
			setPreviewDisplay(view?.getSurfaceViewHolder())
			setPreviewCallback(this@DocumentScanPresenter)
			startPreview()
		}
	}

	override fun closeCamera() {
		synchronized(this) {
			camera?.apply {
				stopPreview()
				setPreviewCallback(null)
				release()
			}
		}
	}

	// Camera.PreviewCallback
	override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
		if (busy) {
			return
		}
		Log.i(TAG, "On process start")
		busy = true

		compositeDisposable.add(
			Observable.create<Corners?> { emitter ->

				Log.i(TAG, "Start prepare paper")
				val parameters = camera?.parameters
				val width = parameters?.previewSize?.width
				val height = parameters?.previewSize?.height
				val yuv = YuvImage(
					data,
					parameters?.previewFormat ?: 0,
					width ?: 1080,
					height ?: 1920,
					null
				)
				val out = ByteArrayOutputStream()
				val rect = Rect(
					0,
					0,
					width ?: 1080,
					height ?: 1920
				)
				yuv.compressToJpeg(rect, 100, out)
				val bytes = out.toByteArray()
				val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

				val img = Mat()
				Utils.bitmapToMat(bitmap, img)
				bitmap.recycle()
				Core.rotate(img, img, Core.ROTATE_90_CLOCKWISE)

				try {
					out.close()
					processPicture(img)?.let { corners ->
						emitter.onNext(corners)
						emitter.onComplete()
					} ?: run {
						emitter.onError(Throwable("Picture processing failed"))
					}
				} catch (e: IOException) {
					e.printStackTrace()
					emitter.onError(e)
				}

			}.observeOn(proxySchedule)
				.subscribe({ corners ->
					corners?.let {
						view?.setCorners(it)
					}
					busy = false
				}, {
					view?.clearCorners()
					busy = false
				})
		)
	}

	override fun stop() {
		closeCamera()
		compositeDisposable.clear()
		view = null
	}

	companion object {
		const val TAG = "DocumentScanPresenter"
	}

}
