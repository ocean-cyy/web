package top.xuqingquan.web.system;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.Subscription;
import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.AdblockEngineProvider;
import org.adblockplus.libadblockplus.android.SingleInstanceEngineProvider;
import org.adblockplus.libadblockplus.android.Utils;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.nokernel.WebConfig;

/**
 * Created by 许清泉 on 2020/8/29 23:41
 */
@SuppressWarnings("all")
public class AdblockWebView extends AgentWebView {

    /**
     * Default (in some conditions) start redraw delay after DOM modified with injected JS (millis)
     */
    public static final int ALLOW_DRAW_DELAY = 200;
  /*
     The value could be different for devices and completely unclear why we need it and
     how to measure actual value
  */

    protected static final String HEADER_REFERRER = "Referer";
    protected static final String HEADER_REQUESTED_WITH = "X-Requested-With";
    protected static final String HEADER_REQUESTED_WITH_XMLHTTPREQUEST = "XMLHttpRequest";

    private static final String BRIDGE_TOKEN = "{{BRIDGE}}";
    private static final String DEBUG_TOKEN = "{{DEBUG}}";
    private static final String HIDE_TOKEN = "{{HIDE}}";
    private static final String BRIDGE = "jsBridge";
    private static final String[] EMPTY_ARRAY = {};
    private static final String EMPTY_ELEMHIDE_ARRAY_STRING = "[]";

    private static final Pattern RE_JS = Pattern.compile("\\.js$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_CSS = Pattern.compile("\\.css$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_IMAGE = Pattern.compile("\\.(?:gif|png|jpe?g|bmp|ico)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_FONT = Pattern.compile("\\.(?:ttf|woff)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_HTML = Pattern.compile("\\.html?$", Pattern.CASE_INSENSITIVE);

    private volatile boolean addDomListener = true;
    private boolean adblockEnabled = true;
    private AdblockEngineProvider provider;
    private Integer loadError;
    private int allowDrawDelay = ALLOW_DRAW_DELAY;
    private WebChromeClient extWebChromeClient;
    private WebViewClient extWebViewClient;
    private WebViewClient intWebViewClient;
    private Map<String, String> url2Referrer = Collections.synchronizedMap(new HashMap<>());
    private String url;
    private String injectJs;
    private CountDownLatch elemHideLatch;
    private String elemHideSelectorsString;
    private Object elemHideThreadLockObject = new Object();
    private AdblockWebView.ElemHideThread elemHideThread;
    private boolean loading;
    private volatile boolean elementsHidden = false;
    private final Handler handler = new Handler();

    // used to prevent user see flickering for elements to hide
    // for some reason it's rendered even if element is hidden on 'dom ready' event
    private volatile boolean allowDraw = true;

    public AdblockWebView(Context context) {
        super(context);
        initAbp();
    }

    public AdblockWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAbp();
    }

    public AdblockWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAbp();
    }

    /**
     * Warning: do not rename (used in injected JS by method name)
     *
     * @param value set if one need to set DOM listener
     */
    @JavascriptInterface
    public void setAddDomListener(boolean value) {
        Timber.d("addDomListener=" + value);
        this.addDomListener = value;
    }

    @JavascriptInterface
    public boolean getAddDomListener() {
        return addDomListener;
    }

    public boolean isAdblockEnabled() {
        return adblockEnabled;
    }

    private void applyAdblockEnabled() {
        super.setWebViewClient(adblockEnabled ? intWebViewClient : extWebViewClient);
        super.setWebChromeClient(adblockEnabled ? intWebChromeClient : extWebChromeClient);
    }

    public void setAdblockEnabled(boolean adblockEnabled) {
        this.adblockEnabled = adblockEnabled;
        applyAdblockEnabled();
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        extWebChromeClient = client;
        applyAdblockEnabled();
    }

    private String readScriptFile(String filename) throws IOException {
        return Utils
                .readAssetAsString(getContext(), filename)
                .replace(BRIDGE_TOKEN, BRIDGE)
                .replace(DEBUG_TOKEN, (WebConfig.DEBUG ? "" : "//"));
    }

