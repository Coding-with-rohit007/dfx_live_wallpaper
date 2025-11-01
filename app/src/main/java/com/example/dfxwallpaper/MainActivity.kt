package com.example.dfxwallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val btn = Button(this).apply {
            text = "Set DFX Live Wallpaper"
            setOnClickListener {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    android.content.ComponentName(this@MainActivity, DFXWallpaperService::class.java))
                startActivity(intent)
            }
        }
        setContentView(btn)
    }
}
