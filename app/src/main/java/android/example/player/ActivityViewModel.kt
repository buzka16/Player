package android.example.player

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ActivityViewModel : ViewModel() {


    private val _albumImage = MutableLiveData<Bitmap>()
    val albumImage: LiveData<Bitmap>
        get() = _albumImage

    private val _artist = MutableLiveData<String>()
    val artist: LiveData<String>
        get() = _artist

    private val _songName = MutableLiveData<String>()
    val songName: LiveData<String>
        get() = _songName

    private val _isPlayerVisible = MutableLiveData<Int>()
    val isPlayerVisible: LiveData<Int>
        get() = _isPlayerVisible

    private val _isMiniPlayerVisible = MutableLiveData<Boolean>().apply { value = false }
    val isMiniPlayerVisible: LiveData<Boolean>
        get() = _isMiniPlayerVisible

    fun setImage(image: Bitmap) {
        _albumImage.value = image
    }

    fun setSongName(songName: String = "Без названия") {
        _songName.value = songName
    }

    fun setArtist(artist: String = "Неизвестный исполнитель") {
        _artist.value = artist
    }

    fun setMiniPlayerVisibility(isVisible: Boolean) {
        _isMiniPlayerVisible.value = isVisible
    }

    fun setPlayerVisibility(isVisible: Int) {
        _isPlayerVisible.value = isVisible
    }
}