package top.xuqingquan.web;

import android.app.Activity;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import android.support.v4.app.Fragment;

import top.xuqingquan.web.nokernel.*;
import top.xuqingquan.web.publics.*;
import top.xuqingquan.utils.Timber;

import java.lang.ref.WeakReference;
import java.util.Map;

@SuppressWarnings({"unused", "rawtypes"})
public final class AgentWeb {
    /**
     * Activity
     */
    private Activity mActivity;
    /**
     * 承载 WebParentLayout 的 ViewGroup
     */
    private ViewGroup mViewGroup;
    /**
     * 负责创建布局 WebView ，WebParentLayout  Indicator等。
     */
    private top.xuqingquan.web.system.WebCreator mWebCreator;
    private top.xuqingquan.web.x5.WebCreator mX5WebCreator;
    /**
     * 管理 WebSettings
     */
    private top.xuqingquan.web.system.IAgentWebSettings mAgentWebSettings;
    private top.xuqingquan.web.x5.IAgentWebSettings mX5AgentWebSettings;
    /**
     * IndicatorController 控制Indicator
     */
    private IndicatorController mIndicatorController;
    /**
     * WebChromeClient
     */
    private android.webkit.WebChromeClient mWebChromeClient;
    private com.tencent.smtt.sdk.WebChromeClient mX5WebChromeClient;
    /**
     * WebViewClient
     */
    private android.webkit.WebViewClient mWebViewClient;
    private com.tencent.smtt.sdk.WebViewClient mX5WebViewClient;
    /**
     * is show indicator
     */
    private boolean mEnableIndicator;
    /**
     * IEventHandler 处理WebView相关返回事件
     */
    private IEventHandler mIEventHandler;
    /**
     * WebView 注入对象
     */
    private ArrayMap<String, Object> mJavaObjects = new ArrayMap<>();
    /**
     * WebListenerManager
     */
    private top.xuqingquan.web.system.WebListenerManager mWebListenerManager;
    private top.xuqingquan.web.x5.WebListenerManager mX5WebListenerManager;
    /**
     * Activity
     */
    private static final int ACTIVITY_TAG = 0;
    /**
     * Fragment
     */
    private static final int FRAGMENT_TAG = 1;
    /**
     * JsAccessEntrace 提供快速JS方法调用
     */
    private JsAccessEntrace mJsAccessEntrace = null;
    /**
     * URL Loader ， 提供了 WebView#loadUrl(url) reload() stopLoading（） postUrl()等方法
     */
    private IUrlLoader mIUrlLoader;
    /**
     * WebView 生命周期 ， 跟随生命周期释放CPU
     */
    private WebLifeCycle mWebLifeCycle;
    /**
     * Video 视屏播放管理类
     */
    private IVideo mIVideo = null;
    /**
     * WebViewClient 辅助控制开关
     */
    private boolean mWebClientHelper;
    /**
     * PermissionInterceptor 权限拦截
     */
    private PermissionInterceptor mPermissionInterceptor;
    /**
     * 是否拦截未知的Url， @link{DefaultWebClient}
     */
    private boolean mIsInterceptUnkownUrl;
    private int mUrlHandleWays = -1;
    /**
     * MiddlewareWebClientBase WebViewClient 中间件
     */
    private top.xuqingquan.web.system.MiddlewareWebClientBase mMiddleWrareWebClientBaseHeader;
    private top.xuqingquan.web.x5.MiddlewareWebClientBase mX5MiddleWrareWebClientBaseHeader;
    /**
     * MiddlewareWebChromeBase WebChromeClient 中间件
     */
    private top.xuqingquan.web.system.MiddlewareWebChromeBase mMiddlewareWebChromeBaseHeader;
    private top.xuqingquan.web.x5.MiddlewareWebChromeBase mX5MiddlewareWebChromeBaseHeader;
    /**
     * 事件拦截
     */
    private EventInterceptor mEventInterceptor;
    /**
     * 注入对象管理类
     */
    private JsInterfaceHolder mJsInterfaceHolder = null;


