package com.darielvrx.easyclaudemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import android.content.Intent
import android.net.Uri

class MainActivity : ComponentActivity() {
	
	private val createDocumentLauncher = registerForActivityResult(
		ActivityResultContracts.CreateDocument("*/*")
	) { destinationUri: Uri? ->
		// aquí vamos a copiar el archivo, en el siguiente paso
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
			val mensaje = if (uri != null) {
				"Archivo recibido: $uri"
			} else {
				"Hola, EasyClaude"
			}
			if (uri != null) {
				createDocumentLauncher.launch("archivo_prueba.txt")
			}
			Text(text = mensaje)
		}
	}
}