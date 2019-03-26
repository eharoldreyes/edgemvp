package com.apexbank.edge.base

interface BaseView {

	fun showLoadingIndicator(visible: Boolean)

	fun showError(message: String)

}