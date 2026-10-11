package com.example.musictheory.util
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
fun Context.findActivity(): Activity? { var c: Context = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }
fun Context.safeStart(intent: Intent, toastOnError: String) {
    try { startActivity(intent) } catch (_: Exception) {
        Toast.makeText(this, toastOnError, Toast.LENGTH_SHORT).show() } }