    private AgentWeb(AgentBuilder agentBuilder) {
        this.mActivity = agentBuilder.mActivity;
        this.mViewGroup = agentBuilder.mViewGroup;
        this.mIEventHandler = agentBuilder.mIEventHandler;
        this.mEnableIndicator = agentBuilder.mEnableIndicator;
        if (WebConfig.enableTbs()) {
            if (agentBuilder.mX5WebCreator == null) {
                mX5WebCreator = configWebCreator(agentBuilder.mBaseIndicatorView, agentBuilder.mIndex, agentBuilder.mLayoutParams, agentBuilder.mIndicatorColor, agentBuilder.mHeight, agentBuilder.mX5WebView, agentBuilder.mX5WebLayout);
            } else {
                mX5WebCreator = agentBuilder.mX5WebCreator;
            }
        } else {
            if (agentBuilder.mWebCreator == null) {
                mWebCreator = configWebCreator(agentBuilder.mBaseIndicatorView, agentBuilder.mIndex, agentBuilder.mLayoutParams, agentBuilder.mIndicatorColor, agentBuilder.mHeight, agentBuilder.mWebView, agentBuilder.mWebLayout);
            } else {
                mWebCreator = agentBuilder.mWebCreator;
            }
        }
        mIndicatorController = agentBuilder.mIndicatorController;
        if (WebConfig.enableTbs()) {
            this.mX5WebChromeClient = agentBuilder.mX5WebChromeClient;
            this.mX5WebViewClient = agentBuilder.mX5WebViewClient;
            this.mX5AgentWebSettings = agentBuilder.mX5AgentWebSettings;
            this.mX5MiddleWrareWebClientBaseHeader = agentBuilder.mX5MiddlewareWebClientBaseHeader;
            this.mX5MiddlewareWebChromeBaseHeader = agentBuilder.mX5ChromeMiddleWareHeader;
            if (getX5WebCreator() != null) {
                this.mIUrlLoader = new UrlLoaderImpl(getX5WebCreator().create().getWebView(), agentBuilder.mHttpHeaders);
                this.mWebLifeCycle = new DefaultWebLifeCycleImpl(getX5WebCreator().getWebView());
                if (getX5WebCreator().getWebParentLayout() instanceof WebParentLayout) {
                    WebParentLayout mWebParentLayout = (WebParentLayout) getX5WebCreator().getWebParentLayout();
                    mWebParentLayout.bindController(agentBuilder.mAgentWebUIController == null ? AgentWebUIControllerImplBase.build() : agentBuilder.mAgentWebUIController);
                    mWebParentLayout.setErrorLayoutRes(agentBuilder.mErrorLayout, agentBuilder.mReloadId);
                    mWebParentLayout.setErrorView(agentBuilder.mErrorView);
                }
            }
        } else {
            this.mWebChromeClient = agentBuilder.mWebChromeClient;
            this.mWebViewClient = agentBuilder.mWebViewClient;
            this.mAgentWebSettings = agentBuilder.mAgentWebSettings;
            this.mMiddleWrareWebClientBaseHeader = agentBuilder.mMiddlewareWebClientBaseHeader;
            this.mMiddlewareWebChromeBaseHeader = agentBuilder.mChromeMiddleWareHeader;
            if (getWebCreator() != null) {
                this.mIUrlLoader = new UrlLoaderImpl(getWebCreator().create().getWebView(), agentBuilder.mHttpHeaders);
                this.mWebLifeCycle = new DefaultWebLifeCycleImpl(getWebCreator().getWebView());
                if (getWebCreator().getWebParentLayout() instanceof WebParentLayout) {
                    WebParentLayout mWebParentLayout = (WebParentLayout) getWebCreator().getWebParentLayout();
                    mWebParentLayout.bindController(agentBuilder.mAgentWebUIController == null ? AgentWebUIControllerImplBase.build() : agentBuilder.mAgentWebUIController);
                    mWebParentLayout.setErrorLayoutRes(agentBuilder.mErrorLayout, agentBuilder.mReloadId);
                    mWebParentLayout.setErrorView(agentBuilder.mErrorView);
                }
            }
        }
        if (agentBuilder.mJavaObject != null && !agentBuilder.mJavaObject.isEmpty()) {
            this.mJavaObjects.putAll((Map<? extends String, ?>) agentBuilder.mJavaObject);
            Timber.i("mJavaObject size:" + this.mJavaObjects.size());
        }
        if (agentBuilder.mPermissionInterceptor == null) {
            this.mPermissionInterceptor = null;
        } else {
            this.mPermissionInterceptor = new PermissionInterceptorWrapper(agentBuilder.mPermissionInterceptor);
        }
        this.mWebClientHelper = agentBuilder.mWebClientHelper;
        this.mIsInterceptUnkownUrl = agentBuilder.mIsInterceptUnkownUrl;
        if (agentBuilder.mOpenOtherPage != null) {
            this.mUrlHandleWays = agentBuilder.mOpenOtherPage.getCode();
        }
        doCompat();
    }

    /**
     * @return PermissionInterceptor 权限控制者
     */
    public PermissionInterceptor getPermissionInterceptor() {
        return this.mPermissionInterceptor;
    }

    public WebLifeCycle getWebLifeCycle() {
        return this.mWebLifeCycle;
    }

