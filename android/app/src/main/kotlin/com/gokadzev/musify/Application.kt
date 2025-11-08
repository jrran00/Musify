package com.gokadzev.musify

import android.app.Application
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import android.util.Log

class Application : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("MusifyApplication", "=== APPLICATION CREATED ===")
        
        try {
            // Instantiate a FlutterEngine
            val flutterEngine = FlutterEngine(this)
            
            Log.d("MusifyApplication", "🎯 FlutterEngine created")
            
            // Start executing Dart code
            flutterEngine.dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
            )
            
            Log.d("MusifyApplication", "✅ Dart entrypoint executed")
            
            // Cache the FlutterEngine
            FlutterEngineCache
                .getInstance()
                .put("musify_engine", flutterEngine)
                
            Log.d("MusifyApplication", "✅ FLUTTER ENGINE CACHED with key: musify_engine")
            
            // Verify it was cached
            val cachedEngine = FlutterEngineCache.getInstance().get("musify_engine")
            if (cachedEngine != null) {
                Log.d("MusifyApplication", "✅ FlutterEngine verified in cache")
            } else {
                Log.e("MusifyApplication", "❌ FlutterEngine NOT found in cache")
            }
            
        } catch (e: Exception) {
            Log.e("MusifyApplication", "💥 Error setting up FlutterEngine: ${e.message}", e)
        }
    }
}