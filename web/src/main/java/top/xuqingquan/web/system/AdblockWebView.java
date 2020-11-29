package top.xuqingquan.web.system;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.adblockplus.libadblockplus.FilterEngine;
import org.adblockplus.libadblockplus.HttpClient;
import org.adblockplus.libadblockplus.Subscription;
import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.AdblockEngineProvider;
import org.adblockplus.libadblockplus.android.SingleInstanceEngineProvider;
import org.adblockplus.libadblockplus.android.Utils;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;
import org.adblockplus.libadblockplus.sitekey.PublicKeyHolderImpl;
import org.adblockplus.libadblockplus.sitekey.SiteKeysConfiguration;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.nokernel.AbpShouldBlockResult;
import top.xuqingquan.web.nokernel.EventsListener;
import top.xuqingquan.web.nokernel.OptionalBoolean;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.web.system.adblock.CombinedSiteKeyExtractor;
import top.xuqingquan.web.system.adblock.SiteKeyExtractor;
import top.xuqingquan.web.system.adblock.content_type.ContentTypeDetector;
import top.xuqingquan.web.system.adblock.content_type.HeadersContentTypeDetector;
import top.xuqingquan.web.system.adblock.content_type.OrderedContentTypeDetector;
import top.xuqingquan.web.system.adblock.content_type.UrlFileExtensionTypeDetector;

/**
 * Created by 许清泉 on 2020/8/29 23:41
 */
@SuppressWarnings("all")
public final class AdblockWebView extends AgentWebView {
    private static final String ASSETS_CHARSET_NAME = "UTF-8";
    private static final String BRIDGE_TOKEN = "{{BRIDGE}}";
    private static final String DEBUG_TOKEN = "{{DEBUG}}";
    private static final String HIDE_TOKEN = "{{HIDE}}";
    private static final String HIDDEN_TOKEN = "{{HIDDEN_FLAG}}";
    private static final String SITEKEY_EXTRACTED_TOKEN = "{{SITEKEY_EXTRACTED_FLAG}}";
    private static final String BRIDGE = "jsBridge";
    private static final String EMPTY_ELEMHIDE_STRING = "";
    private static final String EMPTY_ELEMHIDE_ARRAY_STRING = "[]";

    private OrderedContentTypeDetector contentTypeDetector;
    private final AtomicReference<AdblockEngineProvider> providerReference = new AtomicReference<>();
    private Integer loadError;
    private AdblockWebChromeClient intWebChromeClient;
    private AdblockWebViewClient intWebViewClient;
    private final Map<String, String> url2Referrer
            = Collections.synchronizedMap(new HashMap<String, String>());
    private final AtomicReference<String> navigationUrl = new AtomicReference<>();
    private String injectJs;
    private String elemhideBlockedJs;
    private CountDownLatch elemHideLatch;
    private final AtomicReference<OptionalBoolean> adblockEnabled =
            new AtomicReference<>(OptionalBoolean.UNDEFINED);
    private String elemHideSelectorsString;
    private String elemHideEmuSelectorsString;
    private final Object elemHideThreadLockObject = new Object();
    private ElemHideThread elemHideThread;
    private boolean loading;
    private String elementsHiddenFlag;
    private String sitekeyExtractedFlag;
    private SiteKeyExtractor siteKeyExtractor;
    private boolean isDebug;
    private final AtomicBoolean acceptCookie = new AtomicBoolean(true);
    private final AtomicBoolean acceptThirdPartyCookies = new AtomicBoolean(false);

    private final AtomicReference<EventsListener> eventsListenerAtomicReference = new AtomicReference<>();
    private final AtomicReference<SiteKeysConfiguration> siteKeysConfiguration = new AtomicReference<>();
    private final AdblockEngine.SettingsChangedListener engineSettingsChangedCb =
            new AdblockEngine.SettingsChangedListener() {
                @Override
                public void onEnableStateChanged(final boolean enabled) {
                    final OptionalBoolean newValue = OptionalBoolean.from(enabled);
                    final OptionalBoolean oldValue = adblockEnabled.getAndSet(newValue);
                    if (oldValue != OptionalBoolean.UNDEFINED && oldValue != newValue) {
                        logd("Filter Engine status changed, enable status is %s", newValue);
                        post(new Runnable() {
                            @Override
                            public void run() {
                                clearCache(true);
                            }
                        });
                    }
                }
            };
    private final AdblockEngineProvider.EngineCreatedListener engineCreatedCb = new AdblockEngineProvider.EngineCreatedListener() {
        @Override
        public void onAdblockEngineCreated(final AdblockEngine engine) {
            adblockEnabled.set(OptionalBoolean.from(engine.isEnabled()));
            logd("Filter Engine created, enable status is %s", adblockEnabled.get());
            engine.addSettingsChangedListener(engineSettingsChangedCb);
        }
    };
    private final AdblockEngineProvider.EngineDisposedListener engineDisposedCb
            = new AdblockEngineProvider.EngineDisposedListener() {
        @Override
        public void onAdblockEngineDisposed() {
            adblockEnabled.set(OptionalBoolean.UNDEFINED);
        }
    };

    public AdblockWebView(final Context context) {
        super(context);
        initAbp();
    }

