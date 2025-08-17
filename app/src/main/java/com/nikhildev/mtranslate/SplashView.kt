package com.nikhildev.mtranslate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SplashView : AppCompatActivity() {
    private lateinit var fadeInSlideBottom : Animation
    private lateinit var fadeInSlideTop : Animation
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_view)
        val windowInsetsController  = WindowCompat.getInsetsController(window,window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        WindowCompat.setDecorFitsSystemWindows(window, false)

//        actionBar?.hide()

        auth = FirebaseAuth.getInstance()
        Log.d("User",auth.currentUser.toString())
        fadeInSlideBottom = AnimationUtils.loadAnimation(this,R.anim.fade_in_slide_bottom_bounce)
        fadeInSlideBottom.duration = 2500
        fadeInSlideTop = AnimationUtils.loadAnimation(this,R.anim.fade_in_slide_top_bounce)
        fadeInSlideTop.duration = 2500

        findViewById<ImageView>(R.id.splashImage).startAnimation(fadeInSlideBottom)
        findViewById<CardView>(R.id.card1).startAnimation(fadeInSlideTop)

        Handler(Looper.getMainLooper()).postDelayed({
            val mainIntent =  Intent(this@SplashView, MainActivity::class.java)
            val signInIntent = Intent(this@SplashView, SignInView::class.java)
            if(auth.currentUser !=null){ startActivity(mainIntent) }
            else{ startActivity(signInIntent)}
            finish()
        }, 5000)
    }
}