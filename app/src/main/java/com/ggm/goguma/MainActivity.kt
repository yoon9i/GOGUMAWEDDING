package com.ggm.goguma

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ggm.goguma.databinding.ActivityMainBinding
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.talk.TalkApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // activitiy_main.xml
    private lateinit var progressBar: ProgressBar
    private lateinit var textView: TextView

    private val checkNetworkInterval = 15000L
    private var networkCheckHandler = Handler(Looper.getMainLooper())

    private val gogumaurl: String by lazy {getString(R.string.goguma_url)}

    private var doubleBackToExitPressedOnce = false // backstack setting

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime: Long = SystemClock.elapsedRealtime()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // root -> activity_main.xml - root layout

        progressBar = binding.gogumaProgressbarCircle // Progressbar
        textView = binding.gogumaProgressbarTxt // Progressbar

        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.kakao_sdk))

        // 웹뷰 설정
        with(binding.gogumaWebview.settings) {
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        setBackPressedCallback()

        // 네트워크 상태 확인
        if (isNetworkAvailable(this)) {
            binding.gogumaWebview.webViewClient = GogumaWebViewClient() // 웹뷰 설정
            binding.gogumaWebview.loadUrl(gogumaurl)
        } else {
            // 네트워크가 연결되어 있지 않을 때 사용자에게 메시지 표시
            showToast(getString(R.string.network_unavailable_message))
            scheduleNetworkCheck() // 네트워크 연결상태 체크
        }

        val endTime: Long = SystemClock.elapsedRealtime()
        val elapsedTime: Long = endTime - startTime
        Log.d("메인화면 경과시간", "Time : $elapsedTime ms")
        reportFullyDrawn() // 액티비티가 화면에 완전히 표현되었다고 시스템에 알리기.
    }

    inner class GogumaWebViewClient : WebViewClient() {
        private val startTime: Long = SystemClock.elapsedRealtime()
        // 코루틴을 사용하여 UI 업데이터를 처리
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            lifecycleScope.launch {
                Log.d("-ONPAGESTART-", "onPageStarted: on page start. URL: $url")
                // 페이지 로드가 시작됐을때 progressbar 관련 부분이 표시됨.
                progressBar.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
            }


        }

        override fun onPageFinished(view: WebView?, url: String?) {
            lifecycleScope.launch {
                Log.d("-ONPAGEFINISH-", "onPageFinished: on page finish. URL: $url")
                val endTime: Long = SystemClock.elapsedRealtime()
                val elapsedTime: Long = endTime - startTime
                Log.d("-ONPAGEFINISH_TIME-", "Time : $elapsedTime ms")
                // 페이지 로드가 시작됬을때 progressbar 관련부분이 사라짐.
                progressBar.visibility = View.GONE
                textView.visibility = View.GONE
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            Log.d("CURRENT_LOCATION_URL", request.url.toString()) // 현재 위치를 log 로 url 표시
            val uri: Uri = request.url
            val url: String = uri.toString()

            // url 에 _jiZPj 가 포함되어 있다면 실행
            if (url.contains("_jiZPj")) {
                lifecycleScope.launch {
                    val channelUrl = TalkApiClient.instance.addChannelUrl("_jiZPj")
                    KakaoCustomTabsClient.openWithDefault(this@MainActivity, Uri.parse(channelUrl.toString()))
                }
                return true
            }

            return false
        }
    }

// --------------------------------------------------------------------------------

    // backstack
    private fun setBackPressedCallback() {
        val dispatcher = onBackPressedDispatcher

        val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.gogumaWebview.canGoBack()) {
                    binding.gogumaWebview.goBack()
                    showToast("이전 페이지로 이동합니다.")
                } else {
                    handleAppExit()
                }
            }
        }
        dispatcher.addCallback(this, onBackPressedCallback)
    }

    // network check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun scheduleNetworkCheck() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(checkNetworkInterval)
            if (isNetworkAvailable(applicationContext)) {
                // 네트워크 연결이 복구된 경우 웹뷰 다시 로드
                binding.gogumaWebview.webViewClient = GogumaWebViewClient()
                binding.gogumaWebview.loadUrl(gogumaurl)
                showToast("네트워크가 정상적으로 연결되었습니다.")
            } else {
                // 네트워크가 아직 연결되어 있지 않은 경우 재확인 예약
                scheduleNetworkCheck()
            }
        }
    }

    // backstack 두번시 종료
    private fun handleAppExit() {
        // 2초 안에 두번 backstack 시 앱 종료
        if (doubleBackToExitPressedOnce) {
            finish()
        } else {
            showToast("한 번 더 뒤로 가기를 누르면 종료됩니다.")
            doubleBackToExitPressedOnce = true
            Handler(Looper.myLooper()!!).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        binding.gogumaWebview.destroy()
        networkCheckHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

