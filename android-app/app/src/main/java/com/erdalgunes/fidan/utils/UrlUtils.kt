package com.erdalgunes.fidan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object UrlUtils {
    private const val TAG = "UrlUtils"
    
    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL: $url", e)
            false
        }
    }
    
    fun openUrl(context: Context, url: String): Boolean {
        return try {
            if (!isValidUrl(url)) {
                Log.w(TAG, "Attempted to open invalid URL: $url")
                return false
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            false
        }
    }
}