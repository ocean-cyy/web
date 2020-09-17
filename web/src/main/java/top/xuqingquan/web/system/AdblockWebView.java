package top.xuqingquan.web.system;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
import top.xuqingquan.web.nokernel.AdblockCallback;
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
    private AdblockWebViewClient intWebViewClient;
    private AdblockWebChromeClient intWebChromeClient;
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
    private AdblockCallback adblockCallback;
    private boolean isDebug;

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
        logd("addDomListener=" + value);
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
        intWebChromeClient.setDelegate(client);
        applyAdblockEnabled();
    }

    private String readScriptFile(String filename) throws IOException {
        return Utils
                .readAssetAsString(getContext(), filename)
                .replace(BRIDGE_TOKEN, BRIDGE)
                .replace(DEBUG_TOKEN, (isDebug() ? "" : "//"));
    }

    private void runScript(String script) {
        logd("runScript started");
        evaluateJavascript(script, null);
        logd("runScript finished");
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

    private class AdblockWebChromeClient extends MiddlewareWebChromeBase {

        @Override
        public void onProgressChanged(@Nullable WebView view, int newProgress) {
            logd("Loading progress=" + newProgress + "%");

            // addDomListener is changed to 'false' in `setAddDomListener` invoked from injected JS
            if (getAddDomListener() && loadError == null && injectJs != null) {
                logd("Injecting script");
                runScript(injectJs);

                if (allowDraw && loading) {
                    startPreventDrawing();
                }
            }

            // workaround for the issue: https://issues.adblockplus.org/ticket/5303
            if (newProgress == 100 && !allowDraw) {
                logw("Workaround for the issue #5303");
                stopPreventDrawing();
            }

            if (extWebChromeClient != null) {
                extWebChromeClient.onProgressChanged(view, newProgress);
            } else {
                super.onProgressChanged(view, newProgress);
            }
        }
    }

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

    public boolean isDebug() {
        return isDebug && WebConfig.DEBUG;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        extWebViewClient = client;
        intWebViewClient.setDelegate(client);
        applyAdblockEnabled();
    }

    private void clearReferrers() {
        logd("Clearing referrers");
        url2Referrer.clear();
    }

    /**
     * WebViewClient for API 21 and newer
     * (has Referrer since it overrides `shouldInterceptRequest(..., request)` with referrer)
     */
    private class AdblockWebViewClient extends MiddlewareWebClientBase {

        @Override
        public void onPageStarted(@Nullable WebView view, @Nullable String url, @Nullable Bitmap favicon) {
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
        public void onPageFinished(@Nullable WebView view, @Nullable String url) {
            loading = false;
            if (extWebViewClient != null) {
                extWebViewClient.onPageFinished(view, url);
            } else {
                super.onPageFinished(view, url);
            }
        }

        @Override
        public void onReceivedError(@Nullable WebView view, int errorCode, @Nullable String description, @Nullable String failingUrl) {
            loge("Load error:" +
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

        protected WebResourceResponse shouldInterceptRequest(
                WebView webview, String url, boolean isMainFrame,
                boolean isXmlHttpRequest, String[] referrerChainArray) {
            synchronized (provider.getEngineLock()) {
                // if dispose() was invoke, but the page is still loading then just let it go
                if (provider.getCounter() == 0) {
                    loge("FilterEngine already disposed, allow loading");

                    // allow loading by returning null
                    return null;
                } else {
                    provider.waitForReady();
                }

                if (isMainFrame) {
                    // never blocking main frame requests, just subrequests
                    logw(url + " is main frame, allow loading");

                    // allow loading by returning null
                    return null;
                }

                // whitelisted
                if (provider.getEngine().isDomainWhitelisted(url, referrerChainArray)) {
                    logw(url + " domain is whitelisted, allow loading");

                    // allow loading by returning null
                    return null;
                }

                if (provider.getEngine().isDocumentWhitelisted(url, referrerChainArray)) {
                    logw(url + " document is whitelisted, allow loading");

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
                    logw("Blocked loading " + url);
                    if (adblockCallback != null) {
                        adblockCallback.addBlockCount(url);
                    }
                    // if we should block, return empty response which results in 'errorLoading' callback
                    return new WebResourceResponse("text/plain", "UTF-8", null);
                }

                logd("Allowed loading " + url);

                // continue by returning null
                return null;
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(@Nullable WebView view, @Nullable WebResourceRequest request) {
            if (view == null || request == null) {
                if (extWebViewClient != null) {
                    return extWebViewClient.shouldInterceptRequest(view, request);
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            }
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
                logd("Header referrer for " + url + " is " + referrer);
                url2Referrer.put(url, referrer);

                referrers = new String[]
                        {
                                referrer
                        };
            } else {
                logw("No referrer header for " + url);
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
        intWebViewClient = new AdblockWebViewClient();
        intWebChromeClient = new AdblockWebChromeClient();
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
                        logw("FilterEngine already disposed");
                        selectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                    } else {
                        provider.waitForReady();
                        String[] referrers = new String[]{url};

                        List<Subscription> subscriptions = provider
                                .getEngine()
                                .getFilterEngine()
                                .getListedSubscriptions();

                        try {
                            logd("Listed subscriptions: " + subscriptions.size());
                            if (isDebug()) {
                                for (Subscription eachSubscription : subscriptions) {
                                    logd("Subscribed to "
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
                            loge("Failed to extract domain from " + url);
                            selectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                        } else {
                            logd("Requesting elemhide selectors from AdblockEngine for " + url + " in " + this);
                            List<String> selectors = provider
                                    .getEngine()
                                    .getElementHidingSelectors(url, domain, referrers);

                            logd("Finished requesting elemhide selectors, got " + selectors.size() + " in " + this);
                            selectorsString = Utils.stringListToJsonArray(selectors);
                        }
                    }
                } finally {
                    if (isCancelled.get()) {
                        logw("This thread is cancelled, exiting silently " + this);
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
                logd("Setting elemhide string " + result.length() + " bytes");
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
            logw("Cancelling elemhide thread " + this);
            if (isFinished.get()) {
                logw("This thread is finished, exiting silently " + this);
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
                logw("elemHideThread set to null");
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
                    getContext(), AdblockEngine.BASE_PATH_DIRECTORY, isDebug()));
        }
    }

    private void startAbpLoading(String newUrl) {
        logd("Start loading " + newUrl);

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
            loge("Failed to read script", e);
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
        logd("Stop abp loading");

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
                logd("Scheduled 'allow drawing' invocation in " + allowDrawDelay + " ms");
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
            logw("Prevent drawing");
            drawEmptyPage(canvas);
        }
    }

    private void drawEmptyPage(Canvas canvas) {
        // assuming default color is WHITE
        canvas.drawColor(Color.WHITE);
    }

    protected void startPreventDrawing() {
        logw("Start prevent drawing");

        allowDraw = false;
    }

    protected void stopPreventDrawing() {
        logd("Stop prevent drawing, invalidating");

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
                logd("Waiting for elemhide selectors to be ready");
                elemHideLatch.await();
                logd("Elemhide selectors ready, " + elemHideSelectorsString.length() + " bytes");

                clearReferrers();

                return elemHideSelectorsString;
            } catch (InterruptedException e) {
                logw("Interrupted, returning empty selectors list");
                return EMPTY_ELEMHIDE_ARRAY_STRING;
            }
        }
    }

    private void doDispose() {
        logw("Disposing AdblockEngine");
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
        logd("Dispose invoked");

        if (provider == null) {
            logd("No internal AdblockEngineProvider created");
            return;
        }

        stopLoading();

        AdblockWebView.DisposeRunnable disposeRunnable = new AdblockWebView.DisposeRunnable(disposeFinished);
        synchronized (elemHideThreadLockObject) {
            if (elemHideThread != null) {
                logw("Busy with elemhide selectors, delayed disposing scheduled");
                elemHideThread.setFinishedRunnable(disposeRunnable);
            } else {
                disposeRunnable.run();
            }
        }
    }

    public void setAdblockCallback(AdblockCallback adblockCallback) {
        this.adblockCallback = adblockCallback;
    }

    public void logd(String msg) {
        if (isDebug()) {
            Timber.d(msg);
        }
    }

    public void logw(String msg) {
        if (isDebug()) {
            Timber.w(msg);
        }
    }

    public void loge(String msg) {
        if (isDebug()) {
            Timber.e(msg);
        }
    }

    public void loge(String msg, Throwable t) {
        if (isDebug()) {
            Timber.e(msg, t);
        }
    }

}
