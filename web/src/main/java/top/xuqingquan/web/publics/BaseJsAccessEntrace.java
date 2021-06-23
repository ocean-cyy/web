package top.xuqingquan.web.publics;

import android.os.Build;

import androidx.annotation.Nullable;

import top.xuqingquan.utils.CharacterUtils;

public abstract class BaseJsAccessEntrace implements JsAccessEntrace {

    private final android.webkit.WebView mWebView;

    BaseJsAccessEntrace(android.webkit.WebView webView) {
        this.mWebView = webView;
    }

    @Override
    public void callJs(@Nullable String js, @Nullable android.webkit.ValueCallback<String> callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.evaluateJs(js, callback);
        } else {
            this.loadJs(js);
        }
    }

    @Override
    public void callJs(@Nullable String js) {
        this.callJs(js, (android.webkit.ValueCallback<String>) null);
    }

    private void loadJs(@Nullable String js) {
        if (mWebView != null) {
            mWebView.loadUrl(js);
        }
    }

    private void evaluateJs(@Nullable String js, @Nullable android.webkit.ValueCallback<String> callback) {
        if (mWebView == null) {
            return;
        }
        mWebView.evaluateJavascript(js, value -> {
            if (callback != null) {
                callback.onReceiveValue(value);
            }
        });
    }

    @Override
    public void quickCallJs(@Nullable String method, @Nullable android.webkit.ValueCallback<String> callback, @Nullable String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("javascript:").append(method);
        if (params == null || params.length == 0) {
            sb.append("()");
        } else {
            sb.append("(").append(concat(params)).append(")");
        }
        callJs(sb.toString(), callback);
    }

    private String concat(String... params) {
        StringBuilder mStringBuilder = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            if (!CharacterUtils.isJson(param)) {
                mStringBuilder.append("\"").append(param).append("\"");
            } else {
                mStringBuilder.append(param);
            }
            if (i != params.length - 1) {
                mStringBuilder.append(" , ");
            }
        }
        return mStringBuilder.toString();
    }

    @Override
    public void quickCallJs(@Nullable String method, @Nullable String... params) {
        this.quickCallJs(method, (android.webkit.ValueCallback<String>) null, params);
    }

    @Override
    public void quickCallJs(@Nullable String method) {
        this.quickCallJs(method, (String[]) null);
    }
}
