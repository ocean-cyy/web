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
import top.xuqingquan.utils.toast
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.OpenOtherPageWays
import top.xuqingquan.web.nokernel.PermissionInterceptor

class MainActivity : AppCompatActivity() {

    private val url = "https://m.baidu.com/s?word=拍照识图"

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
            .setWebViewClient(object :WebViewClient(){
                override fun onReceivedError(
                    view: WebView?,request: WebResourceRequest?,error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Timber.d("onReceivedError-system-m")
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Timber.d("onReceivedError-system")
                }
            })
            .setWebViewClient(object :com.tencent.smtt.sdk.WebViewClient(){
                override fun onReceivedError(
                    p0: com.tencent.smtt.sdk.WebView?,
                    p1: com.tencent.smtt.export.external.interfaces.WebResourceRequest?,
                    p2: com.tencent.smtt.export.external.interfaces.WebResourceError?
                ) {
                    super.onReceivedError(p0, p1, p2)
                    Timber.d("onReceivedError-x5-m")
                }

                override fun onReceivedError(
                    p0: com.tencent.smtt.sdk.WebView?,
                    p1: Int,
                    p2: String?,
                    p3: String?
                ) {
                    super.onReceivedError(p0, p1, p2, p3)
                    Timber.d("onReceivedError-x5")
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