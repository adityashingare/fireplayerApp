package com.project.firemusicplayer2

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MusicActivity : AppCompatActivity() {

    private lateinit var storageReference: StorageReference
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var handler: Handler
    private var isPlaying: Boolean = false

    private lateinit var buttonPlayPause: Button
    private lateinit var buttonNext: Button
    private lateinit var buttonPrevious: Button
    private lateinit var textViewSongName: TextView

    private var songList = mutableListOf<StorageReference>()
    private var currentSongIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        storageReference = FirebaseStorage.getInstance().reference.child("music")

        buttonPlayPause = findViewById(R.id.button_play_pause)
        buttonNext = findViewById(R.id.button_next)
        buttonPrevious = findViewById(R.id.button_previous)
        seekBar = findViewById(R.id.seek_bar)
        textViewSongName = findViewById(R.id.text_view_song_name)
        handler = Handler()

        buttonPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseMusic()
            } else {
                playMusic(currentSongIndex)
            }
        }

        buttonNext.setOnClickListener {
            if (songList.isNotEmpty()) {
                currentSongIndex = (currentSongIndex + 1) % songList.size
                playMusic(currentSongIndex)
            }
        }

        buttonPrevious.setOnClickListener {
            if (songList.isNotEmpty()) {
                currentSongIndex = if (currentSongIndex - 1 < 0) songList.size - 1 else currentSongIndex - 1
                playMusic(currentSongIndex)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        fetchSongs()
    }

    private fun fetchSongs() {
        storageReference.listAll().addOnSuccessListener { listResult ->
            if (listResult.items.isNotEmpty()) {
                songList.addAll(listResult.items)
                // Auto play the first song if needed
                playMusic(currentSongIndex)
            } else {
                Toast.makeText(this, "No files found in storage", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error listing items", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMusic(index: Int) {
        if (songList.isEmpty()) return

        val songRef = songList[index]
        songRef.downloadUrl.addOnSuccessListener { uri ->
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.release()
            }

            mediaPlayer = MediaPlayer.create(this, Uri.parse(uri.toString()))
            seekBar.max = mediaPlayer.duration
            updateSeekBar()

            mediaPlayer.setOnCompletionListener {
                buttonPlayPause.text = "Play"
                isPlaying = false
                // Play the next song automatically when the current one finishes
                currentSongIndex = (currentSongIndex + 1) % songList.size
                playMusic(currentSongIndex)
            }

            textViewSongName.text = songRef.name // Update the song name
            mediaPlayer.start()
            buttonPlayPause.text = "Pause"
            isPlaying = true
        }.addOnFailureListener {
            Toast.makeText(this, "Error getting download URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseMusic() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            buttonPlayPause.text = "Play"
            isPlaying = false
        }
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    seekBar.progress = mediaPlayer.currentPosition
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
