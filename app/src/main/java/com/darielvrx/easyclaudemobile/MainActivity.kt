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
import android.os.Environment
import android.provider.Settings
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
	
	private fun tienePermisoCompleto(): Boolean {
		return Environment.isExternalStorageManager()
	}
	
	private fun solicitarPermisoCompleto() {
		val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
			data = Uri.parse("package:$packageName")
		}
		startActivity(intent)
	}
	
	private fun noVolverAPreguntar(): Boolean {
		val prefs = getSharedPreferences("easyclaude_prefs", MODE_PRIVATE)
		return prefs.getBoolean("no_preguntar_permiso", false)
	}

	private fun guardarNoVolverAPreguntar() {
		val prefs = getSharedPreferences("easyclaude_prefs", MODE_PRIVATE)
		prefs.edit().putBoolean("no_preguntar_permiso", true).apply()
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
				var mostrarAvisoPermiso by remember {
					mutableStateOf(!tienePermisoCompleto() && !noVolverAPreguntar())
				}
				if (mostrarAvisoPermiso) {
					Column {
						Text("EasyClaude puede solicitar el permiso \"Acceso a todos los archivos\" para buscar el archivo en todo el almacenamiento y encontrar coincidencias en cualquier carpeta. Sin este permiso, solo podrás buscar dentro de la carpeta que elijas manualmente cada vez.")
						Button(onClick = {
							solicitarPermisoCompleto()
							mostrarAvisoPermiso = false
						}) {
							Text("Permitir")
						}
						Button(onClick = { mostrarAvisoPermiso = false }) {
							Text("Denegar")
						}
						Button(onClick = {
							guardarNoVolverAPreguntar()
							mostrarAvisoPermiso = false
						}) {
							Text("No volver a preguntar")
						}
					}
				} else {
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
				}
			} else {
				Text(text = "Hola, EasyClaude")
			}
		}
	}
}