    private void runScript(String script) {
        Timber.d("runScript started");
        evaluateJavascript(script, null);
        Timber.d("runScript finished");
    }

    public void setProvider(final AdblockEngineProvider provider) {
        if (this.provider != null && provider != null && this.provider == provider) {
            return;
        }

        final Runnable setRunnable = new Runnable() {
            @Override
            public void run() {
                AdblockWebView.this.provider = provider;
                if (AdblockWebView.this.provider != null) {
                    AdblockWebView.this.provider.retain(true); // asynchronously
                }
            }
        };

        if (this.provider != null) {
            // as adblockEngine can be busy with elemhide thread we need to use callback
            this.dispose(setRunnable);
        } else {
            setRunnable.run();
        }
    }

    private WebChromeClient intWebChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onReceivedTitle(view, title);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onReceivedIcon(view, icon);
            }
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onShowCustomView(view, callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
            }
        }

        @Override
        public void onHideCustomView() {
            if (extWebChromeClient != null) {
                extWebChromeClient.onHideCustomView();
            }
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                      Message resultMsg) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            } else {
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }
        }

        @Override
        public void onRequestFocus(WebView view) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onRequestFocus(view);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onCloseWindow(window);
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onJsAlert(view, url, message, result);
            } else {
                return super.onJsAlert(view, url, message, result);
            }
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onJsConfirm(view, url, message, result);
            } else {
                return super.onJsConfirm(view, url, message, result);
            }
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                                  JsPromptResult result) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
            } else {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onJsBeforeUnload(view, url, message, result);
            } else {
                return super.onJsBeforeUnload(view, url, message, result);
            }
        }

        @Override
        public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
                                            long estimatedDatabaseSize, long totalQuota,
                                            WebStorage.QuotaUpdater quotaUpdater) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                        estimatedDatabaseSize, totalQuota, quotaUpdater);
            } else {
                super.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                        estimatedDatabaseSize, totalQuota, quotaUpdater);
            }
        }

        @Override
        public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
                                             WebStorage.QuotaUpdater quotaUpdater) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            } else {
                super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
            } else {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            if (extWebChromeClient != null) {
                extWebChromeClient.onGeolocationPermissionsHidePrompt();
            } else {
                super.onGeolocationPermissionsHidePrompt();
            }
        }

        @Override
        public boolean onJsTimeout() {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onJsTimeout();
            } else {
                return super.onJsTimeout();
            }
        }

        @Override
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            if (extWebChromeClient != null) {
                extWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
            } else {
                super.onConsoleMessage(message, lineNumber, sourceID);
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Timber.d("JS: level=" + consoleMessage.messageLevel()
                    + ", message=\"" + consoleMessage.message() + "\""
                    + ", sourceId=\"" + consoleMessage.sourceId() + "\""
                    + ", line=" + consoleMessage.lineNumber());

            if (extWebChromeClient != null) {
                return extWebChromeClient.onConsoleMessage(consoleMessage);
            } else {
                return super.onConsoleMessage(consoleMessage);
            }
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            if (extWebChromeClient != null) {
                return extWebChromeClient.getDefaultVideoPoster();
            } else {
                return super.getDefaultVideoPoster();
            }
        }

        @Override
        public View getVideoLoadingProgressView() {
            if (extWebChromeClient != null) {
                return extWebChromeClient.getVideoLoadingProgressView();
            } else {
                return super.getVideoLoadingProgressView();
            }
        }

        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            if (extWebChromeClient != null) {
                extWebChromeClient.getVisitedHistory(callback);
            } else {
                super.getVisitedHistory(callback);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            Timber.d("Loading progress=" + newProgress + "%");

            // addDomListener is changed to 'false' in `setAddDomListener` invoked from injected JS
            if (getAddDomListener() && loadError == null && injectJs != null) {
                Timber.d("Injecting script");
                runScript(injectJs);

                if (allowDraw && loading) {
                    startPreventDrawing();
                }
            }

            // workaround for the issue: https://issues.adblockplus.org/ticket/5303
            if (newProgress == 100 && !allowDraw) {
                Timber.w("Workaround for the issue #5303");
                stopPreventDrawing();
            }

            if (extWebChromeClient != null) {
                extWebChromeClient.onProgressChanged(view, newProgress);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            if (extWebChromeClient != null) {
                return extWebChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            } else {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        }
    };

    public int getAllowDrawDelay() {
        return allowDrawDelay;
    }

    /**
     * Set start redraw delay after DOM modified with injected JS
     * (used to prevent flickering after 'DOM ready')
     *
     * @param allowDrawDelay delay (in millis)
     */
    public void setAllowDrawDelay(int allowDrawDelay) {
        if (allowDrawDelay < 0) {
            throw new IllegalArgumentException("Negative value is not allowed");
        }

        this.allowDrawDelay = allowDrawDelay;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        extWebViewClient = client;
        applyAdblockEnabled();
    }

    private void clearReferrers() {
        Timber.d("Clearing referrers");
        url2Referrer.clear();
    }

    /**
     * WebViewClient for API 21 and newer
     * (has Referrer since it overrides `shouldInterceptRequest(..., request)` with referrer)
     */
    private class AdblockWebViewClient extends MiddlewareWebClientBase {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (extWebViewClient != null) {
                return extWebViewClient.shouldOverrideUrlLoading(view, url);
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (loading) {
                stopAbpLoading();
            }

            startAbpLoading(url);

            if (extWebViewClient != null) {
                extWebViewClient.onPageStarted(view, url, favicon);
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            loading = false;
            if (extWebViewClient != null) {
                extWebViewClient.onPageFinished(view, url);
            } else {
                super.onPageFinished(view, url);
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            if (extWebViewClient != null) {
                extWebViewClient.onLoadResource(view, url);
            } else {
                super.onLoadResource(view, url);
            }
        }

        @Override
        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
            if (extWebViewClient != null) {
                extWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
            } else {
                super.onTooManyRedirects(view, cancelMsg, continueMsg);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Timber.e("Load error:" +
                    " code=" + errorCode +
                    " with description=" + description +
                    " for url=" + failingUrl);
            loadError = errorCode;

            stopAbpLoading();

            if (extWebViewClient != null) {
                extWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            if (extWebViewClient != null) {
                extWebViewClient.onFormResubmission(view, dontResend, resend);
            } else {
                super.onFormResubmission(view, dontResend, resend);
            }
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            if (extWebViewClient != null) {
                extWebViewClient.doUpdateVisitedHistory(view, url, isReload);
            } else {
                super.doUpdateVisitedHistory(view, url, isReload);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (extWebViewClient != null) {
                extWebViewClient.onReceivedSslError(view, handler, error);
            } else {
                super.onReceivedSslError(view, handler, error);
            }
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            if (extWebViewClient != null) {
                extWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
            } else {
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            if (extWebViewClient != null) {
                return extWebViewClient.shouldOverrideKeyEvent(view, event);
            } else {
                return super.shouldOverrideKeyEvent(view, event);
            }
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if (extWebViewClient != null) {
                extWebViewClient.onUnhandledKeyEvent(view, event);
            } else {
                super.onUnhandledKeyEvent(view, event);
            }
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            if (extWebViewClient != null) {
                extWebViewClient.onScaleChanged(view, oldScale, newScale);
            } else {
                super.onScaleChanged(view, oldScale, newScale);
            }
        }

        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
            if (extWebViewClient != null) {
                extWebViewClient.onReceivedLoginRequest(view, realm, account, args);
            } else {
                super.onReceivedLoginRequest(view, realm, account, args);
            }
        }

        protected WebResourceResponse shouldInterceptRequest(
                WebView webview, String url, boolean isMainFrame,
                boolean isXmlHttpRequest, String[] referrerChainArray) {
            synchronized (provider.getEngineLock()) {
                // if dispose() was invoke, but the page is still loading then just let it go
                if (provider.getCounter() == 0) {
                    Timber.e("FilterEngine already disposed, allow loading");

                    // allow loading by returning null
                    return null;
                } else {
                    provider.waitForReady();
                }

                if (isMainFrame) {
                    // never blocking main frame requests, just subrequests
                    Timber.w(url + " is main frame, allow loading");

                    // allow loading by returning null
                    return null;
                }

                // whitelisted
                if (provider.getEngine().isDomainWhitelisted(url, referrerChainArray)) {
                    Timber.w(url + " domain is whitelisted, allow loading");

                    // allow loading by returning null
                    return null;
                }

                if (provider.getEngine().isDocumentWhitelisted(url, referrerChainArray)) {
                    Timber.w(url + " document is whitelisted, allow loading");

                    // allow loading by returning null
                    return null;
                }

                // determine the content
                FilterEngine.ContentType contentType;
                if (isXmlHttpRequest) {
                    contentType = FilterEngine.ContentType.XMLHTTPREQUEST;
                } else {
                    if (RE_JS.matcher(url).find()) {
                        contentType = FilterEngine.ContentType.SCRIPT;
                    } else if (RE_CSS.matcher(url).find()) {
                        contentType = FilterEngine.ContentType.STYLESHEET;
                    } else if (RE_IMAGE.matcher(url).find()) {
                        contentType = FilterEngine.ContentType.IMAGE;
                    } else if (RE_FONT.matcher(url).find()) {
                        contentType = FilterEngine.ContentType.FONT;
                    } else if (RE_HTML.matcher(url).find()) {
                        contentType = FilterEngine.ContentType.SUBDOCUMENT;
                    } else {
                        contentType = FilterEngine.ContentType.OTHER;
                    }
                }

                // check if we should block
                if (provider.getEngine().matches(url, contentType, referrerChainArray)) {
                    Timber.w("Blocked loading " + url);

                    // if we should block, return empty response which results in 'errorLoading' callback
                    return new WebResourceResponse("text/plain", "UTF-8", null);
                }

                Timber.d("Allowed loading " + url);

                // continue by returning null
                return null;
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // here we just trying to fill url -> referrer map
            // blocking/allowing loading will happen in `shouldInterceptRequest(WebView,String)`
            String url = request.getUrl().toString();

            boolean isXmlHttpRequest =
                    request.getRequestHeaders().containsKey(HEADER_REQUESTED_WITH) &&
                            HEADER_REQUESTED_WITH_XMLHTTPREQUEST.equals(
                                    request.getRequestHeaders().get(HEADER_REQUESTED_WITH));

            String referrer = request.getRequestHeaders().get(HEADER_REFERRER);
            String[] referrers;

            if (referrer != null) {
                Timber.d("Header referrer for " + url + " is " + referrer);
                url2Referrer.put(url, referrer);

                referrers = new String[]
                        {
                                referrer
                        };
            } else {
                Timber.w("No referrer header for " + url);
                referrers = EMPTY_ARRAY;
            }

            return shouldInterceptRequest(view, url, request.isForMainFrame(), isXmlHttpRequest, referrers);
        }
    }

    private void initAbp() {
        addJavascriptInterface(this, BRIDGE);
        initClients();
        setProvider(AdblockHelper.get().getProvider());
    }

    private void initClients() {
        intWebViewClient = new AdblockWebView.AdblockWebViewClient();
        applyAdblockEnabled();
    }

    private class ElemHideThread extends Thread {
        private String selectorsString;
        private CountDownLatch finishedLatch;
        private AtomicBoolean isFinished;
        private AtomicBoolean isCancelled;

        public ElemHideThread(CountDownLatch finishedLatch) {
            this.finishedLatch = finishedLatch;
            isFinished = new AtomicBoolean(false);
            isCancelled = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            synchronized (provider.getEngineLock()) {
                try {
                    if (provider.getCounter() == 0) {
                        Timber.w("FilterEngine already disposed");
                        selectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                    } else {
                        provider.waitForReady();
                        String[] referrers = new String[]
                                {
                                        url
                                };

                        List<Subscription> subscriptions = provider
                                .getEngine()
                                .getFilterEngine()
                                .getListedSubscriptions();

                        try {
                            Timber.d("Listed subscriptions: " + subscriptions.size());
                            if (WebConfig.DEBUG) {
                                for (Subscription eachSubscription : subscriptions) {
                                    Timber.d("Subscribed to "
                                            + (eachSubscription.isDisabled() ? "disabled" : "enabled")
                                            + " " + eachSubscription);
                                }
                            }
                        } finally {
                            for (Subscription eachSubscription : subscriptions) {
                                eachSubscription.dispose();
                            }
                        }

                        final String domain = provider.getEngine().getFilterEngine().getHostFromURL(url);
                        if (domain == null) {
                            Timber.e("Failed to extract domain from " + url);
                            selectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                        } else {
                            Timber.d("Requesting elemhide selectors from AdblockEngine for " + url + " in " + this);
                            List<String> selectors = provider
                                    .getEngine()
                                    .getElementHidingSelectors(url, domain, referrers);

                            Timber.d("Finished requesting elemhide selectors, got " + selectors.size() + " in " + this);
                            selectorsString = Utils.stringListToJsonArray(selectors);
                        }
                    }
                } finally {
                    if (isCancelled.get()) {
                        Timber.w("This thread is cancelled, exiting silently " + this);
                    } else {
                        finish(selectorsString);
                    }
                }
            }
        }

        private void onFinished() {
            finishedLatch.countDown();
            synchronized (finishedRunnableLockObject) {
                if (finishedRunnable != null) {
                    finishedRunnable.run();
                }
            }
        }

        private void finish(String result) {
            isFinished.set(true);
            if (result != null) {
                Timber.d("Setting elemhide string " + result.length() + " bytes");
            }
            elemHideSelectorsString = result;
            onFinished();
        }

        private final Object finishedRunnableLockObject = new Object();
        private Runnable finishedRunnable;

        public void setFinishedRunnable(Runnable runnable) {
            synchronized (finishedRunnableLockObject) {
                this.finishedRunnable = runnable;
            }
        }

        public void cancel() {
            Timber.w("Cancelling elemhide thread " + this);
            if (isFinished.get()) {
                Timber.w("This thread is finished, exiting silently " + this);
            } else {
                isCancelled.set(true);
                finish(EMPTY_ELEMHIDE_ARRAY_STRING);
            }
        }
    }

    private Runnable elemHideThreadFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (elemHideThreadLockObject) {
                Timber.w("elemHideThread set to null");
                elemHideThread = null;
            }
        }
    };

    private void initAbpLoading() {
        getSettings().setJavaScriptEnabled(true);
        buildInjectJs();
        ensureProvider();
    }

    private void ensureProvider() {
        // if AdblockWebView works as drop-in replacement for WebView 'provider' is not set.
        // Thus AdblockWebView is using SingleInstanceEngineProvider instance
        if (provider == null) {
            setProvider(new SingleInstanceEngineProvider(
                    getContext(), AdblockEngine.BASE_PATH_DIRECTORY, WebConfig.DEBUG));
        }
    }

    private void startAbpLoading(String newUrl) {
        Timber.d("Start loading " + newUrl);

        loading = true;
        addDomListener = true;
        elementsHidden = false;
        loadError = null;
        url = newUrl;

        if (url != null) {
            elemHideLatch = new CountDownLatch(1);
            synchronized (elemHideThreadLockObject) {
                elemHideThread = new AdblockWebView.ElemHideThread(elemHideLatch);
                elemHideThread.setFinishedRunnable(elemHideThreadFinishedRunnable);
                elemHideThread.start();
            }
        } else {
            elemHideLatch = null;
        }
    }

    private void buildInjectJs() {
        try {
            if (injectJs == null) {
                injectJs = readScriptFile("inject.js").replace(HIDE_TOKEN, readScriptFile("css.js"));
            }
        } catch (IOException e) {
            Timber.e("Failed to read script", e);
        }
    }

    @Override
    public void goBack() {
        if (loading) {
            stopAbpLoading();
        }

        super.goBack();
    }

    @Override
    public void goForward() {
        if (loading) {
            stopAbpLoading();
        }

        super.goForward();
    }

    @Override
    public void reload() {
        initAbpLoading();

        if (loading) {
            stopAbpLoading();
        }

        super.reload();
    }

    @Override
    public void loadUrl(String url) {
        initAbpLoading();

        if (loading) {
            stopAbpLoading();
        }

        super.loadUrl(url);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        initAbpLoading();

        if (loading) {
            stopAbpLoading();
        }

        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        initAbpLoading();

        if (loading) {
            stopAbpLoading();
        }

        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                    String historyUrl) {
        initAbpLoading();

        if (loading) {
            stopAbpLoading();
        }

        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    public void stopLoading() {
        stopAbpLoading();
        super.stopLoading();
    }

    private void stopAbpLoading() {
        Timber.d("Stop abp loading");

        loading = false;
        stopPreventDrawing();
        clearReferrers();

        synchronized (elemHideThreadLockObject) {
            if (elemHideThread != null) {
                elemHideThread.cancel();
            }
        }
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public void setElementsHidden(boolean value) {
        // invoked with 'true' by JS callback when DOM is loaded
        elementsHidden = value;

        // fired on worker thread, but needs to be invoked on main thread
        if (value) {
//     handler.post(allowDrawRunnable);
//     should work, but it's not working:
//     the user can see element visible even though it was hidden on dom event

            if (allowDrawDelay > 0) {
                Timber.d("Scheduled 'allow drawing' invocation in " + allowDrawDelay + " ms");
            }
            handler.postDelayed(allowDrawRunnable, allowDrawDelay);
        }
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public boolean isElementsHidden() {
        return elementsHidden;
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(allowDrawRunnable);
        super.onPause();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (allowDraw) {
            super.onDraw(canvas);
        } else {
            Timber.w("Prevent drawing");
            drawEmptyPage(canvas);
        }
    }

    private void drawEmptyPage(Canvas canvas) {
        // assuming default color is WHITE
        canvas.drawColor(Color.WHITE);
    }

    protected void startPreventDrawing() {
        Timber.w("Start prevent drawing");

        allowDraw = false;
    }

    protected void stopPreventDrawing() {
        Timber.d("Stop prevent drawing, invalidating");

        allowDraw = true;
        invalidate();
    }

    private Runnable allowDrawRunnable = new Runnable() {
        @Override
        public void run() {
            stopPreventDrawing();
        }
    };

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public String getElemhideSelectors() {
        if (elemHideLatch == null) {
            return EMPTY_ELEMHIDE_ARRAY_STRING;
        } else {
            try {
                // elemhide selectors list getting is started in startAbpLoad() in background thread
                Timber.d("Waiting for elemhide selectors to be ready");
                elemHideLatch.await();
                Timber.d("Elemhide selectors ready, " + elemHideSelectorsString.length() + " bytes");

                clearReferrers();

                return elemHideSelectorsString;
            } catch (InterruptedException e) {
                Timber.w("Interrupted, returning empty selectors list");
                return EMPTY_ELEMHIDE_ARRAY_STRING;
            }
        }
    }

    private void doDispose() {
        Timber.w("Disposing AdblockEngine");
        provider.release();
    }

    private class DisposeRunnable implements Runnable {
        private Runnable disposeFinished;

        private DisposeRunnable(Runnable disposeFinished) {
            this.disposeFinished = disposeFinished;
        }

        @Override
        public void run() {
            doDispose();

            if (disposeFinished != null) {
                disposeFinished.run();
            }
        }
    }

    /**
     * Dispose AdblockWebView and internal adblockEngine if it was created
     * If external AdblockEngine was passed using `setAdblockEngine()` it should be disposed explicitly
     * Warning: runnable can be invoked from background thread
     *
     * @param disposeFinished runnable to run when AdblockWebView is disposed
     */
    public void dispose(final Runnable disposeFinished) {
        Timber.d("Dispose invoked");

        if (provider == null) {
            Timber.d("No internal AdblockEngineProvider created");
            return;
        }

        stopLoading();

        AdblockWebView.DisposeRunnable disposeRunnable = new AdblockWebView.DisposeRunnable(disposeFinished);
        synchronized (elemHideThreadLockObject) {
            if (elemHideThread != null) {
                Timber.w("Busy with elemhide selectors, delayed disposing scheduled");
                elemHideThread.setFinishedRunnable(disposeRunnable);
            } else {
                disposeRunnable.run();
            }
        }
    }
}