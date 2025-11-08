package com.gokadzev.musify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews

class MusifyWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "MusifyWidgetProvider"
        private const val PREFS_NAME = "MusifyWidget"
        private const val PREF_TITLE = "title"
        private const val PREF_ARTIST = "artist"
        private const val PREF_IS_PLAYING = "isPlaying"
        private const val PREF_ALBUM_PATH = "albumPath"

        // widget button actions
        const val ACTION_TOGGLE_PLAY = "com.gokadzev.musify.ACTION_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.gokadzev.musify.ACTION_NEXT"
        const val ACTION_PREV = "com.gokadzev.musify.ACTION_PREV"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widget(s)")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // CHANGE THIS FROM private TO internal
    internal fun updateWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Log all available keys in SharedPreferences
        val allPrefs = prefs.all
        Log.d(TAG, "📋 All SharedPreferences keys: ${allPrefs.keys}")
        
        val title = prefs.getString(PREF_TITLE, context.getString(R.string.app_name)) ?: context.getString(R.string.app_name)
        val artist = prefs.getString(PREF_ARTIST, "") ?: ""
        val isPlaying = prefs.getBoolean(PREF_IS_PLAYING, false)
        val albumPath = prefs.getString(PREF_ALBUM_PATH, null)

        Log.d(TAG, "🔄 updateWidget called for ID $appWidgetId")
        Log.d(TAG, "📖 Reading data - Title: '$title', Artist: '$artist', Playing: $isPlaying, AlbumPath: $albumPath")

        val views = RemoteViews(context.packageName, R.layout.widget_musify)
        
        // Set the text
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, if (artist.isEmpty()) "No song playing" else artist)
        
        Log.d(TAG, "📝 Set title: '$title', artist: '$artist'")

        // set play/pause icon
        val playRes = if (isPlaying) {
            getDrawableRes(context, "ic_pause") ?: android.R.drawable.ic_media_pause
        } else {
            getDrawableRes(context, "ic_play_arrow") ?: android.R.drawable.ic_media_play
        }
        views.setImageViewResource(R.id.widget_play_pause, playRes)
        Log.d(TAG, "🎵 Set play/pause icon: ${if (isPlaying) "pause" else "play"}")

        // Album art loading with better error handling
    var loaded = false
    if (!albumPath.isNullOrEmpty()) {
        try {
            Log.d(TAG, "🖼️ Trying to load album art from: $albumPath")
            
            // For local files, load directly
            if (albumPath.startsWith("file://") || albumPath.startsWith("/")) {
                val bitmap = if (albumPath.startsWith("file://")) {
                    val file = java.io.File(albumPath.removePrefix("file://"))
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                } else {
                    android.graphics.BitmapFactory.decodeFile(albumPath)
                }
                
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_album, bitmap)
                    loaded = true
                    Log.d(TAG, "✅ Local album art loaded successfully")
                }
            } else {
                // For network URLs, load asynchronously
                val bitmap = loadBitmapFromUrl(context, albumPath)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_album, bitmap)
                    loaded = true
                    Log.d(TAG, "✅ Network album art loaded successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load album art from: $albumPath", e)
        }
    }
    
    if (!loaded) {
        Log.d(TAG, "🖼️ Using default app icon for album art")
        views.setImageViewResource(R.id.widget_album, context.applicationInfo.icon)
    }

        // PendingIntents for buttons - point to WidgetActionReceiver
        setupButtonActions(context, views)

        // Update the widget
        mgr.updateAppWidget(appWidgetId, views)
        Log.d(TAG, "✅ Widget $appWidgetId updated successfully")
    }

    private val imageCache = mutableMapOf<String, android.graphics.Bitmap>()
private fun loadBitmapFromUrl(context: Context, url: String): android.graphics.Bitmap? {
    // Check cache first
    imageCache[url]?.let { return it }
    
    return try {
        val task = object : android.os.AsyncTask<String, Void, android.graphics.Bitmap?>() {
            override fun doInBackground(vararg params: String): android.graphics.Bitmap? {
                return try {
                    val imageUrl = params[0]
                    val connection = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.connect()
                    
                    if (connection.responseCode == 200) {
                        val input = connection.inputStream
                        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                        input.close()
                        
                        // Cache the successful result
                        bitmap?.let { imageCache[url] = it }
                        bitmap
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null // Silent fail for network errors
                }
            }
        }
        
        val result = task.execute(url).get(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
        result
    } catch (e: Exception) {
        null // Silent fail for timeouts
    }
}

    private fun setupButtonActions(context: Context, views: RemoteViews) {
        // Toggle play/pause
        val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_TOGGLE_PLAY
        }
        val togglePending = PendingIntent.getBroadcast(
            context, 
            1, 
            toggleIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_play_pause, togglePending)

        // Next
        val nextIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPending = PendingIntent.getBroadcast(
            context, 
            2, 
            nextIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next, nextPending)

        // Previous
        val prevIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_PREV
        }
        val prevPending = PendingIntent.getBroadcast(
            context, 
            3, 
            prevIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_prev, prevPending)

        // Root click opens app
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val launchPending = PendingIntent.getActivity(
            context, 
            0, 
            launch, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, launchPending)
    }

    private fun getDrawableRes(context: Context, name: String): Int? {
        val res = context.resources
        val id = res.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else null
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Only handle app widget updates, not button clicks
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                Log.d(TAG, "Widget update received")
            }
            else -> {
                Log.d(TAG, "Other action received: ${intent.action} - ignoring")
            }
        }
    }

    // Add this method to force widget updates from other classes
    fun forceWidgetUpdate(context: Context) {
        Log.d(TAG, "🚨 FORCE WIDGET UPDATE CALLED")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MusifyWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        Log.d(TAG, "🚨 Force updating ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
}