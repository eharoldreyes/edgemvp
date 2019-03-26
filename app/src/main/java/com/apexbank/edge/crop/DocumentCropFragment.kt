package com.apexbank.edge.crop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.apexbank.edge.R

class DocumentCropFragment : Fragment(), DocumentCropContract.View {

    override fun showLoadingIndicator(visible: Boolean) {
    }

    override fun showError(message: String) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_document_crop, container, false)

}