package com.darielvrx.easyclaudemobile

import android.os.Bundle
import android.activity.ComponentActivity
import android.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			Text(text = "Hola, EasyClaude")
	}
}