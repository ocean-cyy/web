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

class MainActivity : AppCompatActivity() {

    private val url = "https://ditu.baidu.com/"

    private lateinit var agentWeb: AgentWeb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        agentWeb = AgentWeb
            .with(this)
            .setAgentWebParent(rootView, ViewGroup.LayoutParams(-1, -1))
            .useDefaultIndicator()
            .interceptUnknownUrl()
//            .setWebView(AdblockWebView(this).apply {
//                isDebug=true
//                setEventsListener(object :EventsListener{
//                    override fun onNavigation() {
//                        Timber.d("onNavigation")
//                    }
//
//                    override fun onResourceLoadingBlocked(info: EventsListener.BlockedResourceInfo?) {
//                        Timber.d("onResourceLoadingBlocked:${info?.requestUrl},${info?.parentFrameUrls},${info?.contentType}")
//                    }
//
//                    override fun onResourceLoadingWhitelisted(info: EventsListener.WhitelistedResourceInfo?) {
//                        Timber.d("onResourceLoadingWhitelisted:${info?.requestUrl},${info?.parentFrameUrls},${info?.reason}")
//                    }
//                })
//            })
//            .setPermissionInterceptor(object : PermissionInterceptor {
//                override fun intercept(
//                    url: String?,
//                    permissions: Array<String>,
//                    action: String
//                ): Boolean {
//                    Timber.d("url===>$url")
//                    Timber.d("permissions===>$permissions")
//                    Timber.d("action===>$action")
//                    return false
//                }
//            })
//            .setWebChromeClient(object : WebChromeClient(){
//                override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                    super.onProgressChanged(view, newProgress)
//                    Timber.d("newProgress=${newProgress}")
//                }
//            })
//            .setWebViewClient(object : WebViewClient(){
//                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                    super.onPageStarted(view, url, favicon)
//                    Timber.d("onPageStarted")
//                }
//
//                override fun onPageFinished(view: WebView?, url: String?) {
//                    super.onPageFinished(view, url)
//                    Timber.d("onPageFinished")
//                }
//
//                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//                override fun shouldInterceptRequest(
//                    view: WebView?,
//                    request: WebResourceRequest?
//                ): WebResourceResponse? {
//                    Timber.d("shouldInterceptRequest:"+request?.url)
//                    return super.shouldInterceptRequest(view, request)
//                }
//
//                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//                override fun onReceivedError(
//                    view: WebView?,
//                    request: WebResourceRequest?,
//                    error: WebResourceError?
//                ) {
//                    Timber.d("onReceivedError:"+request?.url)
//                    super.onReceivedError(view, request, error)
//                }
//            })
            .setOpenOtherPageWays(OpenOtherPageWays.ASK)
            .parseThunder()
            .createAgentWeb()
            .ready()
            .get()
        agentWeb.urlLoader?.loadUrl(url)
        agentWeb.webCreator?.getWebView()?.settings?.userAgentString = ""
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