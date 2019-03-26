package com.apexbank.edge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.apexbank.edge.scan.DocumentScanFragment

class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		supportFragmentManager
			.beginTransaction()
			.add(DocumentScanFragment(), DocumentScanFragment.TAG)
			.commitNow()

	}
}
