package top.xuqingquan.web.x5.adblock;

import android.annotation.SuppressLint;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;

import org.adblockplus.libadblockplus.AdblockPlusException;
import org.adblockplus.libadblockplus.HeaderEntry;
import org.adblockplus.libadblockplus.HttpClient;
import org.adblockplus.libadblockplus.HttpRequest;
import org.adblockplus.libadblockplus.ServerResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import timber.log.Timber;
import top.xuqingquan.web.x5.AdblockWebView;

import static org.adblockplus.libadblockplus.android.Utils.convertHeaderEntriesToMap;
import static org.adblockplus.libadblockplus.android.Utils.convertMapToHeadersList;

/**
 * Makes a custom HTTP request and then does the <i>Site Key</i> verification by calling
 * {@link org.adblockplus.libadblockplus.sitekey.SiteKeyVerifier#verifyInHeaders(String, Map, Map)}
 */
@SuppressLint("NewApi")
public class HttpHeaderSiteKeyExtractor extends BaseSiteKeyExtractor {
    private static String getReasonPhrase(final ServerResponse.NsStatus status) {
        return status.name().replace("_", "");
    }

    private static class ResponseHolder {
        ServerResponse response;
    }

    public static class ResourceInfo {
        private static final String CHARSET = "charset=";
        private static final int CHARSET_LENGTH = CHARSET.length();

        private String mimeType;
        private String encoding;

        public String getMimeType() {
            return mimeType;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        // if `contentType` is null the fields will be `null` too
        public static ResourceInfo parse(final String contentType) {
            final ResourceInfo resourceInfo = new ResourceInfo();

            if (contentType != null) {
                final int semicolonPos = contentType.indexOf(";");
                if (semicolonPos > 0) {
                    resourceInfo.mimeType = contentType.substring(0, semicolonPos);
                    final int charsetPos = contentType.indexOf(CHARSET);
                    if ((charsetPos >= 0) &&
                            (charsetPos < contentType.length() - CHARSET_LENGTH)) {
                        resourceInfo.encoding = contentType.substring(charsetPos + CHARSET_LENGTH);
                    }
                } else if (contentType.indexOf("/") > 0) {
                    resourceInfo.mimeType = contentType;
                }
            }

            return resourceInfo;
        }

        private void trim() {
            if (mimeType != null) {
                mimeType = mimeType.trim();
            }

            if (encoding != null) {
                encoding = encoding.trim();
            }
        }
    }

    public static class ServerResponseProcessor {
        public WebResourceResponse process(final String requestUrl,
                                           final ServerResponse response,
                                           final Map<String, String> responseHeaders) {
            final String responseContentType = responseHeaders.get(HttpClient.HEADER_CONTENT_TYPE);
            final ResourceInfo responseInfo = ResourceInfo.parse(responseContentType);

            if (responseInfo.getMimeType() != null) {
                Timber.d("Removing %s to avoid Content-Type duplication",
                        HttpClient.HEADER_CONTENT_TYPE);
                responseHeaders.remove(HttpClient.HEADER_CONTENT_TYPE);

      /*
        Quoting https://developer.android.com/reference/android/webkit/WebResourceResponse:
        Do not use the value of a HTTP Content-Encoding header for encoding, as that header does not
        specify a character encoding. Content without a defined character encoding
        (for example image resources) should pass null for encoding.
       */
                if (responseInfo.getEncoding() != null && responseInfo.getMimeType().startsWith("image")) {
                    Timber.d("Setting responseEncoding to null for contentType == %s",
                            responseInfo.getMimeType());
                    responseInfo.setEncoding(null);
                }
            } else if (responseHeaders.get(HttpClient.HEADER_CONTENT_LENGTH) != null) {
                // For some reason for responses which lack Content-Type header and has Content-Length==0,
                // underlying WebView layer can trigger a DownloadListener. Applying "default" Content-Type
                // value helps. To reduce risk we apply it only when Content-Length==0 as there is no body
                // so there is no risk that browser will
                // render that even when we apply a wrong Content-Type.
                Integer contentLength = null;
                try {
                    // we are catching NPE so disabling lint
                    //noinspection ConstantConditions
                    contentLength = Integer.parseInt(
                            responseHeaders.get(HttpClient.HEADER_CONTENT_LENGTH).trim()
                    );
                } catch (final NumberFormatException | NullPointerException e) {
                    Timber.e(e, "Integer.parseInt(responseHeadersMap.get(HEADER_CONTENT_LENGTH)) failed");
                }

                if (contentLength == null) {
                    Timber.d("Setting responseMimeType to %s",
                            AdblockWebView.WebResponseResult.RESPONSE_MIME_TYPE);
                    responseInfo.setMimeType(AdblockWebView.WebResponseResult.RESPONSE_MIME_TYPE);
                }
            }

            responseInfo.trim();

            Timber.d("Using responseMimeType and responseEncoding: %s => %s (url == %s)",
                    responseInfo.getMimeType(), responseInfo.getEncoding(), requestUrl);
            return new WebResourceResponse(
                    responseInfo.getMimeType(), responseInfo.getEncoding(),
                    response.getResponseStatus(), getReasonPhrase(response.getStatus()),
                    responseHeaders, response.getInputStream());
        }
    }

