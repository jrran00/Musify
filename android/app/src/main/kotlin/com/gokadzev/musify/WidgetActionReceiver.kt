package com.gokadzev.musify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache

class WidgetActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WidgetActionReceiver", "🎯 Action received: ${intent.action}")
        
        // Try to use cached FlutterEngine for background operation
        val flutterEngine = FlutterEngineCache.getInstance().get("musify_engine")
        
        if (flutterEngine != null) {
            Log.d("WidgetActionReceiver", "✅ Using cached FlutterEngine - app will NOT open")
            sendActionViaMethodChannel(flutterEngine, intent.action)
        } else {
            Log.d("WidgetActionReceiver", "❌ No cached FlutterEngine available")
            // Start the activity to initialize the engine and handle the action
            startAudioService(context, intent.action)
        }
    }
    
    private fun startAudioService(context: Context, action: String?) {
        try {
            Log.d("WidgetActionReceiver", "🚀 Starting audio service to initialize engine")
            val serviceIntent = Intent(context, MusifyAudioServiceActivity::class.java).apply {
                this.action = action
                putExtra("from_widget", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(serviceIntent)
        } catch (e: Exception) {
            Log.e("WidgetActionReceiver", "❌ Error starting service: ${e.message}")
        }
    }
    
    private fun sendActionViaMethodChannel(flutterEngine: FlutterEngine, action: String?) {
        try {
            Log.d("WidgetActionReceiver", "🔄 Creating method channel...")
            
            // Use the SAME channel name that's set up in the activity
            val channel = io.flutter.plugin.common.MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.gokadzev.musify/widget")
            
            Log.d("WidgetActionReceiver", "📡 Sending action via method channel: $action")
            
            when (action) {
                MusifyWidgetProvider.ACTION_TOGGLE_PLAY -> {
                    Log.d("WidgetActionReceiver", "⏯️ Invoking togglePlay")
                    channel.invokeMethod("togglePlay", null, object : io.flutter.plugin.common.MethodChannel.Result {
                        override fun success(result: Any?) {
                            Log.d("WidgetActionReceiver", "✅ togglePlay method call successful")
                        }
                        
                        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                            Log.e("WidgetActionReceiver", "❌ togglePlay method call failed: $errorCode - $errorMessage")
                        }
                        
                        override fun notImplemented() {
                            Log.e("WidgetActionReceiver", "❌ togglePlay method not implemented")
                        }
                    })
                }
                MusifyWidgetProvider.ACTION_NEXT -> {
                    Log.d("WidgetActionReceiver", "⏭️ Invoking next")
                    channel.invokeMethod("next", null, object : io.flutter.plugin.common.MethodChannel.Result {
                        override fun success(result: Any?) {
                            Log.d("WidgetActionReceiver", "✅ next method call successful")
                        }
                        
                        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                            Log.e("WidgetActionReceiver", "❌ next method call failed: $errorCode - $errorMessage")
                        }
                        
                        override fun notImplemented() {
                            Log.e("WidgetActionReceiver", "❌ next method not implemented")
                        }
                    })
                }
                MusifyWidgetProvider.ACTION_PREV -> {
                    Log.d("WidgetActionReceiver", "⏮️ Invoking prev")
                    channel.invokeMethod("prev", null, object : io.flutter.plugin.common.MethodChannel.Result {
                        override fun success(result: Any?) {
                            Log.d("WidgetActionReceiver", "✅ prev method call successful")
                        }
                        
                        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                            Log.e("WidgetActionReceiver", "❌ prev method call failed: $errorCode - $errorMessage")
                        }
                        
                        override fun notImplemented() {
                            Log.e("WidgetActionReceiver", "❌ prev method not implemented")
                        }
                    })
                }
                else -> {
                    Log.d("WidgetActionReceiver", "❌ Unknown action: $action")
                }
            }
            
            Log.d("WidgetActionReceiver", "✅ Action sent via method channel - app will remain in background")
        } catch (e: Exception) {
            Log.e("WidgetActionReceiver", "💥 Error using method channel: ${e.message}", e)
        }
    }
}