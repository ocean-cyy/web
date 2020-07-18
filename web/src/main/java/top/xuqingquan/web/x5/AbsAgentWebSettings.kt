package top.xuqingquan.web.x5

import android.annotation.SuppressLint
import android.os.Build
import com.tencent.smtt.sdk.*
import top.xuqingquan.web.AgentWeb
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.nokernel.WebUtils
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.networkIsConnect

abstract class AbsAgentWebSettings : IAgentWebSettings<WebSettings>, WebListenerManager {
    private var mWebSettings: WebSettings? = null

    fun bindAgentWeb(agentWeb: AgentWeb) {
        this.bindAgentWebSupport(agentWeb)
    }

    protected abstract fun bindAgentWebSupport(agentWeb: AgentWeb)

    override fun toSetting(webView: WebView?): IAgentWebSettings<*> {
        settings(webView)
        return this
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled", "ObsoleteSdkInt")
    private fun settings(webView: WebView?) {
        if (webView == null) {
            return
        }
        mWebSettings = webView.settings
        mWebSettings!!.javaScriptEnabled = true
        mWebSettings!!.setSupportZoom(true)
        mWebSettings!!.builtInZoomControls = true
        mWebSettings!!.displayZoomControls = false
        mWebSettings!!.savePassword = false
        if (networkIsConnect(webView.context)) {
            //根据cache-control获取数据。
            mWebSettings!!.cacheMode = WebSettings.LOAD_DEFAULT
        } else {
            //没网，则从本地获取，即离线加载
            mWebSettings!!.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //适配5.0不允许http和https混合使用情况
            mWebSettings!!.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }/* else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //加上这一句可能导致Android4.4手机出现加载网页白屏
            // webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }*/
        mWebSettings!!.textZoom = 100
        mWebSettings!!.databaseEnabled = true
        mWebSettings!!.setAppCacheEnabled(true)
        mWebSettings!!.loadsImagesAutomatically = true
        mWebSettings!!.setSupportMultipleWindows(false)
        // 是否阻塞加载网络图片  协议http or https
        mWebSettings!!.blockNetworkImage = false
        // 允许加载本地文件html  file协议
        mWebSettings!!.allowFileAccess = true
        // 通过 file url 加载的 Javascript 读取其他的本地文件 .建议关闭
        mWebSettings!!.setAllowFileAccessFromFileURLs(false)
        // 允许通过 file url 加载的 Javascript 可以访问其他的源，包括其他的文件和 http，https 等其他的源
        mWebSettings!!.setAllowUniversalAccessFromFileURLs(false)
        mWebSettings!!.javaScriptCanOpenWindowsAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebSettings!!.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        } else {
            mWebSettings!!.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }
        mWebSettings!!.loadWithOverviewMode = true
        mWebSettings!!.useWideViewPort = true
        mWebSettings!!.domStorageEnabled = true
        mWebSettings!!.setNeedInitialFocus(true)
        mWebSettings!!.defaultTextEncodingName = "utf-8"//设置编码格式
        mWebSettings!!.defaultFontSize = 16
        mWebSettings!!.minimumFontSize = 12//设置 WebView 支持的最小字体大小，默认为 8
        mWebSettings!!.setGeolocationEnabled(true)
        val dir = WebUtils.getCachePath(webView.context)
        Timber.i("dir:" + dir + "   appcache:" + WebUtils.getCachePath(webView.context))
        //设置数据库路径  api19 已经废弃,这里只针对 webkit 起作用
        mWebSettings!!.setGeolocationDatabasePath(dir)
        mWebSettings!!.databasePath = dir
        mWebSettings!!.setAppCachePath(dir)
        //缓存文件最大值
        mWebSettings!!.setAppCacheMaxSize(java.lang.Long.MAX_VALUE)
        mWebSettings!!.userAgentString = getWebSettings()!!
            .userAgentString + USERAGENT_AGENTWEB + USERAGENT_UC
        Timber.i("UserAgentString : " + mWebSettings!!.userAgentString)
    }

    override fun getWebSettings(): WebSettings? {
        return mWebSettings
    }

    override fun setWebChromeClient(
        webView: WebView?, webChromeClient: WebChromeClient
    ): WebListenerManager {
        webView?.webChromeClient = webChromeClient
        return this
    }

    override fun setWebViewClient(
        webView: WebView?, webViewClient: WebViewClient
    ): WebListenerManager {
        webView?.webViewClient = webViewClient
        return this
    }

    override fun setDownloader(
        webView: WebView?, downloadListener: DownloadListener?
    ): WebListenerManager {
        webView?.setDownloadListener(downloadListener)
        return this
    }

    companion object {
        private const val USERAGENT_UC = " UCBrowser/11.6.4.950 "
        private const val USERAGENT_AGENTWEB = WebConfig.AGENTWEB_VERSION

        @JvmStatic
        val instance: AbsAgentWebSettings
            get() = AgentWebSettingsImpl()
    }

}
