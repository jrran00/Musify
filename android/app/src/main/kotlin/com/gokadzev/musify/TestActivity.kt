package com.gokadzev.musify

import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class TestActivity: FlutterActivity() {
    private val CHANNEL = "com.gokadzev.musify/widget"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d("TestActivity", "=== TEST ACTIVITY CONFIGURING FLUTTER ENGINE ===")
        
        try {
            val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            
            methodChannel.setMethodCallHandler { call, result ->
                Log.d("TestActivity", "🎯 TEST METHOD CALL RECEIVED: ${call.method}")
                
                when (call.method) {
                    "testConnection" -> {
                        Log.d("TestActivity", "✅ TEST connection received - SUCCESS")
                        result.success("TestActivity connected successfully!")
                    }
                    "updateWidget" -> {
                        Log.d("TestActivity", "📱 TEST updateWidget received")
                        result.success("TestActivity: Widget update received")
                    }
                    else -> {
                        Log.d("TestActivity", "❌ TEST Unknown method: ${call.method}")
                        result.notImplemented()
                    }
                }
            }
            
            Log.d("TestActivity", "✅ TEST Method channel '$CHANNEL' initialized in TestActivity")
        } catch (e: Exception) {
            Log.e("TestActivity", "❌ TEST Failed to initialize method channel: ${e.message}")
        }
    }
}