package top.xuqingquan.web.publics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import top.xuqingquan.web.R;
import top.xuqingquan.web.nokernel.Provider;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.utils.Timber;

public class WebParentLayout extends FrameLayout implements Provider<AbsAgentWebUIController> {
    private AbsAgentWebUIController mAgentWebUIController = null;
    @LayoutRes
    private int mErrorLayoutRes;
    @IdRes
    private int mClickId = -1;
    private View mErrorView;
    private android.webkit.WebView mWebView;
    private com.tencent.smtt.sdk.WebView x5WebView;
    private FrameLayout mErrorLayout = null;

    public WebParentLayout(@NonNull Context context) {
        this(context, null);
        Timber.i("WebParentLayout");
    }

    WebParentLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    WebParentLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("WebParentLayout context must be activity or activity sub class .");
        }
        this.mErrorLayoutRes = R.layout.scaffold_web_error_page;
    }

    public void bindController(AbsAgentWebUIController agentWebUIController) {
        this.mAgentWebUIController = agentWebUIController;
        this.mAgentWebUIController.bindWebParent(this, (Activity) getContext());
    }

    void showPageMainFrameError() {
        View container = this.mErrorLayout;
        if (container != null) {
            container.setVisibility(View.VISIBLE);
        } else {
            createErrorLayout();
            container = this.mErrorLayout;
        }
        if (mClickId != -1) {
            View clickView = container.findViewById(mClickId);
            if (clickView != null) {
                clickView.setClickable(true);
            } else {
                container.setClickable(true);
            }
        } else {
            container.setClickable(true);
        }
    }

    private void createErrorLayout() {
        final FrameLayout mFrameLayout = new FrameLayout(getContext());
        mFrameLayout.setBackgroundColor(Color.WHITE);
        mFrameLayout.setId(R.id.scaffold_mainframe_error_container_id);
        if (this.mErrorView == null) {
            LayoutInflater mLayoutInflater = LayoutInflater.from(getContext());
            Timber.i("mErrorLayoutRes:" + mErrorLayoutRes);
            mLayoutInflater.inflate(mErrorLayoutRes, mFrameLayout, true);
        } else {
            mFrameLayout.addView(mErrorView);
        }
        ViewStub mViewStub = this.findViewById(R.id.scaffold_mainframe_error_viewsub_id);
        final int index = this.indexOfChild(mViewStub);
        this.removeViewInLayout(mViewStub);
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        this.mErrorLayout = mFrameLayout;
        if (layoutParams != null) {
            this.addView(this.mErrorLayout, index, layoutParams);
        } else {
            this.addView(this.mErrorLayout, index);
        }
        mFrameLayout.setVisibility(View.VISIBLE);
        if (mClickId != -1) {
            final View clickView = mFrameLayout.findViewById(mClickId);
            if (clickView != null) {
                clickView.setOnClickListener(v -> {
                    if (WebConfig.enableTbs()) {
                        if (getX5WebView() != null) {
                            clickView.setClickable(false);
                            getX5WebView().reload();
                        }
                    } else {
                        if (getWebView() != null) {
                            clickView.setClickable(false);
                            getWebView().reload();
                        }
                    }
                });
                return;
            } else {
                Timber.e("ClickView is null , cannot bind accurate view to refresh or reload .");
            }
        }
        mFrameLayout.setOnClickListener(v -> {
            if (WebConfig.enableTbs()) {
                if (getX5WebView() != null) {
                    mFrameLayout.setClickable(false);
                    getX5WebView().reload();
                }
            } else {
                if (getWebView() != null) {
                    mFrameLayout.setClickable(false);
                    getWebView().reload();
                }
            }
        });
    }

    void hideErrorLayout() {
        View mView = this.findViewById(R.id.scaffold_mainframe_error_container_id);
        if (mView != null) {
            mView.setVisibility(View.GONE);
        }
    }

    public void setErrorView(@NonNull View errorView) {
        this.mErrorView = errorView;
    }

    public void setErrorLayoutRes(@LayoutRes int resLayout, @IdRes int id) {
        this.mClickId = id;
        if (this.mClickId <= 0) {
            this.mClickId = -1;
        }
        this.mErrorLayoutRes = resLayout;
        if (this.mErrorLayoutRes <= 0) {
            this.mErrorLayoutRes = R.layout.scaffold_web_error_page;
        }
    }

    @Override
    public AbsAgentWebUIController provide() {
        return this.mAgentWebUIController;
    }


    public void bindWebView(android.webkit.WebView view) {
        if (this.mWebView == null) {
            this.mWebView = view;
        }
    }

    public void bindWebView(com.tencent.smtt.sdk.WebView view) {
        if (this.x5WebView == null) {
            this.x5WebView = view;
        }
    }

    android.webkit.WebView getWebView() {
        return this.mWebView;
    }

    com.tencent.smtt.sdk.WebView getX5WebView() {
        return this.x5WebView;
    }

}
