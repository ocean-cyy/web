package top.xuqingquan.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.FrameLayout
import android.support.annotation.ColorInt
import android.support.annotation.DimenRes
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import top.xuqingquan.utils.px2dip
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.publics.AgentWebConfig
import com.tencent.smtt.sdk.WebView as X5WebView

/**
 * Created by 许清泉 on 2019-05-22 21:00
 * 调用 [ScaffoldWebView.loadUrl]方法之前可以设置各种自定义的参数
 */
class ScaffoldWebView : FrameLayout {

    var agentWeb: AgentWeb? = null
        private set

    //错误页面id
    @ColorInt
    var indicatorColor: Int = -1//进度条颜色，-1为默认值

    @DimenRes
    var indicatorHeight: Int = 0 //进度条高度，高度为2，单位为dp

    @LayoutRes
    var error_layout: Int = -1

    @IdRes
    var refresh_error: Int = -1
    var url: String? = "https://m.baidu.com"
    var debug: Boolean = false
        set(value) {
            field = value
            if (field) {
                AgentWebConfig.debug()
            }
        }

    constructor(context: Context) : super(context)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.scaffold_ScaffoldWebView)
        typedArray.getString(R.styleable.scaffold_ScaffoldWebView_scaffold_url)?.let {
            url = it
        }
        debug = typedArray.getBoolean(R.styleable.scaffold_ScaffoldWebView_scaffold_debug, false)
        indicatorColor =
            typedArray.getColor(R.styleable.scaffold_ScaffoldWebView_scaffold_indicatorColor, -1)
        indicatorHeight = typedArray.getDimension(
            R.styleable.scaffold_ScaffoldWebView_scaffold_indicatorHeight,
            -1f
        ).toInt()
        error_layout =
            typedArray.getResourceId(R.styleable.scaffold_ScaffoldWebView_scaffold_error_layout, -1)
        if (error_layout != -1) {
            refresh_error = typedArray.getResourceId(
                R.styleable.scaffold_ScaffoldWebView_scaffold_refresh_error,
                -1
            )
        }
        indicatorHeight = if (indicatorHeight == -1) {
            2
        } else {
            px2dip(indicatorHeight).toInt()
        }
        typedArray.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initAgentWeb() {
        initAgentWeb(null)
    }

    fun initAgentWeb(aw: AgentWeb?) {
        agentWeb = aw
            ?: AgentWeb.with(context as Activity)
                .setAgentWebParent(
                    this,
                    -1,
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                )//传入AgentWeb的父控件。
                .useDefaultIndicator(indicatorColor, indicatorHeight)
                .setMainFrameErrorView(error_layout, refresh_error)//当使用X5时候这一句失效
                .interceptUnkownUrl() //拦截找不到相关页面的Url AgentWeb 3.0.0 加入。
                .setPermissionInterceptor(object : PermissionInterceptor {
                    override fun intercept(
                        url: String?,
                        permissions: Array<String>,
                        action: String
                    ) = false
                })
                .createAgentWeb()//创建AgentWeb。
                .get()
        if (WebConfig.enableTbs()) {
            agentWeb!!.x5WebCreator?.getWebView()?.overScrollMode = X5WebView.OVER_SCROLL_NEVER
        } else {
            agentWeb!!.webCreator?.getWebView()?.overScrollMode = WebView.OVER_SCROLL_NEVER
        }
    }

    /**
     * 在调用该方法前可以自定义其他参数
     */
    fun loadUrl(url: String = this@ScaffoldWebView.url!!) {
        if (agentWeb == null) {
            initAgentWeb()
        }
        agentWeb!!.urlLoader?.loadUrl(url)
    }

    fun onResume() {
        agentWeb?.webLifeCycle?.onResume()
    }

    fun onPause() {
        agentWeb?.webLifeCycle?.onPause()
    }

    fun onDestroy() {
        try {
            agentWeb?.urlLoader?.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
            agentWeb?.clearWebCache()
            agentWeb?.destroy()
        } catch (e: Throwable) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return agentWeb?.handleKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    fun reload() = agentWeb?.urlLoader?.reload()

    fun getCurrentUrl(): String? {
        return if (WebConfig.enableTbs()) {
            agentWeb?.x5WebCreator?.getWebView()?.url
        } else {
            agentWeb?.webCreator?.getWebView()?.url
        }
    }

}
