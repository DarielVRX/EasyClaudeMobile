package com.darielvrx.easyclaudemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import android.content.Intent
import android.net.Uri

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
			val mensaje = if (uri != null) {
				"Archivo recibido: $uri"
			} else {
				"Hola, EasyClaude"
			}
			Text(text = mensaje)
		}
	}
}