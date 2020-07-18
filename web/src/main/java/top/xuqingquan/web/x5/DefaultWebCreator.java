package top.xuqingquan.web.x5;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tencent.smtt.sdk.WebView;

import top.xuqingquan.web.R;
import top.xuqingquan.web.nokernel.BaseIndicatorSpec;
import top.xuqingquan.web.nokernel.BaseIndicatorView;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.web.nokernel.WebIndicator;
import top.xuqingquan.web.publics.WebParentLayout;
import top.xuqingquan.utils.DimensionsKt;
import top.xuqingquan.utils.Timber;

@SuppressWarnings("rawtypes")
public class DefaultWebCreator implements WebCreator {
    private Activity mActivity;
    private ViewGroup mViewGroup;
    private boolean mIsNeedDefaultProgress;
    private int mIndex;
    private BaseIndicatorView mProgressView;
    private ViewGroup.LayoutParams mLayoutParams;
    private int mColor = -1;
    /**
     * 单位dp
     */
    private int mHeight;
    private boolean mIsCreated = false;
    private top.xuqingquan.web.x5.IWebLayout mIWebLayout;
    private BaseIndicatorSpec mBaseIndicatorSpec;
    private WebView mWebView;
    private FrameLayout mFrameLayout = null;
    private int mWebViewType = WebConfig.WEBVIEW_DEFAULT_TYPE;

    /**
     * 使用默认的进度条
     */
    public DefaultWebCreator(
            @NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index,
            int color, int mHeight, WebView webView, top.xuqingquan.web.x5.IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = true;
        this.mIndex = index;
        this.mColor = color;
        this.mLayoutParams = lp;
        this.mHeight = mHeight;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    /**
     * 关闭进度条
     */
    public DefaultWebCreator(@NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index, @Nullable WebView webView, top.xuqingquan.web.x5.IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = false;
        this.mIndex = index;
        this.mLayoutParams = lp;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    /**
     * 自定义Indicator
     */
    public DefaultWebCreator(@NonNull Activity activity, @Nullable ViewGroup viewGroup, ViewGroup.LayoutParams lp, int index, BaseIndicatorView progressView, WebView webView, top.xuqingquan.web.x5.IWebLayout webLayout) {
        this.mActivity = activity;
        this.mViewGroup = viewGroup;
        this.mIsNeedDefaultProgress = false;
        this.mIndex = index;
        this.mLayoutParams = lp;
        this.mProgressView = progressView;
        this.mWebView = webView;
        this.mIWebLayout = webLayout;
    }

    public void setWebView(WebView webView) {
        mWebView = webView;
    }

    @NonNull
    @Override
    public DefaultWebCreator create() {
        if (mIsCreated) {
            return this;
        }
        mIsCreated = true;
        ViewGroup mViewGroup = this.mViewGroup;
        this.mFrameLayout = (FrameLayout) createLayout();
        if (mViewGroup == null) {
            mViewGroup = this.mFrameLayout;
            mActivity.setContentView(mViewGroup);
        } else {
            if (mIndex == -1) {
                mViewGroup.addView(this.mFrameLayout, mLayoutParams);
            } else {
                mViewGroup.addView(this.mFrameLayout, mIndex, mLayoutParams);
            }
        }
        return this;
    }

    @Override
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    @NonNull
    public FrameLayout getWebParentLayout() {
        return mFrameLayout;
    }

    @Override
    public int getWebViewType() {
        return this.mWebViewType;
    }

    private ViewGroup createLayout() {
        Activity mActivity = this.mActivity;
        WebParentLayout mFrameLayout = new WebParentLayout(mActivity);
        mFrameLayout.setId(R.id.scaffold_web_parent_layout_id);
        mFrameLayout.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams mLayoutParams = new FrameLayout.LayoutParams(-1, -1);
        this.mWebView = createWebView();
        View target;
        if (mIWebLayout == null) {
            target = this.mWebView;
        } else {
            target = webLayout();
        }
        mFrameLayout.addView(target, mLayoutParams);
        mFrameLayout.bindWebView(this.mWebView);
        Timber.i("  instanceof  AgentWebView:" + (this.mWebView instanceof top.xuqingquan.web.x5.AgentWebView));
        if (this.mWebView instanceof top.xuqingquan.web.x5.AgentWebView) {
            this.mWebViewType = WebConfig.WEBVIEW_AGENTWEB_SAFE_TYPE;
        }
        ViewStub mViewStub = new ViewStub(mActivity);
        mViewStub.setId(R.id.scaffold_mainframe_error_viewsub_id);
        mFrameLayout.addView(mViewStub, new FrameLayout.LayoutParams(-1, -1));
        if (mIsNeedDefaultProgress) {
            FrameLayout.LayoutParams lp;
            WebIndicator mWebIndicator = new WebIndicator(mActivity);
            if (mHeight > 0) {
                lp = new FrameLayout.LayoutParams(-2, DimensionsKt.dip(mActivity, mHeight));
            } else {
                lp = mWebIndicator.offerLayoutParams();
            }
            if (mColor != -1) {
                mWebIndicator.setColor(mColor);
            }
            lp.gravity = Gravity.TOP;
            this.mBaseIndicatorSpec = mWebIndicator;
            mFrameLayout.addView((View) this.mBaseIndicatorSpec, lp);
            mWebIndicator.setVisibility(View.GONE);
        } else if (mProgressView != null) {
            this.mBaseIndicatorSpec = mProgressView;
            mFrameLayout.addView((View) (this.mBaseIndicatorSpec), mProgressView.offerLayoutParams());
            mProgressView.setVisibility(View.GONE);
        }
        return mFrameLayout;
    }

    private View webLayout() {
        WebView mWebView = mIWebLayout.getWebView();
        if (mWebView == null) {
            mWebView = createWebView();
            mIWebLayout.getLayout().addView(mWebView, -1, -1);
            Timber.i("add webview");
        } else {
            this.mWebViewType = WebConfig.WEBVIEW_CUSTOM_TYPE;
        }
        this.mWebView = mWebView;
        return mIWebLayout.getLayout();
    }

    private WebView createWebView() {
        WebView mWebView;
        if (this.mWebView != null) {
            mWebView = this.mWebView;
            this.mWebViewType = WebConfig.WEBVIEW_CUSTOM_TYPE;
        } else if (WebConfig.IS_KITKAT_OR_BELOW_KITKAT) {
            mWebView = new top.xuqingquan.web.x5.AgentWebView(mActivity);
            this.mWebViewType = WebConfig.WEBVIEW_AGENTWEB_SAFE_TYPE;
        } else {
            mWebView = new LollipopFixedWebView(mActivity);
            this.mWebViewType = WebConfig.WEBVIEW_DEFAULT_TYPE;
        }
        return mWebView;
    }

    @Override
    @NonNull
    public BaseIndicatorSpec offer() {
        return mBaseIndicatorSpec;
    }
}
