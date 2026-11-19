package com.darielvrx.easyclaudemobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class SearchState {
	object Searching : SearchState()
	object NoMatches : SearchState()
	data class HasMatches(val uris: List<Uri>) : SearchState()
}

class MainActivity : ComponentActivity() {

	private var sourceUri: Uri? = null
	private var searchState by mutableStateOf<SearchState?>(null)
	private var fileName by mutableStateOf("")
	private var fileNameInitialized = false
	private var lastTreeUri: Uri? = null
	private var searchJob: Job? = null
	private var pendingOverwrite by mutableStateOf<Uri?>(null)

	private val openTreeLauncher = registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree()
	) { treeUri: Uri? ->
		if (treeUri != null && sourceUri != null) {
			lastTreeUri = treeUri
			searchState = SearchState.Searching
			searchJob = lifecycleScope.launch {
				val matches = searchMatches(treeUri, fileName)
				searchState = if (matches.isEmpty()) {
					SearchState.NoMatches
				} else {
					SearchState.HasMatches(matches)
				}
			}
		}
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
	): List<Uri> = withContext(Dispatchers.IO) {
		val results = mutableListOf<Uri>()
		val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
		searchInFolder(treeUri, rootDocumentId, searchedName, results)
		results
	}

	private suspend fun searchInFolder(
		treeUri: Uri,
		folderDocumentId: String,
		searchedName: String,
		results: MutableList<Uri>,
		recursive: Boolean = true
	) {
		val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocumentId)

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

				val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)

				if (childName == searchedName) {
					results.add(childUri)
				}

				if (isFolder && recursive) {
					searchInFolder(treeUri, childId, searchedName, results, recursive)
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
				if (!fileNameInitialized) {
					fileName = getFileName(uri)
					fileNameInitialized = true
				}
				val currentState = searchState
				if (currentState != null) {
					Column {
						Text("Buscando: $fileName")

						Column(modifier = Modifier.weight(1f)) {
							when (currentState) {
								is SearchState.Searching -> Text("Buscando...")
								is SearchState.NoMatches -> Text("Sin coincidencias")
								is SearchState.HasMatches -> {
									for (matchUri in currentState.uris) {
										val path = DocumentsContract.getDocumentId(matchUri)
										Button(onClick = {
											pendingOverwrite = matchUri
										}) {
											Text(path)
										}
									}
								}
							}
						}

						Row {
							Button(onClick = {
								searchJob?.cancel()
								searchState = null
								openTreeLauncher.launch(lastTreeUri)
							}) {
								Text("Cancelar")
							}
							Button(onClick = {
								searchJob?.cancel()
								val treeUri = lastTreeUri
								if (treeUri != null) {
									lifecycleScope.launch {
										val existing = mutableListOf<Uri>()
										val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
										searchInFolder(treeUri, rootDocumentId, fileName, existing, recursive = false)
										if (existing.isNotEmpty()) {
											pendingOverwrite = existing.first()
										} else {
											val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
												treeUri,
												rootDocumentId
											)
											val newUri = DocumentsContract.createDocument(
												contentResolver,
												parentDocumentUri,
												"application/octet-stream",
												fileName
											)
											if (newUri != null && sourceUri != null) {
												copyFile(sourceUri!!, newUri)
												finish()
											}
										}
									}
								}
							}) {
								Text("Pegar aquí")
							}
						}

						val overwriteUri = pendingOverwrite
						if (overwriteUri != null) {
							AlertDialog(
								onDismissRequest = { pendingOverwrite = null },
								title = { Text("Sobrescribir archivo") },
								text = { Text("Ya existe un archivo con este nombre en esa ubicación. ¿Deseas sobrescribirlo? Esta acción no se puede deshacer.") },
								confirmButton = {
									Button(onClick = {
										copyFile(sourceUri!!, overwriteUri)
										pendingOverwrite = null
										finish()
									}) {
										Text("Sobrescribir")
									}
								},
								dismissButton = {
									Button(onClick = { pendingOverwrite = null }) {
										Text("Cancelar")
									}
								}
							)
						}
					}
				} else {
					Column(
						modifier = Modifier
							.fillMaxSize()
							.padding(24.dp),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						TextField(
							value = fileName,
							onValueChange = { fileName = it },
							textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
							colors = TextFieldDefaults.colors(
								focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
								unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
							),
							modifier = Modifier.fillMaxWidth()
						)
						Button(
							onClick = { openTreeLauncher.launch(null) },
							modifier = Modifier.padding(top = 16.dp)
						) {
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
