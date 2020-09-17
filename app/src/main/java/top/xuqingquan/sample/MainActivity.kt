package top.xuqingquan.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.system.AdblockWebView

class MainActivity : AppCompatActivity() {

    private val url = "https://m.baidu.com/"

    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView = AdblockWebView(this)
        webView.isDebug = true
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .setWebView(webView)
            .setWebChromeClient(object : WebChromeClient() {})
            .setWebViewClient(object : WebViewClient() {})
            .setPermissionInterceptor(object : PermissionInterceptor {
                override fun intercept(
                    url: String?,
                    permissions: Array<String>,
                    action: String
                ): Boolean {
                    Timber.d("url===>$url")
                    Timber.d("permissions===>$permissions")
                    Timber.d("action===>$action")
                    return false
                }
            })
            .setOpenOtherPageWays(OpenOtherPageWays.DISALLOW)
            .createAgentWeb()
            .ready()
            .get()
        agentWeb.urlLoader?.loadUrl(url)
    }

    override fun onPause() {
        agentWeb.webLifeCycle.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        agentWeb.webLifeCycle.onResume()
    }

    override fun onDestroy() {
        agentWeb.webLifeCycle.onDestroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return agentWeb.handleKeyEvent(keyCode, event)
    }
}