    public AdblockWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initAbp();
    }

    public AdblockWebView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initAbp();
    }

    private EventsListener getEventsListener() {
        return eventsListenerAtomicReference.get();
    }

    public void setSiteKeysConfiguration(final SiteKeysConfiguration siteKeysConfiguration) {
        this.siteKeysConfiguration.set(siteKeysConfiguration);
        siteKeyExtractor.setSiteKeysConfiguration(siteKeysConfiguration);
    }

    public SiteKeysConfiguration getSiteKeysConfiguration() {
        return siteKeysConfiguration.get();
    }

    /**
     * Sets an implementation of EventsListener which will receive ad blocking related events.
     *
     * @param eventsListener an implementation of EventsListener.
     */
    public void setEventsListener(final EventsListener eventsListener) {
        this.eventsListenerAtomicReference.set(eventsListener);
    }

    @Override
    public void setWebChromeClient(final WebChromeClient client) {
        intWebChromeClient.setDelegate(client);
    }

    @Override
    public void setWebViewClient(final WebViewClient client) {
        intWebViewClient.setDelegate(client);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initAbp() {
        addJavascriptInterface(this, BRIDGE);
        initRandom();
        buildInjectJs();
        getSettings().setJavaScriptEnabled(true);

        siteKeyExtractor = new CombinedSiteKeyExtractor(this);
        intWebChromeClient = new AdblockWebChromeClient(null);
        intWebViewClient = new AdblockWebViewClient(null);
        setProvider(AdblockHelper.get().getProvider());

        super.setWebChromeClient(intWebChromeClient);
        super.setWebViewClient(intWebViewClient);
    }

    private AdblockEngineProvider getProvider() {
        return providerReference.get();
    }

    private String readScriptFile(final String filename) throws IOException {
        return Utils
                .readAssetAsString(getContext(), filename, ASSETS_CHARSET_NAME)
                .replace(BRIDGE_TOKEN, BRIDGE)
                .replace(DEBUG_TOKEN, (isDebug() ? "" : "//"))
                .replace(HIDDEN_TOKEN, elementsHiddenFlag)
                .replace(SITEKEY_EXTRACTED_TOKEN, sitekeyExtractedFlag);
    }

    private void runScript(final String script) {
        logd("runScript started");
        evaluateJavascript(script, null);
        logd("runScript finished");
    }

    public void setProvider(final AdblockEngineProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        if (this.getProvider() == provider) {
            return;
        }

        final Runnable setRunnable = new Runnable() {
            @Override
            public void run() {
                AdblockWebView.this.providerReference.set(provider);
                final ReentrantReadWriteLock.ReadLock lock = provider.getReadEngineLock();
                final boolean locked = lock.tryLock();

                try {
                    // Note that if retain() needs to create a FilterEngine it will wait (in bg thread)
                    // until we finish this synchronized block and release the engine lock.
                    getProvider().retain(true); // asynchronously
                    if (locked && getProvider().getEngine() != null) {
                        adblockEnabled.set(OptionalBoolean.from(getProvider().getEngine().isEnabled()));
                        logd("Filter Engine already created, enable status is %s", adblockEnabled);
                        getProvider().getEngine().addSettingsChangedListener(engineSettingsChangedCb);
                    } else {
                        getProvider().addEngineCreatedListener(engineCreatedCb);
                        getProvider().addEngineDisposedListener(engineDisposedCb);
                    }
                } finally {
                    if (locked) {
                        lock.unlock();
                    }
                }
            }
        };

        if (this.getProvider() != null) {
            // as adblockEngine can be busy with elemhide thread we need to use callback
            this.dispose(setRunnable);
        } else {
            setRunnable.run();
        }
    }

    private class AdblockWebChromeClient extends MiddlewareWebChromeBase {

        public AdblockWebChromeClient(@Nullable WebChromeClient webChromeClient) {
            super(webChromeClient);
        }

        @Override
        public void onProgressChanged(final WebView view, final int newProgress) {
            logd("onProgressChanged to %d%% for url: %s", newProgress, view.getUrl());
            tryInjectJs();
            if (getDelegate() != null) {
                getDelegate().onProgressChanged(view, newProgress);
            } else {
                super.onProgressChanged(view, newProgress);
            }
        }
    }

    private void tryInjectJs() {
        if (adblockEnabled.get() != OptionalBoolean.TRUE) {
            return;
        }
        if (loadError == null && injectJs != null) {
            logd("Injecting script");
            runScript(injectJs);
        }
    }

    private void clearReferrers() {
        logd("Clearing referrers");
        url2Referrer.clear();
    }

    public static class WebResponseResult {
        // decisions
        public static final String RESPONSE_CHARSET_NAME = "UTF-8";
        public static final String RESPONSE_MIME_TYPE = "text/plain";

        public static final WebResourceResponse ALLOW_LOAD = null;
        public static final WebResourceResponse BLOCK_LOAD =
                new WebResourceResponse(RESPONSE_MIME_TYPE, RESPONSE_CHARSET_NAME, null);
    }

    /**
     * WebViewClient for API 21 and newer
     * (has Referrer since it overrides `shouldInterceptRequest(..., request)` with referrer)
     */
    private class AdblockWebViewClient extends MiddlewareWebClientBase {

        AdblockWebViewClient(final WebViewClient extWebViewClient) {
            super(extWebViewClient);
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            logd("onPageStarted called for url %s", url);
            if (loading) {
                stopAbpLoading();
            }
            startAbpLoading(url);
            notifyNavigation();
            if (getDelegate() != null) {
                getDelegate().onPageStarted(view, url, favicon);
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            logd("onPageFinished called for url %s", url);
            loading = false;
            if (getDelegate() != null) {
                getDelegate().onPageFinished(view, url);
            } else {
                super.onPageFinished(view, url);
            }
        }

        @Override
        public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
            loge("onReceivedError:" +
                            " code=%d" +
                            " with description=%s" +
                            " for url=%s",
                    errorCode, description, failingUrl);
            loadError = errorCode;

            if (getDelegate() != null) {
                getDelegate().onReceivedError(view, errorCode, description, failingUrl);
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {
            loge("onReceivedError:" +
                            " code=%d" +
                            " with description=%s" +
                            " for url=%s" +
                            " request.isForMainFrame()=%s",
                    error.getErrorCode(), error.getDescription(), request.getUrl(),
                    request.isForMainFrame());
            if (getDelegate() != null) {
                getDelegate().onReceivedError(view, request, error);
            } else {
                super.onReceivedError(view, request, error);
            }
        }

        private AbpShouldBlockResult notifyAndReturnBlockingResponse(final String requestUrl,
                                                                     final List<String> parentFrameUrls,
                                                                     final FilterEngine.ContentType contentType) {
            if (isVisibleResource(contentType)) {
                elemhideBlockedResource(requestUrl);
            }
            notifyResourceBlocked(new EventsListener.BlockedResourceInfo(requestUrl,
                    parentFrameUrls, contentType));
            return AbpShouldBlockResult.BLOCK_LOAD;
        }

        private AbpShouldBlockResult shouldAbpBlockRequest(final WebResourceRequest request) {
            // here we just trying to fill url -> referrer map
            final String url = request.getUrl().toString();
            final String urlWithoutFragment = Utils.getUrlWithoutFragment(url);

            final boolean isMainFrame = request.isForMainFrame();
            boolean isWhitelisted = false;
            boolean canContainSitekey = false;
            boolean isAcceptableAdsEnabled = true;

            final String referrer = request.getRequestHeaders().get(HttpClient.HEADER_REFERRER);

            final Lock lock = getProvider().getReadEngineLock();
            lock.lock();

            try {
                // if dispose() was invoke, but the page is still loading then just let it go
                boolean isDisposed = false;
                if (getProvider().getCounter() == 0) {
                    isDisposed = true;
                } else {
                    lock.unlock();
                    getProvider().waitForReady();
                    lock.lock();
                    if (getProvider().getCounter() == 0) {
                        isDisposed = true;
                    }
                }

                final AdblockEngine engine = getProvider().getEngine();

                // Apart from checking counter (getProvider().getCounter()) we also need to make sure
                // that getProvider().getEngine() is already set.
                // We check that under getProvider().getReadEngineLock(); so we are sure it will not be
                // changed after this check.
                if (isDisposed || engine == null) {
                    loge("FilterEngine already disposed");
                    return AbpShouldBlockResult.NOT_ENABLED;
                }

                if (adblockEnabled.get() == OptionalBoolean.UNDEFINED) {
                    loge("No adblockEnabled value");
                    return AbpShouldBlockResult.NOT_ENABLED;
                } else {
                    // check the real enable status and update adblockEnabled flag which is used
                    // later on to check if we should execute element hiding JS
                    final OptionalBoolean newValue = OptionalBoolean.from(engine.isEnabled());
                    adblockEnabled.set(newValue);
                    if (newValue == OptionalBoolean.FALSE) {
                        logd("adblockEnabled = false");
                        return AbpShouldBlockResult.NOT_ENABLED;
                    }
                }

                logd("Loading url %s", url);

                if (referrer != null) {
                    logd("Header referrer for " + url + " is " + referrer);
                    if (!url.equals(referrer)) {
                        url2Referrer.put(urlWithoutFragment, referrer);
                    } else {
                        logw("Header referrer value is the same as url, skipping url2Referrer.put()");
                    }
                } else {
                    logw("No referrer header for %s", url);
                }

                // reconstruct frames hierarchy
                final List<String> referrerChain = new ArrayList<>();
                String parent = urlWithoutFragment;
                while ((parent = url2Referrer.get(parent)) != null) {
                    if (referrerChain.contains(parent)) {
                        logw("Detected referrer loop, finished creating referrers list");
                        break;
                    }
                    referrerChain.add(0, parent);
                }

                isAcceptableAdsEnabled = engine.isAcceptableAdsEnabled();
                if (isMainFrame) {
                    // never blocking main frame requests, just subrequests
                    logw("%s is main frame, allow loading", url);
                    siteKeyExtractor.setEnabled(isAcceptableAdsEnabled);
                } else {
                    // Here we discover if referrerChain is empty or incomplete (i.e. does not contain the
                    // navigation url) so we add at least the top referrer which is navigationUrl.
                    final String navigationUrlLocal = navigationUrl.get();
                    if (!TextUtils.isEmpty(navigationUrlLocal) && (referrerChain.isEmpty() ||
                            !referrerChain.contains(navigationUrlLocal))) {
                        logd("Adding top level referrer `%s` for `%s`", navigationUrlLocal, url);
                        referrerChain.add(0, navigationUrlLocal);
                    }

                    final SiteKeysConfiguration siteKeysConfiguration = getSiteKeysConfiguration();
                    String siteKey = (siteKeysConfiguration != null
                            ? PublicKeyHolderImpl.stripPadding(siteKeysConfiguration.getPublicKeyHolder()
                            .getAny(referrerChain, ""))
                            : null);

                    // determine the content
                    FilterEngine.ContentType contentType =
                            ensureContentTypeDetectorCreatedAndGet().detect(request);

                    if (contentType == null) {
                        logw("contentTypeDetector didn't recognize content type");
                        contentType = FilterEngine.ContentType.OTHER;
                    }

                    if (contentType == FilterEngine.ContentType.SUBDOCUMENT && referrer != null) {
                        // Due to "strict-origin-when-cross-origin" referrer policy set as default starting
                        // Chromium 85 we have to fix the referrers chain with just "origin".
                        // See https://jira.eyeo.com/browse/DP-1621
                        try {
                            url2Referrer.put(Utils.getOrigin(url), referrer);
                        } catch (final MalformedURLException | IllegalArgumentException e) {
                            loge(e, "Failed to extract origin from %s", url);
                        }
                    }

                    // whitelisted
                    if (engine.isDocumentWhitelisted(url, referrerChain, siteKey)) {
                        isWhitelisted = true;
                        logw("%s document is whitelisted, allow loading", url);
                        notifyResourceWhitelisted(new EventsListener.WhitelistedResourceInfo(
                                url, referrerChain, EventsListener.WhitelistReason.DOCUMENT));
                    } else {
                        if (contentType == FilterEngine.ContentType.SUBDOCUMENT ||
                                contentType == FilterEngine.ContentType.OTHER) {
                            canContainSitekey = true;
                        }

                        boolean specificOnly = false;
                        if (!referrerChain.isEmpty()) {
                            final String parentUrl = referrerChain.get(0);
                            final List<String> referrerChainForGenericblock = referrerChain.subList(1,
                                    referrerChain.size());
                            specificOnly = engine.isGenericblockWhitelisted(parentUrl,
                                    referrerChainForGenericblock, siteKey);
                            if (specificOnly) {
                                logw("Found genericblock filter for url %s which parent is %s",
                                        url, parentUrl);
                            }
                        }

                        // check if we should block
                        AdblockEngine.MatchesResult result = engine.matches(
                                url, FilterEngine.ContentType.maskOf(contentType),
                                referrerChain, siteKey, specificOnly);

                        if (result == AdblockEngine.MatchesResult.NOT_WHITELISTED) {
                            logi("Attempting to block request with AA on the first try: %s", url);

                            // Need to run `waitForSitekeyCheck` to hold the actual check until
                            // the sitekey is either obtained or not present
                            final boolean waitedForSitekey = siteKeyExtractor.waitForSitekeyCheck(request);
                            if (waitedForSitekey) {
                                // Request was held, start over to see if it's now whitelisted
                                logi("Restarting the check having waited for the sitekey: %s", url);

                                siteKey = (siteKeysConfiguration != null
                                        ? PublicKeyHolderImpl.stripPadding(siteKeysConfiguration.getPublicKeyHolder()
                                        .getAny(referrerChain, ""))
                                        : null);

                                if (siteKey == null || siteKey.isEmpty()) {
                                    logi("SiteKey is not found, blocking the resource %s", url);
                                    return notifyAndReturnBlockingResponse(url, referrerChain, contentType);
                                }

                                if (engine.isDocumentWhitelisted(url, referrerChain, siteKey)) {
                                    isWhitelisted = true;
                                    logw("%s document is whitelisted, allow loading", url);
                                    notifyResourceWhitelisted(new EventsListener.WhitelistedResourceInfo(
                                            url, referrerChain, EventsListener.WhitelistReason.DOCUMENT));
                                } else {
                                    specificOnly = false;
                                    if (!referrerChain.isEmpty()) {
                                        final String parentUrl = referrerChain.get(0);
                                        final List<String> referrerChainForGenericblock = referrerChain.subList(1,
                                                referrerChain.size());
                                        specificOnly = engine.isGenericblockWhitelisted(parentUrl,
                                                referrerChainForGenericblock, siteKey);
                                        if (specificOnly) {
                                            logw("Found genericblock filter for url %s which parent is %s",
                                                    url, parentUrl);
                                        }
                                    }

                                    // check if we should block
                                    result = engine.matches(
                                            url, FilterEngine.ContentType.maskOf(contentType),
                                            referrerChain, siteKey, specificOnly);

                                    if (result == AdblockEngine.MatchesResult.NOT_WHITELISTED) {
                                        logi("Blocked loading %s with AA %s", url,
                                                isAcceptableAdsEnabled ? "enabled" : "disabled");
                                        return notifyAndReturnBlockingResponse(url, referrerChain, contentType);
                                    }
                                    if (result == AdblockEngine.MatchesResult.WHITELISTED) {
                                        isWhitelisted = true;
                                        logw("%s is whitelisted in matches()", url);
                                        notifyResourceWhitelisted(new EventsListener.WhitelistedResourceInfo(
                                                url, referrerChain, EventsListener.WhitelistReason.FILTER));
                                    }
                                    logd("Allowed loading %s", url);
                                }
                            } // if (waitedForSitekey)

                            // This check is required because the resource could be whitelisted on the second
                            // check after waiting for the sitekey check conclusion
                            if (!isWhitelisted) {
                                logi("Blocked loading %s with AA %s", url,
                                        isAcceptableAdsEnabled ? "enabled" : "disabled");
                                return notifyAndReturnBlockingResponse(url, referrerChain, contentType);
                            }
                        } else if (result == AdblockEngine.MatchesResult.WHITELISTED) {
                            isWhitelisted = true;
                            logw("%s is whitelisted in matches()", url);
                            notifyResourceWhitelisted(new EventsListener.WhitelistedResourceInfo(
                                    url, referrerChain, EventsListener.WhitelistReason.FILTER));
                        }
                        logd("Allowed loading %s", url);
                    }
                } // !MainFrame
            } finally {
                lock.unlock();
            }

            // we rely on calling `fetchUrlAndCheckSiteKey` later in `shouldInterceptRequest`, now we
            // just reply that it's fine to load the resource
            final SiteKeysConfiguration siteKeysConfiguration = getSiteKeysConfiguration();
            if ((
                    isAcceptableAdsEnabled
                            ||
                            (siteKeysConfiguration != null && siteKeysConfiguration.getForceChecks())
            )
                    &&
                    (
                            isMainFrame
                                    ||
                                    (canContainSitekey && !isWhitelisted)
                    )) {
                // if url is a main frame (whitelisted by default) or can contain by design a site key header
                // (it content type is SUBDOCUMENT or OTHER) and it is not yet whitelisted then we need to
                // make custom HTTP get request to try to obtain a site key header.
                return AbpShouldBlockResult.ALLOW_LOAD;
            }

            return AbpShouldBlockResult.ALLOW_LOAD_NO_SITEKEY_CHECK;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {
            if (request.isForMainFrame()) {
                logd("Updating navigationUrl to `%s`", request.getUrl().toString());
                navigationUrl.set(request.getUrl().toString());
            }
            final AbpShouldBlockResult abpBlockResult = shouldAbpBlockRequest(request);

            // if FilterEngine is unavailable or not enabled, just let it go (and skip sitekey check)
            if (AbpShouldBlockResult.NOT_ENABLED.equals(abpBlockResult)) {
                return WebResponseResult.ALLOW_LOAD;
            }

            // if url should be blocked, we are not performing any further actions
            if (AbpShouldBlockResult.BLOCK_LOAD.equals(abpBlockResult)) {
                return WebResponseResult.BLOCK_LOAD;
            }

            final Map<String, String> requestHeaders = request.getRequestHeaders();
            final String url = request.getUrl().toString();

            final WebViewClient extWebViewClient = getDelegate();
            if (extWebViewClient != null) {
                // allow external WebViewClient to perform and intercept requests
                // its fine to block shouldAbpBlockRequest and wait
                final WebResourceResponse externalResponse
                        = extWebViewClient.shouldInterceptRequest(view, request);

                // if we are having an external WebResourceResponse provided by external WebViewClient,
                // we will do the sitekey verification and just return the Response
                if (externalResponse != null) {
                    if (!AbpShouldBlockResult.ALLOW_LOAD_NO_SITEKEY_CHECK.equals(abpBlockResult)) {
                        logd("Verifying site keys with external shouldInterceptRequest response");
                        getSiteKeysConfiguration().getSiteKeyVerifier().verifyInHeaders(url,
                                requestHeaders,
                                externalResponse.getResponseHeaders());
                        logd("Finished verifying, returning external response and stop");
                    } else {
                        logd("Skipped verifying of the site keys with " +
                                "external shouldInterceptRequest response");
                    }
                    return externalResponse;
                }
            }

            // we don't need to make a HTTP GET request to check a site key header
            if (AbpShouldBlockResult.ALLOW_LOAD_NO_SITEKEY_CHECK.equals(abpBlockResult)) {
                return WebResponseResult.ALLOW_LOAD;
            }

            if (requestHeaders.containsKey(HttpClient.HEADER_REQUESTED_RANGE)) {
                logd("Skipping site key check for the request with a Range header");
                return WebResponseResult.ALLOW_LOAD;
            }

            return siteKeyExtractor.extract(request);
        }
    }

    public boolean canAcceptCookie(final String requestUrl, final String cookieString) {
        final String documentUrl = navigationUrl.get();
        if (documentUrl == null || requestUrl == null || cookieString == null) {
            return false;
        }
        if (!acceptCookie.get()) {
            return false;
        }
        if (!acceptThirdPartyCookies.get()) {
            return Utils.isFirstPartyCookie(documentUrl, requestUrl, cookieString);
        }
        return true;
    }

    // not a huge saving, but still nice to lazy init
    // since `contentTypeDetector` might not be used ever
    private ContentTypeDetector ensureContentTypeDetectorCreatedAndGet() {
        if (contentTypeDetector == null) {
            final HeadersContentTypeDetector headersContentTypeDetector =
                    new HeadersContentTypeDetector();
            final UrlFileExtensionTypeDetector urlFileExtensionTypeDetector =
                    new UrlFileExtensionTypeDetector();
            contentTypeDetector = new OrderedContentTypeDetector(headersContentTypeDetector,
                    urlFileExtensionTypeDetector);
        }
        return contentTypeDetector;
    }

    private void notifyNavigation() {
        final EventsListener eventsListener = getEventsListener();
        if (eventsListener != null) {
            eventsListener.onNavigation();
        }
    }

    private void notifyResourceBlocked(final EventsListener.BlockedResourceInfo info) {
        final EventsListener eventsListener = getEventsListener();
        if (eventsListener != null) {
            eventsListener.onResourceLoadingBlocked(info);
        }
    }

    private void notifyResourceWhitelisted(final EventsListener.WhitelistedResourceInfo info) {
        final EventsListener eventsListener = getEventsListener();
        if (eventsListener != null) {
            eventsListener.onResourceLoadingWhitelisted(info);
        }
    }

    private boolean isVisibleResource(final FilterEngine.ContentType contentType) {
        return
                contentType == FilterEngine.ContentType.IMAGE ||
                        contentType == FilterEngine.ContentType.MEDIA ||
                        contentType == FilterEngine.ContentType.OBJECT ||
                        contentType == FilterEngine.ContentType.SUBDOCUMENT;
    }

    private void elemhideBlockedResource(final String url) {
        String filenameWithQuery;
        try {
            filenameWithQuery = Utils.extractPathWithQuery(url);
            if (filenameWithQuery.startsWith("/")) {
                filenameWithQuery = filenameWithQuery.substring(1);
            }
        } catch (final MalformedURLException e) {
            loge("Failed to parse URI for blocked resource:" + url + ". Skipping element hiding");
            return;
        }
        logd("Trying to elemhide visible blocked resource with url `%s` and path `%s`",
                url, filenameWithQuery);

    /*
    It finds all the elements with source URLs ending with ... and then compare full paths.
    We do this trick because the paths in JS (code) can be relative and in DOM tree they are absolute.
     */
        final StringBuilder selectorBuilder = new StringBuilder();
        selectorBuilder.append("[src$='");
        selectorBuilder.append(filenameWithQuery);
        selectorBuilder.append("'], [srcset$='");
        selectorBuilder.append(filenameWithQuery);
        selectorBuilder.append("']");

        // all UI views including AdblockWebView can be touched from UI thread only
        post(new Runnable() {
            @Override
            public void run() {
                final StringBuilder scriptBuilder = new StringBuilder(elemhideBlockedJs);
                scriptBuilder.append("\n\n");
                scriptBuilder.append("elemhideForSelector(\"");
                scriptBuilder.append(url); // 1st argument
                scriptBuilder.append("\", \"");
                scriptBuilder.append(Utils.escapeJavaScriptString(selectorBuilder.toString())); // 2nd argument
                scriptBuilder.append("\", 0)"); // attempt #0

                AdblockWebView.this.evaluateJavascript(scriptBuilder.toString(), null);
            }
        });
    }

    private void initRandom() {
        final Random random = new Random();
        elementsHiddenFlag = "abp" + Math.abs(random.nextLong());
        sitekeyExtractedFlag = "abp" + Math.abs(random.nextLong());
    }

    private class ElemHideThread extends Thread {
        private String stylesheetString;
        private String emuSelectorsString;
        private final CountDownLatch finishedLatch;
        private final AtomicBoolean isFinished;
        private final AtomicBoolean isCancelled;

        public ElemHideThread(final CountDownLatch finishedLatch) {
            this.finishedLatch = finishedLatch;
            isFinished = new AtomicBoolean(false);
            isCancelled = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            final Lock lock = getProvider().getReadEngineLock();
            lock.lock();

            try {
                boolean isDisposed = false;
                if (getProvider().getCounter() == 0) {
                    isDisposed = true;
                } else {
                    lock.unlock();
                    getProvider().waitForReady();
                    lock.lock();
                    if (getProvider().getCounter() == 0) {
                        isDisposed = true;
                    }
                }

                // Apart from checking counter (getProvider().getCounter()) we also need to make sure
                // that getProvider().getEngine() is already set.
                // We check that under getProvider().getReadEngineLock(); so we are sure it will not be
                // changed after this check.
                if (isDisposed || getProvider().getEngine() == null) {
                    logw("FilterEngine already disposed");
                    stylesheetString = EMPTY_ELEMHIDE_STRING;
                    emuSelectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                } else {
                    final List<String> referrerChain = new ArrayList<>(1);
                    String parentUrl = navigationUrl.get();
                    referrerChain.add(parentUrl);
                    while ((parentUrl = url2Referrer.get(parentUrl)) != null) {
                        if (referrerChain.contains(parentUrl)) {
                            logw("Detected referrer loop, finished creating referrers list");
                            break;
                        }
                        referrerChain.add(0, parentUrl);
                    }

                    final FilterEngine filterEngine = getProvider().getEngine().getFilterEngine();

                    final List<Subscription> subscriptions = filterEngine.getListedSubscriptions();

                    try {
                        logd("Listed subscriptions: %d", subscriptions.size());
                        if (isDebug()) {
                            for (final Subscription eachSubscription : subscriptions) {
                                logd("Subscribed to "
                                        + (eachSubscription.isDisabled() ? "disabled" : "enabled")
                                        + " " + eachSubscription);
                            }
                        }
                    } finally {
                        for (final Subscription eachSubscription : subscriptions) {
                            eachSubscription.dispose();
                        }
                    }

                    final String navigationUrlLocalRef = navigationUrl.get();
                    final String domain = filterEngine.getHostFromURL(navigationUrlLocalRef);
                    if (domain == null) {
                        loge("Failed to extract domain from %s", navigationUrlLocalRef);
                        stylesheetString = EMPTY_ELEMHIDE_STRING;
                        emuSelectorsString = EMPTY_ELEMHIDE_ARRAY_STRING;
                    } else {
                        // elemhide
                        logd("Requesting elemhide selectors from AdblockEngine for %s in %s",
                                navigationUrlLocalRef, this);

                        final SiteKeysConfiguration siteKeysConfiguration = getSiteKeysConfiguration();
                        final String siteKey = (siteKeysConfiguration != null
                                ? PublicKeyHolderImpl.stripPadding(siteKeysConfiguration.getPublicKeyHolder()
                                .getAny(referrerChain, ""))
                                : null);

                        final boolean specificOnly = filterEngine.matches(navigationUrlLocalRef,
                                FilterEngine.ContentType.maskOf(FilterEngine.ContentType.GENERICHIDE),
                                Collections.<String>emptyList(), null) != null;

                        if (specificOnly) {
                            logd("elemhide - specificOnly selectors");
                        }

                        stylesheetString = getProvider()
                                .getEngine()
                                .getElementHidingStyleSheet(navigationUrlLocalRef, domain, referrerChain, siteKey, specificOnly);

                        logd("Finished requesting elemhide stylesheet, got %d symbols in %s",
                                stylesheetString.length(), this);

                        // elemhideemu
                        logd("Requesting elemhideemu selectors from AdblockEngine for %s in %s",
                                navigationUrlLocalRef, this);
                        final List<FilterEngine.EmulationSelector> emuSelectors = getProvider()
                                .getEngine()
                                .getElementHidingEmulationSelectors(navigationUrlLocalRef, domain, referrerChain, siteKey);

                        logd("Finished requesting elemhideemu selectors, got  got %d in %s",
                                emuSelectors.size(), this);
                        emuSelectorsString = Utils.emulationSelectorListToJsonArray(emuSelectors);
                    }
                }
            } finally {
                lock.unlock();
                if (isCancelled.get()) {
                    logw("This thread is cancelled, exiting silently %s", this);
                } else {
                    finish(stylesheetString, emuSelectorsString);
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

        private void finish(final String selectorsString, final String emuSelectorsString) {
            isFinished.set(true);
            logd("Setting elemhide string %d bytes", selectorsString.length());
            elemHideSelectorsString = selectorsString;

            logd("Setting elemhideemu string %d bytes", emuSelectorsString.length());
            elemHideEmuSelectorsString = emuSelectorsString;

            onFinished();
        }

        private final Object finishedRunnableLockObject = new Object();
        private Runnable finishedRunnable;

        public void setFinishedRunnable(final Runnable runnable) {
            synchronized (finishedRunnableLockObject) {
                this.finishedRunnable = runnable;
            }
        }

        public void cancel() {
            logw("Cancelling elemhide thread %s", this);
            if (isFinished.get()) {
                logw("This thread is finished, exiting silently %s", this);
            } else {
                isCancelled.set(true);
                finish(EMPTY_ELEMHIDE_STRING, EMPTY_ELEMHIDE_ARRAY_STRING);
            }
        }
    }

    private final Runnable elemHideThreadFinishedRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (elemHideThreadLockObject) {
                logw("elemHideThread set to null");
                elemHideThread = null;
            }
        }
    };

    private void ensureProvider() {
        // if AdblockWebView works as drop-in replacement for WebView 'provider' is not set.
        // Thus AdblockWebView is using SingleInstanceEngineProvider instance
        if (getProvider() == null) {
            final AdblockEngine.Factory factory = AdblockEngine
                    .builder(getContext(), AdblockEngine.BASE_PATH_DIRECTORY);
            setProvider(new SingleInstanceEngineProvider(factory));
        }
    }

    private void startAbpLoading(final String newUrl) {
        logd("Start loading %s", newUrl);

        loading = true;
        loadError = null;

        if (newUrl != null) {
            navigationUrl.compareAndSet(null, newUrl);

            // elemhide and elemhideemu
            elemHideLatch = new CountDownLatch(1);
            synchronized (elemHideThreadLockObject) {
                elemHideThread = new ElemHideThread(elemHideLatch);
                elemHideThread.setFinishedRunnable(elemHideThreadFinishedRunnable);
                elemHideThread.start();
            }
        } else {
            elemHideLatch = null;
        }
        clearReferrers();
    }

    private void buildInjectJs() {
        try {
            if (injectJs == null) {
                final StringBuffer sb = new StringBuffer();
                sb.append(readScriptFile("inject.js").replace(HIDE_TOKEN, readScriptFile("css.js")));
                sb.append(readScriptFile("elemhideemu.js"));
                injectJs = sb.toString();
            }

            if (elemhideBlockedJs == null) {
                elemhideBlockedJs = readScriptFile("elemhideblocked.js");
            }
        } catch (final IOException e) {
            loge(e, "Failed to read script");
        }
    }

    @Override
    public void goBack() {
        if (loading) {
            stopAbpLoading();
        }

        if (this.canGoBack()) {
            navigationUrl.set(null);
            siteKeyExtractor.startNewPage();
        }
        super.goBack();
    }

    @Override
    public void goForward() {
        if (loading) {
            stopAbpLoading();
        }

        if (this.canGoForward()) {
            navigationUrl.set(null);
            siteKeyExtractor.startNewPage();
        }
        super.goForward();
    }

    @Override
    public void reload() {
        checkCookieSettings();
        ensureProvider();

        if (loading) {
            stopAbpLoading();
        }

        super.reload();
    }

    private void checkCookieSettings() {
        final boolean acceptCookies = CookieManager.getInstance().acceptCookie();
        acceptCookie.set(acceptCookies);
        // If cookies are disabled no need to check more
        if (acceptCookies) {
            // acceptThirdPartyCookies() needs to be called from UI thread
            acceptThirdPartyCookies.set(CookieManager.getInstance().acceptThirdPartyCookies(this));
        }
    }

    @Override
    public WebBackForwardList restoreState(final Bundle inState) {
        siteKeyExtractor.startNewPage();
        return super.restoreState(inState);
    }

    private void loadUrlCommon() {
        checkCookieSettings();
        ensureProvider();

        if (loading) {
            stopAbpLoading();
        }

        siteKeyExtractor.startNewPage();
    }

    @Override
    public void loadUrl(final String url) {
        loadUrlCommon();
        super.loadUrl(url);
    }

    @Override
    public void loadUrl(final String url, final Map<String, String> additionalHttpHeaders) {
        loadUrlCommon();
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadData(final String data, final String mimeType, final String encoding) {
        loadUrlCommon();
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(final String baseUrl, final String data, final String mimeType,
                                    final String encoding, final String historyUrl) {
        loadUrlCommon();
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
        clearReferrers();

        synchronized (elemHideThreadLockObject) {
            if (elemHideThread != null) {
                elemHideThread.cancel();
            }
        }
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public String getElemhideStyleSheet() {
        if (elemHideLatch == null) {
            return EMPTY_ELEMHIDE_STRING;
        } else {
            try {
                // elemhide selectors list getting is started in startAbpLoad() in background thread
                logd("Waiting for elemhide selectors to be ready");
                elemHideLatch.await();
                logd("Elemhide selectors ready, %d bytes", elemHideSelectorsString.length());

                return elemHideSelectorsString;
            } catch (final InterruptedException e) {
                logw("Interrupted, returning empty selectors list");
                return EMPTY_ELEMHIDE_STRING;
            }
        }
    }

    // warning: do not rename (used in injected JS by method name)
    @JavascriptInterface
    public String getElemhideEmulationSelectors() {
        if (elemHideLatch == null) {
            return EMPTY_ELEMHIDE_ARRAY_STRING;
        } else {
            try {
                // elemhideemu selectors list getting is started in startAbpLoad() in background thread
                logd("Waiting for elemhideemu selectors to be ready");
                elemHideLatch.await();
                logd("Elemhideemu selectors ready, %d bytes", elemHideEmuSelectorsString.length());

                return elemHideEmuSelectorsString;
            } catch (final InterruptedException e) {
                logw("Interrupted, returning empty elemhideemu selectors list");
                return EMPTY_ELEMHIDE_ARRAY_STRING;
            }
        }
    }

    private void doDispose() {
        logw("Disposing AdblockEngine");
        getProvider().release();
    }

    private class DisposeRunnable implements Runnable {
        private final Runnable disposeFinished;

        private DisposeRunnable(final Runnable disposeFinished) {
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

        if (getProvider() == null) {
            logd("No internal AdblockEngineProvider created");
            return;
        }

        final Lock lock = getProvider().getReadEngineLock();
        lock.lock();

        try {
            final AdblockEngine engine = getProvider().getEngine();
            if (engine != null) {
                engine.removeSettingsChangedListener(engineSettingsChangedCb);
            }
            getProvider().removeEngineCreatedListener(engineCreatedCb);
            getProvider().removeEngineDisposedListener(engineDisposedCb);
        } finally {
            lock.unlock();
        }

        stopLoading();

        final DisposeRunnable disposeRunnable = new DisposeRunnable(disposeFinished);
        synchronized (elemHideThreadLockObject) {
            if (elemHideThread != null) {
                logw("Busy with elemhide selectors, delayed disposing scheduled");
                elemHideThread.setFinishedRunnable(disposeRunnable);
            } else {
                disposeRunnable.run();
            }
        }
    }

    public boolean isDebug() {
        return isDebug && WebConfig.DEBUG;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public void logi(String msg, Object... args) {
        if (isDebug()) {
            Timber.i(msg, args);
        }
    }

    public void logd(String msg, Object... args) {
        if (isDebug()) {
            Timber.d(msg, args);
        }
    }

    public void logw(String msg, Object... args) {
        if (isDebug()) {
            Timber.w(msg, args);
        }
    }

    public void loge(String msg, Object... args) {
        if (isDebug()) {
            Timber.e(msg, args);
        }
    }

    public void loge(Throwable t, @NonNls String message, Object... args) {
        if (isDebug()) {
            Timber.e(t, message, args);
        }
    }
}
