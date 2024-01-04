package com.ggm.goguma

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val DURATION : Long = 3000
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startTime: Long = SystemClock.elapsedRealtime()
        setContentView(R.layout.activity_splash)

        Handler(Looper.myLooper()!!).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish()
        },DURATION)
        val endTime: Long = SystemClock.elapsedRealtime()
        val elapsedTime: Long = endTime - startTime
        Log.d("스플래시화면 경과시간", "Time : $elapsedTime ms")
    }

}