    public JsAccessEntrace getJsAccessEntrace() {
        JsAccessEntrace mJsAccessEntrace = this.mJsAccessEntrace;
        if (mJsAccessEntrace == null) {
            if (WebConfig.enableTbs()) {
                this.mJsAccessEntrace = JsAccessEntraceImpl.getInstance(getX5WebCreator().getWebView());
            } else {
                this.mJsAccessEntrace = JsAccessEntraceImpl.getInstance(getWebCreator().getWebView());
            }
            mJsAccessEntrace = this.mJsAccessEntrace;
        }
        return mJsAccessEntrace;
    }

    public AgentWeb clearWebCache() {
        if (WebConfig.enableTbs()) {
            if (getX5WebCreator() != null && getX5WebCreator().getWebView() != null) {
                AgentWebUtils.clearWebViewAllCache(mActivity, getX5WebCreator().getWebView());
            } else {
                AgentWebUtils.clearWebViewAllCache(mActivity);
            }
        } else {
            if (getWebCreator() != null && getWebCreator().getWebView() != null) {
                AgentWebUtils.clearWebViewAllCache(mActivity, getWebCreator().getWebView());
            } else {
                AgentWebUtils.clearWebViewAllCache(mActivity);
            }
        }
        return this;
    }

    public static AgentBuilder with(@NonNull Activity activity) {
        return new AgentBuilder(activity);
    }

    public static AgentBuilder with(@NonNull Fragment fragment) {
        Activity mActivity = fragment.getActivity();
        if (mActivity == null) {
            throw new NullPointerException("fragment.getActivity() can not be null .");
        }
        return new AgentBuilder(fragment);
    }

    public boolean handleKeyEvent(int keyCode, KeyEvent keyEvent) {
        this.mIEventHandler = getIEventHandler();
        if (mIEventHandler != null) {
            return mIEventHandler.onKeyDown(keyCode, keyEvent);
        } else {
            return false;
        }
    }

    public boolean back() {
        this.mIEventHandler = getIEventHandler();
        if (mIEventHandler != null) {
            return mIEventHandler.back();
        } else {
            return false;
        }
    }

    @Nullable
    public top.xuqingquan.web.system.WebCreator getWebCreator() {
        return this.mWebCreator;
    }

    @Nullable
    public top.xuqingquan.web.x5.WebCreator getX5WebCreator() {
        return this.mX5WebCreator;
    }

    private IEventHandler getIEventHandler() {
        if (this.mIEventHandler == null) {
            if (WebConfig.enableTbs()) {
                if (getX5WebCreator() != null) {
                    mIEventHandler = EventHandlerImpl.getInstance(getX5WebCreator().getWebView(), getInterceptor());
                }
            } else {
                if (getWebCreator() != null) {
                    mIEventHandler = EventHandlerImpl.getInstance(getWebCreator().getWebView(), getInterceptor());
                }
            }
        }
        return this.mIEventHandler;
    }

    public top.xuqingquan.web.system.IAgentWebSettings getAgentWebSettings() {
        return this.mAgentWebSettings;
    }

    public top.xuqingquan.web.x5.IAgentWebSettings getX5AgentWebSettings() {
        return this.mX5AgentWebSettings;
    }

    @SuppressWarnings("WeakerAccess")
    public IndicatorController getIndicatorController() {
        return this.mIndicatorController;
    }

    public JsInterfaceHolder getJsInterfaceHolder() {
        return this.mJsInterfaceHolder;
    }

    @Nullable
    public IUrlLoader getUrlLoader() {
        return this.mIUrlLoader;
    }

    public void destroy() {
        this.mWebLifeCycle.onDestroy();
    }

    private void doCompat() {
        mJavaObjects.put("agentWeb", new AgentWebJsInterfaceCompat(this, mActivity));
    }

    private top.xuqingquan.web.system.WebCreator configWebCreator(BaseIndicatorView progressView, int index, ViewGroup.LayoutParams lp, int indicatorColor, int height_dp, android.webkit.WebView webView, top.xuqingquan.web.system.IWebLayout webLayout) {
        if (progressView != null && mEnableIndicator) {
            return new top.xuqingquan.web.system.DefaultWebCreator(mActivity, mViewGroup, lp, index, progressView, webView, webLayout);
        } else {
            return mEnableIndicator ?
                    new top.xuqingquan.web.system.DefaultWebCreator(mActivity, mViewGroup, lp, index, indicatorColor, height_dp, webView, webLayout)
                    : new top.xuqingquan.web.system.DefaultWebCreator(mActivity, mViewGroup, lp, index, webView, webLayout);
        }
    }

