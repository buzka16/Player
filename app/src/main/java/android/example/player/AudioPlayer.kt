package android.example.player

import android.content.Context
import android.content.Intent
import android.example.player.albumview.Item
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import java.lang.Runnable


object AudioPlayer {
    var player: SimpleExoPlayer? = null

    var samples = ArrayList<Uri>()
    var albumImages: List<Item> = listOf(Item(null))
    var songInfo = arrayOf(arrayOf("Неизвестный исполнитель", "Без названия"))

    private var _currentIndex = MutableLiveData<Int?>()
    val currentIndex: LiveData<Int?>
        get() = _currentIndex
    private var _currentProgress = MutableLiveData<Long>()
    val currentProgress: LiveData<Long>
        get() = _currentProgress
    private var _currentState = MutableLiveData<Int>().apply { value = 0 }
    val currentState: LiveData<Int>
        get() = _currentState

    private var _isLoaded = MutableLiveData<Boolean?>().apply { value = null }
    val isLoaded: LiveData<Boolean?>
        get() = _isLoaded

    fun setLoadedState(state: Boolean?) {
        _isLoaded.value = state
    }

    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index
    }

    private val handler = Handler()

    suspend fun initPlayer(context: Context, intent: Intent?) {
        withContext(Dispatchers.Main) { _isLoaded.value = false }
        withContext(Dispatchers.Default) {
            samples = getSamples(context, intent?.extras?.getParcelable("uri")!!)
            loadImages(context)
        }
        withContext(Dispatchers.Main) {
            val dataSourceFactory =
                DefaultDataSourceFactory(context, Util.getUserAgent(context, "Player"))
            val concatenatingMediaSource = ConcatenatingMediaSource()
            for (sample in samples) {
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(sample)
                concatenatingMediaSource.addMediaSource(mediaSource)
            }
            player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())

            player?.prepare(concatenatingMediaSource)
            player?.playWhenReady = false
            player?.addListener(object : Player.EventListener {

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    _currentState.value = player?.playbackState
                    updateProgressBar(handler)
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    _currentIndex.value = player?.currentWindowIndex
                    _currentProgress.value = 0
                }
            })
            _isLoaded.value = true
        }
    }

    private suspend fun loadImages(
        context: Context
    ) {
        withContext(Dispatchers.Default) {
            val defaultIcon = BitmapFactory.decodeResource(context.resources, R.drawable.default_song_icon)
            albumImages = listOf(Item(defaultIcon))
            songInfo = arrayOf(arrayOf("Неизвестный исполнитель", "Без названия"))
            if (samples.isNotEmpty()) {
                songInfo = Array(samples.size) { arrayOf("", "") }
                albumImages = List(samples.size) { Item(null) }
                for ((index, sample) in samples.withIndex()) {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(
                        context,
                        sample
                    )
                    val byteArray = mmr.embeddedPicture
                    if (byteArray != null) {
                        val image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        albumImages[index].icon = image ?: defaultIcon
                    } else albumImages[index].icon = defaultIcon

                    val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    songInfo[index] = arrayOf(artist ?: "Неизвестный исполнитель", title ?: "Без названия")
                }

            }

        }
    }

    private fun getSamples(context: Context, dir: Uri): ArrayList<Uri> {

        val filenames = ArrayList<Uri>()

        val directory = DocumentFile.fromTreeUri(context, dir)
        val files = directory?.listFiles()

        if (files != null)
            for (file in files) {
                val fileName = file.uri
                if (fileName.toString().substringAfterLast('.').contains("mp3"))
                    filenames.add(fileName)
            }
        return filenames
    }

    private fun updateProgressBar(handler: Handler) {
        val position = (if (player == null) 0 else player?.currentPosition)
        _currentProgress.value = position?.div(1000)

        // Remove scheduled updates.
        handler.removeCallbacks(updateProgressAction)
        // Schedule an update if necessary.
        val playbackState = if (player == null) Player.STATE_IDLE else player?.playbackState
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            handler.postDelayed(updateProgressAction, 1)
        }
    }

    private val updateProgressAction = Runnable { updateProgressBar(handler) }
}