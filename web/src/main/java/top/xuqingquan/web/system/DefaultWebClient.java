package top.xuqingquan.web.system;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alipay.sdk.app.PayTask;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.nokernel.PermissionInterceptor;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.web.nokernel.WebUtils;
import top.xuqingquan.web.publics.AbsAgentWebUIController;
import top.xuqingquan.web.publics.AgentWebUtils;

public class DefaultWebClient extends MiddlewareWebClientBase {
    /**
     * Activity's WeakReference
     */
    private WeakReference<Activity> mWeakReference;
    /**
     * 缩放
     */
    private static final int CONSTANTS_ABNORMAL_BIG = 7;
    /**
     * WebViewClient
     */
    private WebViewClient mWebViewClient;
    /**
     * mWebClientHelper
     */
    private boolean webClientHelper;
    /**
     * intent ' s scheme
     */
    private static final String INTENT_SCHEME = "intent://";
    /**
     * Wechat pay scheme ，用于唤醒微信支付
     */
    private static final String WEBCHAT_PAY_SCHEME = "weixin://wap/pay?";
    /**
     * 支付宝
     */
    private static final String ALIPAYS_SCHEME = "alipays://";
    /**
     * http scheme
     */
    private static final String HTTP_SCHEME = "http://";
    /**
     * https scheme
     */
    private static final String HTTPS_SCHEME = "https://";
    /**
     * true 表示当前应用内依赖了 alipay library , false  反之
     */
    private static final boolean HAS_ALIPAY_LIB;
    /**
     * 默认为咨询用户
     */
    private int mUrlHandleWays;
    /**
     * 是否拦截找不到相应页面的Url，默认拦截
     */
    private boolean mIsInterceptUnknownUrl;
    /**
     * AbsAgentWebUIController
     */
    private WeakReference<AbsAgentWebUIController> mAgentWebUIController;
    /**
     * WebView
     */
    private WebView mWebView;
    /**
     * Alipay PayTask 对象
     */
    private Object mPayTask;
    /**
     * SMS scheme
     */
    private static final String SCHEME_SMS = "sms:";
    /**
     * 缓存当前出现错误的页面
     */
    private Set<String> mErrorUrlsSet = new HashSet<>();
    /**
     * 缓存等待加载完成的页面 onPageStart()执行之后 ，onPageFinished()执行之前
     */
    private Set<String> mWaitingFinishSet = new HashSet<>();

    static {
        boolean tag = true;
        try {
            Class.forName("com.alipay.sdk.app.PayTask");
        } catch (Throwable ignore) {
            tag = false;
        }
        HAS_ALIPAY_LIB = tag;
        Timber.i("HAS_ALIPAY_LIB:" + HAS_ALIPAY_LIB);
    }

