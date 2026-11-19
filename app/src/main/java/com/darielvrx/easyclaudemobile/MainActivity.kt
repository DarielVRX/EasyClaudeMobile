package com.darielvrx.easyclaudemobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

sealed class SearchState {
	object Searching : SearchState()
	object NoMatches : SearchState()
	data class HasMatches(val uris: List<Uri>) : SearchState()
}

class MainActivity : ComponentActivity() {

	private var sourceUri: Uri? = null
	private var searchState by mutableStateOf<SearchState?>(null)
	private var fileName by mutableStateOf("")

	private val createDocumentLauncher = registerForActivityResult(
		ActivityResultContracts.CreateDocument("*/*")
	) { destinationUri: Uri? ->
		if (destinationUri != null && sourceUri != null) {
			copyFile(sourceUri!!, destinationUri)
		}
	}

	private val openTreeLauncher = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree()
	) { treeUri: Uri? ->
		// aquí vamos a recorrer el árbol, próximo paso
	}

	private fun getFileName(uri: Uri): String {
		var name = "archivo"
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			if (cursor.moveToFirst() && index >= 0) {
				name = cursor.getString(index)
			}
		}
		return name
	}

	private fun copyFile(source: Uri, destination: Uri) {
		contentResolver.openInputStream(source)?.use { input ->
			contentResolver.openOutputStream(destination)?.use { output ->
				input.copyTo(output)
			}
		}
	}

	private suspend fun searchMatches(
		treeUri: Uri,
		searchedName: String
	): List<Uri> {
		val results = mutableListOf<Uri>()
		searchInFolder(treeUri, searchedName, results)
		return results
	}

	private suspend fun searchInFolder(
		folderUri: Uri,
		searchedName: String,
		results: MutableList<Uri>
	) {
		val documentId = DocumentsContract.getTreeDocumentId(folderUri)
		val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, documentId)

		val projection = arrayOf(
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_MIME_TYPE
		)

		contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
			val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
			val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
			val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

			while (cursor.moveToNext()) {
				coroutineContext.ensureActive()

				val childId = cursor.getString(idIndex)
				val childName = cursor.getString(nameIndex)
				val childMime = cursor.getString(mimeIndex)
				val isFolder = childMime == DocumentsContract.Document.MIME_TYPE_DIR

				val childUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, childId)

				if (childName == searchedName) {
					results.add(childUri)
				}

				if (isFolder) {
					searchInFolder(childUri, searchedName, results)
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
			if (uri != null) {
				sourceUri = uri
				if (fileName.isEmpty()) {
					fileName = getFileName(uri)
				}
				Column {
					TextField(
						value = fileName,
						onValueChange = { fileName = it }
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
