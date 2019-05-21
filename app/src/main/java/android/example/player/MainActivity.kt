package android.example.player

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.example.player.albumview.AlbumAdapter
import android.example.player.albumview.PageTransformer
import android.example.player.databinding.PlayerActivityBinding
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val STORAGE_PERMISSION_CODE = 1
        const val READ_REQUEST_CODE = 2
    }

    private lateinit var viewModel: ActivityViewModel
    private lateinit var binding: PlayerActivityBinding

    private lateinit var pageAdapter: AlbumAdapter
    private val pageListener = object :
        ViewPager.OnPageChangeListener {
        var flag = false
        var pageIndex = 0
        override fun onPageScrollStateChanged(state: Int) {
            when (state) {
                ViewPager.SCROLL_STATE_IDLE -> if (flag) {
                    AudioPlayer.player?.seekTo(pageIndex, 0)
                    flag = false
                }
            }
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            pageIndex = position
            flag = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.player_activity)
        binding.lifecycleOwner = this

        binding.miniPlayerSong.isSelected = true
        binding.playerBottom.artistText.isSelected = true
        binding.playerBottom.songName.isSelected = true

        viewModel = ViewModelProviders.of(this).get(ActivityViewModel::class.java)

        AudioPlayer.currentState.observe(this, Observer
        { newState ->
            when (newState) {
                1 -> {
                    viewModel.setMiniPlayerVisibility(false)
                    BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state =
                        BottomSheetBehavior.STATE_COLLAPSED
                    AudioPlayer.setLoadedState(null)
                    if (AudioPlayer.hasException) {
                        Toast.makeText(this, "Ошибка при воспроизведении файла", Toast.LENGTH_LONG).show()
                        AudioPlayer.hasException = false
                    }
                }
            }
        })

        AudioPlayer.currentProgress.observe(this, Observer
        { newProgress ->
            var time = "${newProgress / 60}"
            time += ":"
            if (newProgress % 60 < 10)
                time += "0"
            time += "${newProgress % 60}"
            var remTime = if (AudioPlayer.player!!.duration > 0)
                AudioPlayer.player!!.duration / 1000 - newProgress
            else 0
            if (remTime < 0)
                remTime = 0
            val remTimeText =
                if (remTime % 60 >= 10) "-${remTime / 60}:${remTime % 60}" else "-${remTime / 60}:0${remTime % 60}"

            binding.playerBottom.songProgress.text = time
            binding.playerBottom.songProgressReverse.text = remTimeText
            binding.miniPlayerTime.text = remTimeText

        })

        AudioPlayer.isLoaded.observe(this, Observer
        { isLoaded ->
            if (isLoaded != null) {
                if (isLoaded != false)
                    binding.selectButtonHolder.visibility = View.VISIBLE
                else
                    binding.selectButtonHolder.visibility = View.INVISIBLE
                CoroutineScope(Dispatchers.Default).launch {
                    initPageAdapter()
                    withContext(Dispatchers.Main) {
                        if (isLoaded == true) {
                            initIcon(AudioPlayer.currentIndex.value ?: 0)
                            if (viewModel.isPlayerVisible.value != BottomSheetBehavior.STATE_EXPANDED)
                                viewModel.setMiniPlayerVisibility(true)
                            //Start service
                            Util.startForegroundService(
                                applicationContext,
                                Intent(applicationContext, PlayerService::class.java)
                            )

                            binding.miniPlayerControl.player = AudioPlayer.player
                            binding.playerBottom.mainPlayerProgress.player = AudioPlayer.player
                            binding.playerBottom.mainPlayer.player = AudioPlayer.player
                            binding.playerBottom.songProgress.text = "0:00"
                            binding.playerBottom.songProgressReverse.text = "-0:00"
                            binding.miniPlayerTime.text = "-0:00"
                        }
                    }
                }
            } else {
                binding.selectButtonHolder.visibility = View.VISIBLE
                viewModel.setMiniPlayerVisibility(false)
            }

        })


        AudioPlayer.currentIndex.observe(this, Observer
        { newIndex ->
            initIcon(newIndex ?: 0)
            binding.playerBottom.albumViewPager.clearOnPageChangeListeners()
            binding.playerBottom.albumViewPager.setCurrentItem(
                newIndex ?: 0,
                true
            )
            if (binding.playerBottom.albumViewPager.adapter != null)
                binding.playerBottom.albumViewPager.addOnPageChangeListener(pageListener)
        })



        viewModel.isMiniPlayerVisible.observe(this, Observer
        { isVisible ->
            if (isVisible)
                binding.miniPlayerToolbar.visibility = View.VISIBLE
            else
                binding.miniPlayerToolbar.visibility = View.INVISIBLE
        })

        viewModel.isPlayerVisible.observe(this, Observer { newState ->
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    if (AudioPlayer.isLoaded.value == true) {
                        binding.selectButtonHolder.visibility = View.VISIBLE
                        viewModel.setMiniPlayerVisibility(true)
                    } else {
                        if (AudioPlayer.isLoaded.value == false)
                            binding.selectButtonHolder.visibility = View.INVISIBLE
                        else binding.selectButtonHolder.visibility = View.VISIBLE
                        viewModel.setMiniPlayerVisibility(false)
                    }
                }
                BottomSheetBehavior.STATE_DRAGGING -> {
                    if (AudioPlayer.isLoaded.value == true) {
                        binding.selectButtonHolder.visibility = View.VISIBLE
                        viewModel.setMiniPlayerVisibility(true)
                    } else {
                        if (AudioPlayer.isLoaded.value == false)
                            binding.selectButtonHolder.visibility = View.INVISIBLE
                        else binding.selectButtonHolder.visibility = View.VISIBLE
                        viewModel.setMiniPlayerVisibility(false)
                    }
                }

                BottomSheetBehavior.STATE_EXPANDED -> {
                    binding.selectButtonHolder.visibility = View.INVISIBLE
                    viewModel.setMiniPlayerVisibility(false)
                }
            }
        })


        viewModel.albumImage.observe(this, Observer
        { newImage ->
            binding.albumImage.setImageBitmap(newImage)
            binding.albumImage.setImageBitmap(newImage)
        })

        viewModel.songName.observe(
            this,
            Observer
            { newSongName ->
                binding.playerBottom.songName.text = newSongName.toString()
                binding.miniPlayerSong.text = newSongName
            })

        viewModel.artist.observe(
            this,
            Observer
            { newArtist -> binding.playerBottom.artistText.text = newArtist.toString() })


        when (viewModel.isPlayerVisible.value) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state =
                    viewModel.isPlayerVisible.value!!
                binding.selectButtonHolder.visibility = View.INVISIBLE
                viewModel.setMiniPlayerVisibility(false)
            }
            BottomSheetBehavior.STATE_COLLAPSED -> {
                if (AudioPlayer.isLoaded.value == true) {
                    binding.selectButtonHolder.visibility = View.VISIBLE
                    viewModel.setMiniPlayerVisibility(true)
                } else {
                    if (AudioPlayer.isLoaded.value == false)
                        binding.selectButtonHolder.visibility = View.INVISIBLE
                    else binding.selectButtonHolder.visibility = View.VISIBLE
                    viewModel.setMiniPlayerVisibility(false)
                }
            }

        }

        when (AudioPlayer.isLoaded.value) {
            true -> {
                binding.selectButtonHolder.visibility = View.VISIBLE
            }
            false -> {
                binding.selectButtonHolder.visibility = View.INVISIBLE
                viewModel.setMiniPlayerVisibility(false)
            }
            else -> viewModel.setMiniPlayerVisibility(false)
        }



        binding.playerBottom.hidePlayerButton.setOnClickListener {
            if (viewModel.isPlayerVisible.value == BottomSheetBehavior.STATE_EXPANDED)
                BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state =
                    BottomSheetBehavior.STATE_COLLAPSED
        }


        binding.miniPlayerToolbar.setOnClickListener {
            BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state =
                BottomSheetBehavior.STATE_EXPANDED
        }

        binding.selectButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                CoroutineScope(Dispatchers.Default).launch { getFolder() }
            } else {
                requestStoragePermission()
            }
        }


        //FIX visibility of the menu
        val bottomSheetBehavior =
            BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer)
        bottomSheetBehavior.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    viewModel.setPlayerVisibility(BottomSheetBehavior.STATE_DRAGGING)

                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        binding.selectButtonHolder.visibility = View.INVISIBLE
                    }
                    viewModel.setPlayerVisibility(newState)
                }
            })

    }

    override fun onResume() {
        super.onResume()
        binding.miniPlayerControl.player = AudioPlayer.player
        binding.playerBottom.mainPlayerProgress.player = AudioPlayer.player
        binding.playerBottom.mainPlayer.player = AudioPlayer.player
    }

    override fun onStop() {
        super.onStop()
        binding.miniPlayerControl.player = null
        binding.playerBottom.mainPlayerProgress.player = null
        binding.playerBottom.mainPlayer.player = null
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Разрешение не предоставлено")
                .setMessage("Разрешите доступ к хранилищу, чтобы проигрывать музыку.")
                .setPositiveButton("ОК", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            STORAGE_PERMISSION_CODE
                        )
                    }
                })
                .setNegativeButton("Отмена", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        dialog!!.dismiss()
                    }
                }).create().show()

        } else
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Доступ предоставлен", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.Default).launch { getFolder() }
            } else
                Toast.makeText(this, "Доступ не предоставлен", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getFolder() {
        withContext(Dispatchers.Default) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            withContext(Dispatchers.Main) { startActivityForResult(intent, READ_REQUEST_CODE) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    //Prepare view
                    withContext(Dispatchers.Main) {
                        AudioPlayer.player?.release()
                        AudioPlayer.setLoadedState(false)
                        binding.selectButtonHolder.visibility = View.INVISIBLE
                        binding.miniPlayerControl.player = null
                        binding.playerBottom.mainPlayerProgress.player = null
                        binding.playerBottom.mainPlayer.player = null
                        viewModel.setMiniPlayerVisibility(false)
                        stopService(Intent(applicationContext, PlayerService::class.java))
                        Toast.makeText(this@MainActivity, "Плеер загружается...", Toast.LENGTH_LONG).show()
                    }
                    //Load Player
                    withContext(Dispatchers.Default) {
                        AudioPlayer.initPlayer(
                            applicationContext,
                            Intent(applicationContext, AudioPlayer::class.java).apply {
                                putExtra(
                                    "uri",
                                    data.data
                                )
                            })
                        initPageAdapter()
                    }
                    //Player loaded
                    withContext(Dispatchers.Main) {
                        binding.selectButtonHolder.visibility = View.VISIBLE
                        if (AudioPlayer.samples.isNotEmpty())
                            Toast.makeText(this@MainActivity, "Плеер загружен!", Toast.LENGTH_LONG).show()
                        else {
                            Toast.makeText(
                                this@MainActivity,
                                "В папке отсутствуют MP3 файлы!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private suspend fun initPageAdapter() {
        withContext(Dispatchers.Default) {
            if (AudioPlayer.player != null) {
                pageAdapter = AlbumAdapter(this@MainActivity, AudioPlayer.albumImages)
                CoroutineScope(Dispatchers.Main).launch {
                    binding.playerBottom.albumViewPager.adapter = pageAdapter
                    binding.playerBottom.albumViewPager.offscreenPageLimit = 5
                    binding.playerBottom.albumViewPager.setPageTransformer(false, PageTransformer())
                    binding.playerBottom.albumViewPager.clearOnPageChangeListeners()
                    binding.playerBottom.albumViewPager.setCurrentItem(
                        AudioPlayer.currentIndex.value ?: 0,
                        true
                    )
                    binding.playerBottom.albumViewPager.addOnPageChangeListener(pageListener)
                }
            }
        }
    }

    private fun initIcon(index: Int) {
        viewModel.setImage(
            AudioPlayer.albumImages[index].icon ?: BitmapFactory.decodeResource(
                resources,
                R.drawable.default_song_icon
            )
        )
        viewModel.setArtist(AudioPlayer.songInfo[index][0])
        viewModel.setSongName(AudioPlayer.songInfo[index][1])
    }

    override fun onBackPressed() {
        if (BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state != BottomSheetBehavior.STATE_EXPANDED)
            super.onBackPressed()
        else BottomSheetBehavior.from(binding.playerBottom.bottomSheetPlayer).state =
            BottomSheetBehavior.STATE_COLLAPSED
    }

}