    private DefaultWebClient(Builder builder) {
        super(builder.mClient);
        this.mWebView = builder.mWebView;
        this.mWebViewClient = builder.mClient;
        this.mWeakReference = new WeakReference<>(builder.mActivity);
        this.webClientHelper = builder.mWebClientHelper;
        this.mAgentWebUIController = new WeakReference<>(AgentWebUtils.getAgentWebUIControllerByWebView(builder.mWebView));
        this.mIsInterceptUnknownUrl = builder.mIsInterceptUnkownScheme;
        if (builder.mUrlHandleWays <= 0) {
            mUrlHandleWays = WebConfig.ASK_USER_OPEN_OTHER_PAGE;
        } else {
            mUrlHandleWays = builder.mUrlHandleWays;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (url.startsWith(HTTP_SCHEME) || url.startsWith(HTTPS_SCHEME)) {
            return (webClientHelper && HAS_ALIPAY_LIB && isAlipay(view, url));
        }
        if (!webClientHelper) {
            return super.shouldOverrideUrlLoading(view, request);
        }
        if (handleCommonLink(url)) {
            return true;
        }
        // intent
        if (url.startsWith(INTENT_SCHEME)) {
            handleIntentUrl(url);
            Timber.i("intent url ");
            return true;
        }
        // 微信支付
        if (url.startsWith(WEBCHAT_PAY_SCHEME)) {
            Timber.i("lookup wechat to pay ~~");
            startActivity(url);
            return true;
        }
        if (url.startsWith(ALIPAYS_SCHEME) && lookup(url)) {
            Timber.i("alipays url lookup alipay ~~ ");
            return true;
        }
        if (queryActivitiesNumber(url) > 0 && deepLink(url)) {
            Timber.i("intercept OtherAppScheme");
            return true;
        }
        if (mIsInterceptUnknownUrl) {
            Timber.i("intercept UnknownUrl :" + request.getUrl());
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    private boolean deepLink(String url) {
        switch (mUrlHandleWays) {
            // 直接打开其他App
            case WebConfig.DERECT_OPEN_OTHER_PAGE:
                lookup(url);
                return true;
            // 咨询用户是否打开其他App
            case WebConfig.ASK_USER_OPEN_OTHER_PAGE:
                Activity mActivity = mWeakReference.get();
                if (mActivity == null) {
                    return false;
                }
                ResolveInfo resolveInfo = lookupResolveInfo(url);
                if (null == resolveInfo) {
                    return false;
                }
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                Timber.e("resolve package:" + resolveInfo.activityInfo.packageName + " app package:" + mActivity.getPackageName());
                if (!TextUtils.isEmpty(activityInfo.packageName) && activityInfo.packageName.equals(mActivity.getPackageName())) {
                    return lookup(url);
                }
                if (mAgentWebUIController.get() != null) {
                    mAgentWebUIController.get()
                            .onOpenPagePrompt(this.mWebView,
                                    mWebView.getUrl(),
                                    msg -> {
                                        if (msg.what == 1) {
                                            lookup(url);
                                        }
                                        return true;
                                    });
                }
                return true;
            // 默认不打开
            default:
                return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    @Deprecated
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(HTTP_SCHEME) || url.startsWith(HTTPS_SCHEME)) {
            return (webClientHelper && HAS_ALIPAY_LIB && isAlipay(view, url));
        }
        if (!webClientHelper) {
            return false;
        }
        //电话 ， 邮箱 ， 短信
        if (handleCommonLink(url)) {
            return true;
        }
        //Intent scheme
        if (url.startsWith(INTENT_SCHEME)) {
            handleIntentUrl(url);
            return true;
        }
        //微信支付
        if (url.startsWith(WEBCHAT_PAY_SCHEME)) {
            startActivity(url);
            return true;
        }
        //支付宝
        if (url.startsWith(ALIPAYS_SCHEME) && lookup(url)) {
            return true;
        }
        //打开url 相对应的页面
        if (queryActivitiesNumber(url) > 0 && deepLink(url)) {
            Timber.i("intercept OtherAppScheme");
            return true;
        }
        // 手机里面没有页面能匹配到该链接 ，拦截下来。
        if (mIsInterceptUnknownUrl) {
            Timber.i("intercept InterceptUnknownScheme : " + url);
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    private int queryActivitiesNumber(String url) {
        try {
            if (mWeakReference.get() == null) {
                return 0;
            }
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            PackageManager mPackageManager = mWeakReference.get().getPackageManager();
            List<ResolveInfo> mResolveInfos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return mResolveInfos.size();
        } catch (Throwable t) {
            Timber.e(t);
            return 0;
        }
    }

    private void handleIntentUrl(String intentUrl) {
        try {
            if (TextUtils.isEmpty(intentUrl) || !intentUrl.startsWith(INTENT_SCHEME)) {
                return;
            }
            lookup(intentUrl);
        } catch (Throwable e) {
            Timber.e(e);
        }
    }

    private ResolveInfo lookupResolveInfo(String url) {
        try {
            Intent intent;
            Activity mActivity = mWeakReference.get();
            if (mActivity == null) {
                return null;
            }
            PackageManager packageManager = mActivity.getPackageManager();
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
        return null;
    }

    private boolean lookup(String url) {
        try {
            Intent intent;
            Activity mActivity = mWeakReference.get();
            if (mActivity == null) {
                return true;
            }
            PackageManager packageManager = mActivity.getPackageManager();
            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            // 跳到该应用
            if (info != null) {
                mActivity.startActivity(intent);
                return true;
            }
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
        return false;
    }

    private boolean isAlipay(final WebView view, String url) {
        try {
            Activity mActivity = mWeakReference.get();
            if (mActivity == null) {
                return false;
            }
            /*
             * 推荐采用的新的二合一接口(payInterceptorWithUrl),只需调用一次
             */
            if (mPayTask == null) {
                mPayTask = new PayTask(mActivity);
            }
            final PayTask task = (PayTask) mPayTask;
            boolean isIntercepted = task.payInterceptorWithUrl(url, true, result -> {
                final String url1 = result.getReturnUrl();
                if (!TextUtils.isEmpty(url1)) {
                    WebUtils.runInUiThread(() -> view.loadUrl(url1));
                }
            });
            Timber.i("alipay-isIntercepted:" + isIntercepted + "  url:" + url);
            return isIntercepted;
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
        return false;
    }

    private boolean handleCommonLink(String url) {
        if (url.startsWith(WebView.SCHEME_TEL)
                || url.startsWith(SCHEME_SMS)
                || url.startsWith(WebView.SCHEME_MAILTO)
                || url.startsWith(WebView.SCHEME_GEO)) {
            try {
                Activity mActivity = mWeakReference.get();
                if (mActivity == null) {
                    return false;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(intent);
            } catch (Throwable throwable) {
                Timber.e(throwable);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        //noinspection RedundantCollectionOperation
        if (!mWaitingFinishSet.contains(url)) {
            mWaitingFinishSet.add(url);
        }
        super.onPageStarted(view, url, favicon);

    }

    /**
     * MainFrame Error
     */
    @Override
    @Deprecated
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Timber.i("onReceivedError：" + description + "  CODE:" + errorCode);
        onMainFrameError(view, errorCode, description, failingUrl);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request.isForMainFrame()) {
            onMainFrameError(view,
                    error.getErrorCode(), error.getDescription().toString(),
                    request.getUrl().toString());
        }
        Timber.i("onReceivedError:" + error.getDescription() + " code:" + error.getErrorCode());
    }

    private void onMainFrameError(WebView view, int errorCode, String description, String failingUrl) {
        mErrorUrlsSet.add(failingUrl);
        // 下面逻辑判断开发者是否重写了 onMainFrameError 方法 ， 优先交给开发者处理
        if (this.mWebViewClient != null && webClientHelper) {
            /*
             * MainFrameErrorMethod
             */
            Method mMethod = WebUtils.isExistMethod(mWebViewClient, "onMainFrameError", AbsAgentWebUIController.class, WebView.class, int.class, String.class, String.class);
            if (mMethod != null) {
                try {
                    mMethod.invoke(this.mWebViewClient, mAgentWebUIController.get(), view, errorCode, description, failingUrl);
                } catch (Throwable throwable) {
                    Timber.e(throwable);
                }
                return;
            }
        }
        if (mAgentWebUIController.get() != null) {
            mAgentWebUIController.get().onMainFrameError(view, errorCode, description, failingUrl);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (!mErrorUrlsSet.contains(url) && mWaitingFinishSet.contains(url)) {
            if (mAgentWebUIController.get() != null) {
                mAgentWebUIController.get().onShowMainFrame();
            }
        } else {
            view.setVisibility(View.VISIBLE);
        }
        //noinspection RedundantCollectionOperation
        if (mWaitingFinishSet.contains(url)) {
            mWaitingFinishSet.remove(url);
        }
        if (!mErrorUrlsSet.isEmpty()) {
            mErrorUrlsSet.clear();
        }
        super.onPageFinished(view, url);
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return super.shouldOverrideKeyEvent(view, event);
    }

    private void startActivity(String url) {
        try {
            if (mWeakReference.get() == null) {
                return;
            }
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mWeakReference.get().startActivity(intent);

        } catch (Throwable e) {
            Timber.e(e);
        }
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        Timber.i("onScaleChanged:" + oldScale + "   n:" + newScale);
        if (newScale - oldScale > CONSTANTS_ABNORMAL_BIG) {
            view.setInitialScale((int) (oldScale / newScale * 100));
        }
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Activity mActivity;
        private WebViewClient mClient;
        private boolean mWebClientHelper;
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private PermissionInterceptor mPermissionInterceptor;
        private WebView mWebView;
        private boolean mIsInterceptUnkownScheme;
        private int mUrlHandleWays;

        public Builder setActivity(Activity activity) {
            this.mActivity = activity;
            return this;
        }

        public Builder setClient(WebViewClient client) {
            this.mClient = client;
            return this;
        }

        public Builder setWebClientHelper(boolean webClientHelper) {
            this.mWebClientHelper = webClientHelper;
            return this;
        }

        public Builder setPermissionInterceptor(PermissionInterceptor permissionInterceptor) {
            this.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public Builder setWebView(WebView webView) {
            this.mWebView = webView;
            return this;
        }

        public Builder setInterceptUnkownUrl(boolean interceptUnkownScheme) {
            this.mIsInterceptUnkownScheme = interceptUnkownScheme;
            return this;
        }

        public Builder setUrlHandleWays(int urlHandleWays) {
            this.mUrlHandleWays = urlHandleWays;
            return this;
        }

        public DefaultWebClient build() {
            return new DefaultWebClient(this);
        }
    }
}
