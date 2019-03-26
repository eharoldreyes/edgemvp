package com.apexbank.edge.scan

import android.hardware.Camera
import android.view.SurfaceHolder
import com.apexbank.edge.base.BasePresenter
import com.apexbank.edge.base.BaseView
import com.apexbank.edge.views.Corners

interface DocumentScanContract {

	interface View : BaseView {

		fun hasCameraAutoFocus(): Boolean

		fun setSurfaceViewParamsLayout(size: Camera.Size?)

		fun getPreviewRatio(): Float

		fun getSurfaceViewHolder(): SurfaceHolder

		fun setCorners(corners: Corners)

		fun clearCorners()

	}

	interface Presenter : BasePresenter {

		fun openCamera(camera: Camera)

		fun updateCamera()

		fun closeCamera()

	}

}