    private top.xuqingquan.web.x5.WebCreator configWebCreator(BaseIndicatorView progressView, int index, ViewGroup.LayoutParams lp, int indicatorColor, int height_dp, com.tencent.smtt.sdk.WebView webView, top.xuqingquan.web.x5.IWebLayout webLayout) {
        if (progressView != null && mEnableIndicator) {
            return new top.xuqingquan.web.x5.DefaultWebCreator(mActivity, mViewGroup, lp, index, progressView, webView, webLayout);
        } else {
            return mEnableIndicator ?
                    new top.xuqingquan.web.x5.DefaultWebCreator(mActivity, mViewGroup, lp, index, indicatorColor, height_dp, webView, webLayout)
                    : new top.xuqingquan.web.x5.DefaultWebCreator(mActivity, mViewGroup, lp, index, webView, webLayout);
        }
    }

    private AgentWeb go(String url) {
        this.getUrlLoader().loadUrl(url);
        IndicatorController mIndicatorController = getIndicatorController();
        if (!TextUtils.isEmpty(url) && mIndicatorController != null && mIndicatorController.offerIndicator() != null) {
            //noinspection ConstantConditions
            getIndicatorController().offerIndicator().show();
        }
        return this;
    }

    private EventInterceptor getInterceptor() {
        if (this.mEventInterceptor != null) {
            return this.mEventInterceptor;
        }
        if (mIVideo instanceof VideoImpl) {
            this.mEventInterceptor = (EventInterceptor) this.mIVideo;
            return this.mEventInterceptor;
        }
        return null;
    }

    private IVideo getIVideo() {
        if (mIVideo == null) {
            mIVideo = new VideoImpl(mActivity, getWebCreator().getWebView());
        }
        return mIVideo;
    }

