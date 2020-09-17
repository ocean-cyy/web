package top.xuqingquan.sample

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_start.*
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.publics.AgentWebConfig

//import org.adblockplus.libadblockplus.android.settings.AdblockHelper

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        AgentWebConfig.removeAllCookies(null)
        AgentWebConfig.removeAllX5Cookies(null)
        btn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
//        test.setOnClickListener {
//        Timber.d("isAdblockEnabled=${main_webview.isAdblockEnabled}")
//        main_webview.isDebugMode = true
//        main_webview.setProvider(AdblockHelper.get().provider)
//        main_webview.loadUrl("http://www.meilizyz.com/")
//        main_webview.webChromeClient = object : WebChromeClient() {}
//        main_webview.webViewClient = object : WebViewClient() {}
//        }
    }
}