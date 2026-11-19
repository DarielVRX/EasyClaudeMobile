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
import android.provider.DocumentsContract
import kotlinx.coroutines.ensureActive
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
	
	private suspend fun buscarCoincidencias(
		treeUri: Uri,
		nombreBuscado: String
	): List<Uri> {
		val resultados = mutableListOf<Uri>()
		buscarEnCarpeta(treeUri, nombreBuscado, resultados)
		return resultados
	}

	private suspend fun buscarEnCarpeta(
		carpetaUri: Uri,
		nombreBuscado: String,
		resultados: MutableList<Uri>
	) {
		val documentId = DocumentsContract.getTreeDocumentId(carpetaUri)
		val hijosUri = DocumentsContract.buildChildDocumentsUriUsingTree(carpetaUri, documentId)

		val proyeccion = arrayOf(
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_MIME_TYPE
		)

		contentResolver.query(hijosUri, proyeccion, null, null, null)?.use { cursor ->
			val indiceId = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
			val indiceNombre = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
			val indiceMime = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

			while (cursor.moveToNext()) {
				coroutineContext.ensureActive()

				val hijoId = cursor.getString(indiceId)
				val hijoNombre = cursor.getString(indiceNombre)
				val hijoMime = cursor.getString(indiceMime)
				val esCarpeta = hijoMime == DocumentsContract.Document.MIME_TYPE_DIR

				val hijoUri = DocumentsContract.buildDocumentUriUsingTree(carpetaUri, hijoId)

				if (hijoNombre == nombreBuscado) {
					resultados.add(hijoUri)
				}

				if (esCarpeta) {
					buscarEnCarpeta(hijoUri, nombreBuscado, resultados)
				}
			}
		}
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