    @SuppressWarnings("ConstantConditions")
    private android.webkit.WebViewClient getWebViewClient() {
        Timber.i("getDelegate:" + this.mMiddleWrareWebClientBaseHeader);
        top.xuqingquan.web.system.DefaultWebClient mDefaultWebClient = top.xuqingquan.web.system.DefaultWebClient
                .createBuilder()
                .setActivity(this.mActivity)
                .setClient(mWebViewClient)
                .setWebClientHelper(this.mWebClientHelper)
                .setPermissionInterceptor(this.mPermissionInterceptor)
                .setWebView(getWebCreator().getWebView())
                .setInterceptUnkownUrl(this.mIsInterceptUnkownUrl)
                .setUrlHandleWays(this.mUrlHandleWays)
                .build();
        top.xuqingquan.web.system.MiddlewareWebClientBase header = this.mMiddleWrareWebClientBaseHeader;
        if (header != null) {
            top.xuqingquan.web.system.MiddlewareWebClientBase tail = header;
            int count = 1;
            top.xuqingquan.web.system.MiddlewareWebClientBase tmp = header;
            while (tmp.next() != null) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultWebClient);
            return header;
        } else {
            return mDefaultWebClient;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private com.tencent.smtt.sdk.WebViewClient getX5WebViewClient() {
        Timber.i("getDelegate:" + this.mX5MiddleWrareWebClientBaseHeader);
        top.xuqingquan.web.x5.DefaultWebClient mDefaultWebClient = top.xuqingquan.web.x5.DefaultWebClient
                .createBuilder()
                .setActivity(this.mActivity)
                .setClient(mX5WebViewClient)
                .setWebClientHelper(this.mWebClientHelper)
                .setPermissionInterceptor(this.mPermissionInterceptor)
                .setWebView(getX5WebCreator().getWebView())
                .setInterceptUnkownUrl(this.mIsInterceptUnkownUrl)
                .setUrlHandleWays(this.mUrlHandleWays)
                .build();
        top.xuqingquan.web.x5.MiddlewareWebClientBase header = this.mX5MiddleWrareWebClientBaseHeader;
        if (header != null) {
            top.xuqingquan.web.x5.MiddlewareWebClientBase tail = header;
            int count = 1;
            top.xuqingquan.web.x5.MiddlewareWebClientBase tmp = header;
            while (tmp.next() != null) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultWebClient);
            return header;
        } else {
            return mDefaultWebClient;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private AgentWeb ready() {
        AgentWebConfig.initCookiesManager(mActivity.getApplicationContext());
        if (WebConfig.enableTbs()) {
            top.xuqingquan.web.x5.IAgentWebSettings mAgentWebSettings = this.mX5AgentWebSettings;
            if (mAgentWebSettings == null) {
                this.mX5AgentWebSettings = top.xuqingquan.web.x5.AgentWebSettingsImpl.getInstance();
                mAgentWebSettings = this.mX5AgentWebSettings;
            }
            if (mAgentWebSettings instanceof top.xuqingquan.web.x5.AbsAgentWebSettings) {
                ((top.xuqingquan.web.x5.AbsAgentWebSettings) mAgentWebSettings).bindAgentWeb(this);
                if (mX5WebListenerManager == null) {
                    mX5WebListenerManager = (top.xuqingquan.web.x5.WebListenerManager) mAgentWebSettings;
                }
            }
            com.tencent.smtt.sdk.WebView webView = getX5WebCreator().getWebView();
            mAgentWebSettings.toSetting(webView);
            if (mJsInterfaceHolder == null) {
                mJsInterfaceHolder = top.xuqingquan.web.x5.JsInterfaceHolderImpl.getJsInterfaceHolder(mX5WebCreator);
            }
            Timber.i("mJavaObjects:" + mJavaObjects.size());
            if (mJavaObjects != null && !mJavaObjects.isEmpty()) {
                mJsInterfaceHolder.addJavaObjects(mJavaObjects);
            }
            if (mX5WebListenerManager != null) {
                mX5WebListenerManager.setDownloader(webView, null);
                mX5WebListenerManager.setWebChromeClient(webView, getX5ChromeClient());
                mX5WebListenerManager.setWebViewClient(webView, getX5WebViewClient());
            }
        } else {
            top.xuqingquan.web.system.IAgentWebSettings mAgentWebSettings = this.mAgentWebSettings;
            if (mAgentWebSettings == null) {
                this.mAgentWebSettings = top.xuqingquan.web.system.AgentWebSettingsImpl.getInstance();
                mAgentWebSettings = this.mAgentWebSettings;
            }
            if (mAgentWebSettings instanceof top.xuqingquan.web.system.AbsAgentWebSettings) {
                ((top.xuqingquan.web.system.AbsAgentWebSettings) mAgentWebSettings).bindAgentWeb(this);
                if (mWebListenerManager == null) {
                    mWebListenerManager = (top.xuqingquan.web.system.WebListenerManager) mAgentWebSettings;
                }
            }
            android.webkit.WebView webView = getWebCreator().getWebView();
            mAgentWebSettings.toSetting(webView);
            if (mJsInterfaceHolder == null) {
                mJsInterfaceHolder = top.xuqingquan.web.system.JsInterfaceHolderImpl.getJsInterfaceHolder(mWebCreator);
            }
            Timber.i("mJavaObjects:" + mJavaObjects.size());
            if (mJavaObjects != null && !mJavaObjects.isEmpty()) {
                mJsInterfaceHolder.addJavaObjects(mJavaObjects);
            }
            if (mWebListenerManager != null) {
                mWebListenerManager.setDownloader(webView, null);
                mWebListenerManager.setWebChromeClient(webView, getChromeClient());
                mWebListenerManager.setWebViewClient(webView, getWebViewClient());
            }
        }
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    private android.webkit.WebChromeClient getChromeClient() {
        this.mIndicatorController = (this.mIndicatorController == null) ?
                IndicatorHandler.getInstance().inJectIndicator(getWebCreator().offer())
                : this.mIndicatorController;
        this.mIVideo = getIVideo();
        top.xuqingquan.web.system.DefaultChromeClient mDefaultChromeClient =
                new top.xuqingquan.web.system.DefaultChromeClient(this.mActivity,
                        this.mIndicatorController,
                        mWebChromeClient, this.mIVideo,
                        this.mPermissionInterceptor, getWebCreator().getWebView());
        Timber.i("WebChromeClient:" + this.mWebChromeClient);
        top.xuqingquan.web.system.MiddlewareWebChromeBase header = this.mMiddlewareWebChromeBaseHeader;
        if (header != null) {
            top.xuqingquan.web.system.MiddlewareWebChromeBase tail = header;
            int count = 1;
            top.xuqingquan.web.system.MiddlewareWebChromeBase tmp = header;
            for (; tmp.next() != null; ) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultChromeClient);
            return header;
        } else {
            return mDefaultChromeClient;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private com.tencent.smtt.sdk.WebChromeClient getX5ChromeClient() {
        this.mIndicatorController = (this.mIndicatorController == null) ?
                IndicatorHandler.getInstance().inJectIndicator(getX5WebCreator().offer())
                : this.mIndicatorController;
        top.xuqingquan.web.x5.DefaultChromeClient mDefaultChromeClient =
                new top.xuqingquan.web.x5.DefaultChromeClient(this.mActivity,
                        this.mIndicatorController,
                        mX5WebChromeClient,
                        this.mPermissionInterceptor, getX5WebCreator().getWebView());
        Timber.i("WebChromeClient:" + this.mX5WebChromeClient);
        top.xuqingquan.web.x5.MiddlewareWebChromeBase header = this.mX5MiddlewareWebChromeBaseHeader;
        if (header != null) {
            top.xuqingquan.web.x5.MiddlewareWebChromeBase tail = header;
            int count = 1;
            top.xuqingquan.web.x5.MiddlewareWebChromeBase tmp = header;
            for (; tmp.next() != null; ) {
                tmp = tmp.next();
                tail = tmp;
                count++;
            }
            Timber.i("MiddlewareWebClientBase middleware count:" + count);
            tail.setDelegate(mDefaultChromeClient);
            return header;
        } else {
            return mDefaultChromeClient;
        }
    }

    public static final class PreAgentWeb {
        private AgentWeb mAgentWeb;
        private boolean isReady = false;

        PreAgentWeb(AgentWeb agentWeb) {
            this.mAgentWeb = agentWeb;
        }

        @SuppressWarnings("UnusedReturnValue")
        PreAgentWeb ready() {
            if (!isReady) {
                mAgentWeb.ready();
                isReady = true;
            }
            return this;
        }

        public AgentWeb get() {
            ready();
            return mAgentWeb;
        }

        public AgentWeb go(@Nullable String url) {
            if (!isReady) {
                ready();
            }
            return mAgentWeb.go(url);
        }
    }

    public static final class AgentBuilder {
        private Activity mActivity;
        private ViewGroup mViewGroup;
        private int mIndex = -1;
        private BaseIndicatorView mBaseIndicatorView;
        private IndicatorController mIndicatorController = null;
        /*默认进度条是显示的*/
        private boolean mEnableIndicator = true;
        private ViewGroup.LayoutParams mLayoutParams = null;
        private android.webkit.WebViewClient mWebViewClient;
        private com.tencent.smtt.sdk.WebViewClient mX5WebViewClient;
        private android.webkit.WebChromeClient mWebChromeClient;
        private com.tencent.smtt.sdk.WebChromeClient mX5WebChromeClient;
        private int mIndicatorColor = -1;
        private top.xuqingquan.web.system.IAgentWebSettings mAgentWebSettings;
        private top.xuqingquan.web.x5.IAgentWebSettings mX5AgentWebSettings;
        private top.xuqingquan.web.system.WebCreator mWebCreator;
        private top.xuqingquan.web.x5.WebCreator mX5WebCreator;
        private HttpHeaders mHttpHeaders = null;
        private IEventHandler mIEventHandler;
        private int mHeight = -1;
        private ArrayMap<String, Object> mJavaObject;
        private android.webkit.WebView mWebView;
        private com.tencent.smtt.sdk.WebView mX5WebView;
        private boolean mWebClientHelper = true;
        private top.xuqingquan.web.system.IWebLayout mWebLayout = null;
        private top.xuqingquan.web.x5.IWebLayout mX5WebLayout = null;
        private PermissionInterceptor mPermissionInterceptor = null;
        private AbsAgentWebUIController mAgentWebUIController;
        private OpenOtherPageWays mOpenOtherPage = null;
        private boolean mIsInterceptUnkownUrl = false;
        private top.xuqingquan.web.system.MiddlewareWebClientBase mMiddlewareWebClientBaseHeader;
        private top.xuqingquan.web.x5.MiddlewareWebClientBase mX5MiddlewareWebClientBaseHeader;
        private top.xuqingquan.web.system.MiddlewareWebClientBase mMiddlewareWebClientBaseTail;
        private top.xuqingquan.web.x5.MiddlewareWebClientBase mX5MiddlewareWebClientBaseTail;
        private top.xuqingquan.web.system.MiddlewareWebChromeBase mChromeMiddleWareHeader = null;
        private top.xuqingquan.web.x5.MiddlewareWebChromeBase mX5ChromeMiddleWareHeader = null;
        private top.xuqingquan.web.system.MiddlewareWebChromeBase mChromeMiddleWareTail = null;
        private top.xuqingquan.web.x5.MiddlewareWebChromeBase mX5ChromeMiddleWareTail = null;
        private View mErrorView;
        private int mErrorLayout;
        private int mReloadId;
        private int mTag;

        AgentBuilder(@NonNull Fragment fragment) {
            mActivity = fragment.getActivity();
            mTag = AgentWeb.FRAGMENT_TAG;
        }

        AgentBuilder(@NonNull Activity activity) {
            mActivity = activity;
            mTag = AgentWeb.ACTIVITY_TAG;
        }


        public IndicatorBuilder setAgentWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            return new IndicatorBuilder(this);
        }

        public IndicatorBuilder setAgentWebParent(@NonNull ViewGroup v, int index, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            this.mIndex = index;
            return new IndicatorBuilder(this);
        }

        private PreAgentWeb buildAgentWeb() {
            if (mTag == AgentWeb.FRAGMENT_TAG && this.mViewGroup == null) {
                throw new NullPointerException("ViewGroup is null,Please check your parameters .");
            }
            return new PreAgentWeb(HookManager.hookAgentWeb(new AgentWeb(this)));
        }

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null) {
                mJavaObject = new ArrayMap<>();
            }
            mJavaObject.put(key, o);
        }

        private void addHeader(String baseUrl, String k, String v) {
            if (mHttpHeaders == null) {
                mHttpHeaders = HttpHeaders.create();
            }
            mHttpHeaders.additionalHttpHeader(baseUrl, k, v);
        }

        private void addHeader(String baseUrl, Map<String, String> headers) {
            if (mHttpHeaders == null) {
                mHttpHeaders = HttpHeaders.create();
            }
            mHttpHeaders.additionalHttpHeaders(baseUrl, headers);
        }
    }

    public static class IndicatorBuilder {
        private AgentBuilder mAgentBuilder;

        IndicatorBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilder useDefaultIndicator(int color) {
            this.mAgentBuilder.mEnableIndicator = true;
            this.mAgentBuilder.mIndicatorColor = color;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder useDefaultIndicator() {
            this.mAgentBuilder.mEnableIndicator = true;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder closeIndicator() {
            this.mAgentBuilder.mEnableIndicator = false;
            this.mAgentBuilder.mIndicatorColor = -1;
            this.mAgentBuilder.mHeight = -1;
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder setCustomIndicator(@Nullable BaseIndicatorView v) {
            if (v != null) {
                this.mAgentBuilder.mEnableIndicator = true;
                this.mAgentBuilder.mBaseIndicatorView = v;
            } else {
                this.mAgentBuilder.mEnableIndicator = true;
            }
            return new CommonBuilder(mAgentBuilder);
        }

        public CommonBuilder useDefaultIndicator(@ColorInt int color, int height_dp) {
            this.mAgentBuilder.mIndicatorColor = color;
            this.mAgentBuilder.mHeight = height_dp;
            return new CommonBuilder(this.mAgentBuilder);
        }
    }

    public static class CommonBuilder {
        private AgentBuilder mAgentBuilder;

        CommonBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public CommonBuilder setEventHanadler(@Nullable IEventHandler iEventHandler) {
            mAgentBuilder.mIEventHandler = iEventHandler;
            return this;
        }

        public CommonBuilder closeWebViewClientHelper() {
            mAgentBuilder.mWebClientHelper = false;
            return this;
        }

        public CommonBuilder setWebChromeClient(@Nullable android.webkit.WebChromeClient webChromeClient) {
            this.mAgentBuilder.mWebChromeClient = webChromeClient;
            return this;
        }

        public CommonBuilder setWebChromeClient(@Nullable com.tencent.smtt.sdk.WebChromeClient webChromeClient) {
            this.mAgentBuilder.mX5WebChromeClient = webChromeClient;
            return this;
        }

        public CommonBuilder setWebViewClient(@Nullable android.webkit.WebViewClient webViewClient) {
            this.mAgentBuilder.mWebViewClient = webViewClient;
            return this;
        }

        public CommonBuilder setWebViewClient(@Nullable com.tencent.smtt.sdk.WebViewClient webViewClient) {
            this.mAgentBuilder.mX5WebViewClient = webViewClient;
            return this;
        }

        public CommonBuilder useMiddlewareWebClient(@Nullable top.xuqingquan.web.system.MiddlewareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            this.mAgentBuilder.mMiddlewareWebClientBaseTail = middleWrareWebClientBase;
            if (this.mAgentBuilder.mMiddlewareWebClientBaseHeader == null) {
                this.mAgentBuilder.mMiddlewareWebClientBaseHeader = middleWrareWebClientBase;
            } else {
                this.mAgentBuilder.mMiddlewareWebClientBaseTail.enq(middleWrareWebClientBase);
            }
            return this;
        }

        public CommonBuilder useMiddlewareWebClient(@Nullable top.xuqingquan.web.x5.MiddlewareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            this.mAgentBuilder.mX5MiddlewareWebClientBaseTail = middleWrareWebClientBase;
            if (this.mAgentBuilder.mX5MiddlewareWebClientBaseHeader == null) {
                this.mAgentBuilder.mX5MiddlewareWebClientBaseHeader = middleWrareWebClientBase;
            } else {
                this.mAgentBuilder.mX5MiddlewareWebClientBaseTail.enq(middleWrareWebClientBase);
            }
            return this;
        }

        public CommonBuilder useMiddlewareWebChrome(@Nullable top.xuqingquan.web.system.MiddlewareWebChromeBase middlewareWebChromeBase) {
            if (middlewareWebChromeBase == null) {
                return this;
            }
            this.mAgentBuilder.mChromeMiddleWareTail = middlewareWebChromeBase;
            if (this.mAgentBuilder.mChromeMiddleWareHeader == null) {
                this.mAgentBuilder.mChromeMiddleWareHeader = middlewareWebChromeBase;
            } else {
                this.mAgentBuilder.mChromeMiddleWareTail.enq(middlewareWebChromeBase);
            }
            return this;
        }

        public CommonBuilder useMiddlewareWebChrome(@Nullable top.xuqingquan.web.x5.MiddlewareWebChromeBase middlewareWebChromeBase) {
            if (middlewareWebChromeBase == null) {
                return this;
            }
            this.mAgentBuilder.mX5ChromeMiddleWareTail = middlewareWebChromeBase;
            if (this.mAgentBuilder.mX5ChromeMiddleWareHeader == null) {
                this.mAgentBuilder.mX5ChromeMiddleWareHeader = middlewareWebChromeBase;
            } else {
                this.mAgentBuilder.mX5ChromeMiddleWareTail.enq(middlewareWebChromeBase);
            }
            return this;
        }

        public CommonBuilder setMainFrameErrorView(@NonNull View view) {
            this.mAgentBuilder.mErrorView = view;
            return this;
        }

        public CommonBuilder setMainFrameErrorView(@LayoutRes int errorLayout, @IdRes int clickViewId) {
            this.mAgentBuilder.mErrorLayout = errorLayout;
            this.mAgentBuilder.mReloadId = clickViewId;
            return this;
        }

        public CommonBuilder setAgentWebWebSettings(@Nullable top.xuqingquan.web.system.IAgentWebSettings agentWebSettings) {
            this.mAgentBuilder.mAgentWebSettings = agentWebSettings;
            return this;
        }

        public CommonBuilder setAgentWebWebSettings(@Nullable top.xuqingquan.web.x5.IAgentWebSettings agentWebSettings) {
            this.mAgentBuilder.mX5AgentWebSettings = agentWebSettings;
            return this;
        }

        public PreAgentWeb createAgentWeb() {
            return this.mAgentBuilder.buildAgentWeb();
        }


        public CommonBuilder addJavascriptInterface(@NonNull String name, @NonNull Object o) {
            this.mAgentBuilder.addJavaObject(name, o);
            return this;
        }

        public CommonBuilder setWebView(@Nullable android.webkit.WebView webView) {
            this.mAgentBuilder.mWebView = webView;
            return this;
        }

        public CommonBuilder setWebView(@Nullable com.tencent.smtt.sdk.WebView webView) {
            this.mAgentBuilder.mX5WebView = webView;
            return this;
        }

        public CommonBuilder setWebLayout(@Nullable top.xuqingquan.web.system.IWebLayout iWebLayout) {
            this.mAgentBuilder.mWebLayout = iWebLayout;
            return this;
        }

        public CommonBuilder setWebLayout(@Nullable top.xuqingquan.web.x5.IWebLayout iWebLayout) {
            this.mAgentBuilder.mX5WebLayout = iWebLayout;
            return this;
        }

        public CommonBuilder additionalHttpHeader(String baseUrl, String k, String v) {
            this.mAgentBuilder.addHeader(baseUrl, k, v);
            return this;
        }

        public CommonBuilder additionalHttpHeader(String baseUrl, Map<String, String> headers) {
            this.mAgentBuilder.addHeader(baseUrl, headers);
            return this;
        }

        public CommonBuilder setPermissionInterceptor(@Nullable PermissionInterceptor permissionInterceptor) {
            this.mAgentBuilder.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public CommonBuilder setAgentWebUIController(@Nullable AbsAgentWebUIController agentWebUIController) {
            this.mAgentBuilder.mAgentWebUIController = agentWebUIController;
            return this;
        }

        public CommonBuilder setOpenOtherPageWays(@Nullable OpenOtherPageWays openOtherPageWays) {
            this.mAgentBuilder.mOpenOtherPage = openOtherPageWays;
            return this;
        }

        public CommonBuilder interceptUnkownUrl() {
            this.mAgentBuilder.mIsInterceptUnkownUrl = true;
            return this;
        }
    }

    public Activity getActivity() {
        return this.mActivity;
    }

    public static final class PermissionInterceptorWrapper implements PermissionInterceptor {

        private WeakReference<PermissionInterceptor> mWeakReference;

        private PermissionInterceptorWrapper(PermissionInterceptor permissionInterceptor) {
            this.mWeakReference = new WeakReference<>(permissionInterceptor);
        }

        @Override
        public boolean intercept(String url, @NonNull String[] permissions, @NonNull String action) {
            if (this.mWeakReference.get() == null) {
                return false;
            }
            return mWeakReference.get().intercept(url, permissions, action);
        }
    }
}
