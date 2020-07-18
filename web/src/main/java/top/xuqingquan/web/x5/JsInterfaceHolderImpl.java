package top.xuqingquan.web.x5;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.tencent.smtt.sdk.WebView;

import java.util.Map;
import java.util.Set;

import top.xuqingquan.web.nokernel.JsInterfaceHolder;
import top.xuqingquan.web.nokernel.JsInterfaceObjectException;
import top.xuqingquan.utils.Timber;

@SuppressWarnings("rawtypes")
public class JsInterfaceHolderImpl extends JsBaseInterfaceHolder {

    private WebView mWebView;

    public static JsInterfaceHolderImpl getJsInterfaceHolder(WebCreator webCreator) {
        return new JsInterfaceHolderImpl(webCreator);
    }

    private JsInterfaceHolderImpl(WebCreator webCreator) {
        super(webCreator);
        this.mWebView = webCreator.getWebView();
    }

    @NonNull
    @Override
    public JsInterfaceHolder addJavaObjects(Map<String, Object> maps) {
        Set<Map.Entry<String, Object>> sets = maps.entrySet();
        for (Map.Entry<String, Object> mEntry : sets) {
            Object v = mEntry.getValue();
            boolean t = checkObject(v);
            if (!t) {
                throw new JsInterfaceObjectException("This object has not offer method javascript to call ,please check addJavascriptInterface annotation was be added");
            } else {
                addJavaObjectDirect(mEntry.getKey(), v);
            }
        }
        return this;
    }

    @NonNull
    @Override
    public JsInterfaceHolder addJavaObject(@NonNull String k, @NonNull Object v) {
        boolean t = checkObject(v);
        if (!t) {
            throw new JsInterfaceObjectException("this object has not offer method javascript to call , please check addJavascriptInterface annotation was be added");
        } else {
            addJavaObjectDirect(k, v);
        }
        return this;
    }

    @SuppressLint("JavascriptInterface")
    private JsInterfaceHolder addJavaObjectDirect(String k, Object v) {
        Timber.i("k:$k  v:$v");
        this.mWebView.addJavascriptInterface(v, k);
        return this;
    }

}
