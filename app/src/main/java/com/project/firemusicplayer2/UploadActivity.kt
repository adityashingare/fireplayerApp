package com.project.firemusicplayer2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class UploadActivity : AppCompatActivity() {

    private lateinit var buttonChoose: Button
    private lateinit var buttonUpload: Button
    private lateinit var textViewFileName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewProgress: TextView
    private lateinit var filePath: Uri
    private lateinit var storageReference: StorageReference
    private val PICK_FILE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        buttonChoose = findViewById(R.id.button_choose_file)
        buttonUpload = findViewById(R.id.button_upload)
        textViewFileName = findViewById(R.id.text_view_file)
        progressBar = findViewById(R.id.progress_bar)
        textViewProgress = findViewById(R.id.text_view_progress)
        storageReference = FirebaseStorage.getInstance().reference.child("music")

        buttonChoose.setOnClickListener { chooseFile() }
        buttonUpload.setOnClickListener { uploadFile() }
    }

    private fun chooseFile() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data!!
            textViewFileName.text = "Selected File: ${getFileName(filePath)}"
        }
    }

    private fun uploadFile() {
        if (::filePath.isInitialized) {
            val fileName = getFileName(filePath)
            val ref = storageReference.child(fileName)
            val uploadTask = ref.putFile(filePath)
            uploadTask.addOnSuccessListener {
                Toast.makeText(this, "File Uploaded", Toast.LENGTH_SHORT).show()
                progressBar.progress = 0
                progressBar.visibility = ProgressBar.GONE
                textViewProgress.text = ""
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed: " + e.message, Toast.LENGTH_SHORT).show()
                progressBar.progress = 0
                progressBar.visibility = ProgressBar.GONE
                textViewProgress.text = ""
            }.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressBar.progress = progress
                progressBar.visibility = ProgressBar.VISIBLE
                textViewProgress.text = "$progress% uploaded"
            }
        } else {
            Toast.makeText(this, "No file chosen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown"
    }
}
