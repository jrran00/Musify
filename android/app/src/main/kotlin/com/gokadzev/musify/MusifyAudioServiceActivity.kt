package com.gokadzev.musify

import com.ryanheise.audioservice.AudioServiceActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.util.Log
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class MusifyAudioServiceActivity : AudioServiceActivity() {
    private val CHANNEL = "com.gokadzev.musify/widget"
    private var methodChannel: MethodChannel? = null

    init {
        Log.d("DEBUG", "🔥🔥🔥 MusifyAudioServiceActivity - INIT BLOCK CALLED")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DEBUG", "🔥🔥🔥 MusifyAudioServiceActivity - onCreate CALLED")
        Log.d("DEBUG", "🔥 Intent action: ${intent?.action}")
        Log.d("DEBUG", "🔥 Intent component: ${intent?.component}")
        
        cacheFlutterEngine()
        handleWidgetAction(intent)
        
        // Test if we have FlutterEngine available immediately
        testImmediateEngineAvailability()
    }

    override fun onStart() {
        super.onStart()
        Log.d("DEBUG", "🔥 MusifyAudioServiceActivity - onStart CALLED")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DEBUG", "🔥 MusifyAudioServiceActivity - onResume CALLED")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d("DEBUG", "🔥🔥🔥 MusifyAudioServiceActivity - configureFlutterEngine CALLED")
        Log.d("DEBUG", "🔥 FlutterEngine: $flutterEngine")
        
        try {
            cacheFlutterEngine()
            setupMethodChannel(flutterEngine)
            Log.d("DEBUG", "✅✅✅ MusifyAudioServiceActivity - Method channel SETUP COMPLETE")
            
            // Test the channel immediately from native side
            testChannelFromNative()
            
        } catch (e: Exception) {
            Log.e("DEBUG", "❌❌❌ MusifyAudioServiceActivity - Method channel setup FAILED: ${e.message}", e)
        }
    }

    private fun testImmediateEngineAvailability() {
        Log.d("DEBUG", "🧪 Testing immediate engine availability...")
        val flutterEngine = getFlutterEngine()
        if (flutterEngine != null) {
            Log.d("DEBUG", "✅ FlutterEngine available immediately in onCreate")
            setupMethodChannel(flutterEngine)
        } else {
            Log.d("DEBUG", "❌ FlutterEngine NOT available immediately in onCreate")
        }
    }

    private fun testChannelFromNative() {
        Log.d("DEBUG", "🧪 Testing channel from native side...")
        // We can't call back to Flutter here, but we can log that we're ready
        Log.d("DEBUG", "✅ Native side ready to receive method calls")
    }

    private fun setupMethodChannel(flutterEngine: FlutterEngine) {
        Log.d("DEBUG", "🔄 Setting up method channel: $CHANNEL")
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        
        methodChannel?.setMethodCallHandler { call, result ->
            Log.d("DEBUG", "🎯🎯🎯 METHOD CALL RECEIVED: ${call.method}")
            Log.d("DEBUG", "📦 Method arguments: ${call.arguments}")
            
            when (call.method) {
                "testConnection" -> {
                    Log.d("DEBUG", "✅ testConnection - SUCCESS")
                    result.success("MusifyAudioServiceActivity connected at ${System.currentTimeMillis()}")
                }
                "updateWidget" -> {
                    val title = call.argument<String>("title") ?: ""
                    val artist = call.argument<String>("artist") ?: ""
                    val isPlaying = call.argument<Boolean>("isPlaying") ?: false
                    val albumPath = call.argument<String>("albumPath")
                    
                    Log.d("DEBUG", "📱 updateWidget - Title: '$title', Artist: '$artist', Playing: $isPlaying")
                    
                    updateWidgetData(this, title, artist, isPlaying, albumPath)
                    result.success("Widget updated successfully")
                }
                "togglePlay" -> {
                    Log.d("DEBUG", "⏯️ togglePlay received")
                    result.success("Toggle play received")
                }
                "next" -> {
                    Log.d("DEBUG", "⏭️ next received")
                    result.success("Next received")
                }
                "prev" -> {
                    Log.d("DEBUG", "⏮️ prev received")
                    result.success("Previous received")
                }
                else -> {
                    Log.d("DEBUG", "❌ Unknown method: ${call.method}")
                    result.notImplemented()
                }
            }
        }
        
        Log.d("DEBUG", "✅ Method channel handler registered for: $CHANNEL")
    }

    private fun cacheFlutterEngine() {
        try {
            val flutterEngine = getFlutterEngine()
            if (flutterEngine != null) {
                FlutterEngineCache.getInstance().put("musify_engine", flutterEngine)
                Log.d("DEBUG", "✅ FlutterEngine cached successfully")
            } else {
                Log.d("DEBUG", "❌ No FlutterEngine available to cache")
            }
        } catch (e: Exception) {
            Log.e("DEBUG", "❌ Error caching FlutterEngine: ${e.message}")
        }
    }

    private fun handleWidgetAction(intent: Intent?) {
        val action = intent?.action
        Log.d("DEBUG", "Handling widget action: $action")
        
        if (action != null) {
            processWidgetAction(action)
        }
    }
    
    private fun processWidgetAction(action: String?) {
        Log.d("DEBUG", "Processing widget action: $action")
        
        when (action) {
            MusifyWidgetProvider.ACTION_TOGGLE_PLAY -> {
                methodChannel?.invokeMethod("togglePlay", null)
            }
            MusifyWidgetProvider.ACTION_NEXT -> {
                methodChannel?.invokeMethod("next", null)
            }
            MusifyWidgetProvider.ACTION_PREV -> {
                methodChannel?.invokeMethod("prev", null)
            }
        }
    }

    private fun updateWidgetData(context: Context, title: String, artist: String, isPlaying: Boolean, albumPath: String?) {
        try {
            Log.d("DEBUG", "🔄 updateWidgetData: '$title' - '$artist', playing: $isPlaying")
            
            // Save data to SharedPreferences
            val prefs: SharedPreferences = context.getSharedPreferences("MusifyWidget", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("title", title)
            editor.putString("artist", artist)
            editor.putBoolean("isPlaying", isPlaying)
            editor.putString("albumPath", albumPath)
            editor.apply()
            
            Log.d("DEBUG", "✅ Widget data saved to SharedPreferences")
            
            // Trigger widget update
            MusifyWidgetProvider().forceWidgetUpdate(context)
            Log.d("DEBUG", "✅ Widget update triggered")
            
        } catch (e: Exception) {
            Log.e("DEBUG", "❌ Error updating widget data: ${e.message}", e)
        }
    }
}