package com.darielvrx.easyclaudemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

class MainActivity : ComponentActivity() {
	
	private var sourceUri: Uri? = null

	private val createDocumentLauncher = registerForActivityResult(
		ActivityResultContracts.CreateDocument("*/*")
	) { destinationUri: Uri? ->
		if (destinationUri != null && sourceUri != null) {
			copiarArchivo(sourceUri!!, destinationUri)
		}
	}
	
	private fun obtenerNombreArchivo(uri: Uri): String {
		var nombre = "archivo"
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			if (cursor.moveToFirst() && index >= 0) {
				nombre = cursor.getString(index)
			}
		}
		return nombre
	}

	private fun copiarArchivo(origen: Uri, destino: Uri) {
		contentResolver.openInputStream(origen)?.use { input ->
			contentResolver.openOutputStream(destino)?.use { output ->
				input.copyTo(output)
			}
		}
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
				sourceUri = uri
				createDocumentLauncher.launch(obtenerNombreArchivo(uri))
			}
			Text(text = mensaje)
		}
	}
}