    public HttpHeaderSiteKeyExtractor(final AdblockWebView webView) {
        super(webView);
    }

    @Override
    public WebResourceResponse extract(final WebResourceRequest request) {
        // if disabled (probably AA is disabled) do nothing
        if (!isEnabled()) {
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        }

        if (getSiteKeysConfiguration() == null ||
                !request.getMethod().equalsIgnoreCase(HttpClient.REQUEST_METHOD_GET)) {
            // for now we handle site key only for GET requests
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        }

        Timber.d("extract() called from Thread %s",
                Thread.currentThread().getId());

        final ServerResponse response;
        try {
            response = sendRequest(request);
        } catch (final AdblockPlusException e) {
            Timber.e(e, "WebRequest failed");
            // allow WebView to continue, repeating the request and handling the response
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        } catch (final InterruptedException e) {
            // error waiting for the response, continue by returning null
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        }

        // in some circumstances statusCode gets > 599
        // also checking redirect should not happen but
        // jic it would not crash
        if (!HttpClient.isValidCode(response.getResponseStatus()) ||
                HttpClient.isRedirectCode(response.getResponseStatus())) {
            // looks like the response is just broken, let it go
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        }

        String url = request.getUrl().toString();
        processResponseCookies(webViewWeakReference.get(), url, response);

        if (response.getFinalUrl() != null) {
            Timber.d("Updating url to %s, was (%s)", response.getFinalUrl(), url);
            url = response.getFinalUrl();
        }

        if (response.getInputStream() == null) {
            Timber.w("fetchUrlAndCheckSiteKey() passes control to WebView");
            return AdblockWebView.WebResponseResult.ALLOW_LOAD;
        }

        return processResponse(request, url, response);
    }

    private WebResourceResponse processResponse(final WebResourceRequest request,
                                                final String requestUrl,
                                                final ServerResponse response) {
        final Map<String, String> requestHeaders = request.getRequestHeaders();
        final Map<String, String> responseHeaders =
                convertHeaderEntriesToMap(response.getResponseHeaders());

        // extract the sitekey from HTTP response header
        getSiteKeysConfiguration().getSiteKeyVerifier().verifyInHeaders(
                requestUrl, requestHeaders, responseHeaders);

        return new ServerResponseProcessor().process(requestUrl, response, responseHeaders);
    }

    // Note: `response` headers can be modified inside
    private void processResponseCookies(final AdblockWebView webView,
                                        final String requestUrl,
                                        final ServerResponse response) {
        final List<HeaderEntry> responseHeaders = response.getResponseHeaders();
        final List<HeaderEntry> cookieHeadersToRemove = new ArrayList<>();
        for (final HeaderEntry eachEntry : responseHeaders) {
            if (HttpClient.HEADER_SET_COOKIE.equalsIgnoreCase(eachEntry.getKey())) {
                if (webView.canAcceptCookie(requestUrl, eachEntry.getValue())) {
                    Timber.d("Calling setCookie(%s)", requestUrl);
                    CookieManager.getInstance().setCookie(requestUrl, eachEntry.getValue());
                } else {
                    Timber.d("Rejecting setCookie(%s)", requestUrl);
                }
                cookieHeadersToRemove.add(eachEntry);
            }
        }

        // DP-971: We don't need to pass HEADER_SET_COOKIE data further
        responseHeaders.removeAll(cookieHeadersToRemove);
    }

    private ServerResponse sendRequest(final WebResourceRequest request) throws InterruptedException {
        final String requestUrl = request.getUrl().toString();
        final Map<String, String> requestHeadersMap = request.getRequestHeaders();

        final ResponseHolder responseHolder = new ResponseHolder();
        final CountDownLatch latch = new CountDownLatch(1);
        final HttpClient.Callback callback = new HttpClient.Callback() {
            @Override
            public void onFinished(final ServerResponse response_) {
                responseHolder.response = response_;
                latch.countDown();
            }
        };

        final List<HeaderEntry> requestHeadersList = convertMapToHeadersList(requestHeadersMap);
        final String cookieValue = CookieManager.getInstance().getCookie(requestUrl);
        if (cookieValue != null && !cookieValue.isEmpty()) {
            Timber.d("Adding %s request header for url %s", HttpClient.HEADER_COOKIE, requestUrl);
            requestHeadersList.add(new HeaderEntry(HttpClient.HEADER_COOKIE, cookieValue));
        }

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                request.getMethod(),
                requestHeadersList,
                true,              // always true since we don't use it for main frame
                true);
        getSiteKeysConfiguration().getHttpClient().request(httpRequest, callback);

        latch.await();

        return responseHolder.response;
    }

    @Override
    public void startNewPage() {
        // no-op
    }

    @Override
    public boolean waitForSitekeyCheck(final WebResourceRequest request) {
        // no need to block the network request for this extractor
        // this callback is used in JsSiteKeyExtractor
        return false;
    }
}