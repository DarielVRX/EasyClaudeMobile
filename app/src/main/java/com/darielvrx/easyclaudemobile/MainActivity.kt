package com.darielvrx.easyclaudemobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
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
	
	private val openTreeLauncher = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree()
	) { treeUri: Uri? ->
		// aquí vamos a recorrer el árbol, próximo paso
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
			if (uri != null) {
				sourceUri = uri
				var nombreArchivo by remember { mutableStateOf(obtenerNombreArchivo(uri)) }
				Column {
					TextField(
						value = nombreArchivo,
						onValueChange = { nombreArchivo = it }
					)
					Button(onClick = { openTreeLauncher.launch(null) }) {
						Text("Continuar")
					}
				}
			} else {
				Text(text = "Hola, EasyClaude")
			}
		}
	}
}