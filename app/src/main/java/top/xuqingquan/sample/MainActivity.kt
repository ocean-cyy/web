package top.xuqingquan.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.publics.AbsAgentWebUIController

class MainActivity : AppCompatActivity() {

    private val url = "https://m.baidu.com/"

    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
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
            .setWebViewClient(object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    Timber.d("onReceivedError-system-m")
                    super.onReceivedError(view, request, error)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    Timber.d("onReceivedError-system")
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }
            })
            .setWebViewClient(object : com.tencent.smtt.sdk.WebViewClient() {
                override fun onReceivedError(
                    p0: com.tencent.smtt.sdk.WebView?,
                    p1: com.tencent.smtt.export.external.interfaces.WebResourceRequest?,
                    p2: com.tencent.smtt.export.external.interfaces.WebResourceError?
                ) {
                    Timber.d("onReceivedError-x5-m")
//                    super.onReceivedError(p0, p1, p2)
                }

                fun onMainFrameError(
                    controller: AbsAgentWebUIController, view: com.tencent.smtt.sdk.WebView,
                    errorCode: Int, description: String, failingUrl: String
                ) {
                    Timber.d("errorCode=${errorCode},description=${description},failingUrl=${failingUrl}")
                }


            })
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