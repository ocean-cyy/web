package top.xuqingquan.web.system;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.DownloadListener;
import android.webkit.WebView;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.download.library.DownloadImpl;
import com.download.library.DownloadListenerAdapter;
import com.download.library.Extra;
import com.download.library.ResourceRequest;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import top.xuqingquan.web.R;
import top.xuqingquan.web.nokernel.Action;
import top.xuqingquan.web.nokernel.ActionActivity;
import top.xuqingquan.web.nokernel.AgentWebPermissions;
import top.xuqingquan.web.nokernel.PermissionInterceptor;
import top.xuqingquan.web.nokernel.WebUtils;
import top.xuqingquan.web.publics.AbsAgentWebUIController;
import top.xuqingquan.web.publics.AgentWebConfig;
import top.xuqingquan.web.publics.AgentWebUtils;
import top.xuqingquan.utils.FileUtils;
import top.xuqingquan.utils.Timber;
import top.xuqingquan.utils.NetUtils;
import top.xuqingquan.utils.PermissionUtils;

/**
 * Created by 许清泉 on 2019-06-19 23:29
 */
@SuppressWarnings("rawtypes")
public class DefaultDownloadImpl implements DownloadListener {
    /**
     * Application Context
     */
    protected Context mContext;
    protected ConcurrentHashMap<String, ResourceRequest> mDownloadTasks = new ConcurrentHashMap<>();
    /**
     * Activity
     */
    protected WeakReference<Activity> mActivityWeakReference;
    /**
     * 权限拦截
     */
    protected PermissionInterceptor mPermissionListener;
    /**
     * AbsAgentWebUIController
     */
    protected WeakReference<AbsAgentWebUIController> mAgentWebUIController;

    private static Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean isInstallDownloader;

    protected DefaultDownloadImpl(Activity activity, WebView webView, PermissionInterceptor permissionInterceptor) {
        this.mContext = activity.getApplicationContext();
        this.mActivityWeakReference = new WeakReference<>(activity);
        this.mPermissionListener = permissionInterceptor;
        this.mAgentWebUIController = new WeakReference<>(AgentWebUtils.getAgentWebUIControllerByWebView(webView));
        try {
            DownloadImpl.getInstance().with(this.mContext);
            isInstallDownloader = true;
        } catch (Throwable throwable) {
            Timber.e(throwable);
            isInstallDownloader = false;
        }
    }


    @Override
    public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimetype, final long contentLength) {
        if (!isInstallDownloader) {
            Timber.e("unable start download " + url + "; implementation 'com.download.library:Downloader:x.x.x'");
            return;
        }
        mHandler.post(() -> onDownloadStartInternal(url, userAgent, contentDisposition, mimetype, contentLength));
    }

    protected void onDownloadStartInternal(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (null == mActivityWeakReference.get() || mActivityWeakReference.get().isFinishing()) {
            return;
        }
        if (null != this.mPermissionListener) {
            if (this.mPermissionListener.intercept(url, AgentWebPermissions.STORAGE, "download")) {
                return;
            }
        }
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            if (mAgentWebUIController.get() != null) {
                mAgentWebUIController.get().onShowMessage(mContext.getString(R.string.scaffold_no_allow_download_file), "preDownload");
            } else {
                WebUtils.toastShowShort(mContext, mContext.getString(R.string.scaffold_no_allow_download_file));
            }
            return;
        }
        ResourceRequest resourceRequest = createResourceRequest(url);
        if (resourceRequest == null) {
            return;
        }
        this.mDownloadTasks.put(url, resourceRequest);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> mList = checkNeedPermission();
            if (mList.isEmpty()) {
                preDownload(url);
            } else {
                Action mAction = Action.createPermissionsAction(mList.toArray(new String[]{}));
                ActionActivity.setPermissionListener(getPermissionListener(url));
                ActionActivity.start(mActivityWeakReference.get(), mAction);
            }
        } else {
            preDownload(url);
        }
    }

    @Nullable
    protected ResourceRequest createResourceRequest(String url) {
        String fileName = convertUrl2FileName(url);
        File downloadFile = new File(FileUtils.getCacheFilePath(mContext), fileName);
        if (downloadFile.exists()) {
            Timber.d("文件已存在");
            if (null != mAgentWebUIController.get()) {
                mAgentWebUIController.get().onShowMessage(mContext.getString(R.string.scaffold_download_file_has_been_exist), "reDownload");
            }
            Intent mIntent = WebUtils.getCommonFileIntentCompat(mContext, downloadFile);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(mIntent);
            return null;
        }
        Timber.d("fileName=" + fileName);
        return DownloadImpl.getInstance()
                .with(url)
                .target(downloadFile, mContext.getPackageName() + ".ScaffoldFileProvider")
                .setEnableIndicator(true)
                .autoOpenIgnoreMD5();
    }

    @NonNull
    private String convertUrl2FileName(String url) {
        String fileName;
        try {
            int lastIndexOf = url.lastIndexOf("/");
            if (lastIndexOf != url.length() - 1) {//如果已经不是最后一项了
                lastIndexOf += 1;
            }
            if (url.contains("?")) {
                fileName = url.substring(lastIndexOf, url.indexOf("?"));
            } else {
                fileName = url.substring(lastIndexOf);
            }
        } catch (Throwable e) {
            fileName = url;
        }
        return fileName;
    }

    protected ActionActivity.PermissionListener getPermissionListener(final String url) {
        return (permissions, grantResults, extras) -> {
            if (checkNeedPermission().isEmpty()) {
                preDownload(url);
            } else {
                if (null != mAgentWebUIController.get()) {
                    mAgentWebUIController
                            .get()
                            .onPermissionsDeny(
                                    checkNeedPermission().
                                            toArray(new String[]{}),
                                    AgentWebPermissions.ACTION_STORAGE, "Download");
                }
                Timber.e("储存权限获取失败~");
            }

        };
    }

    protected List<String> checkNeedPermission() {
        List<String> deniedPermissions = new ArrayList<>();
        if (!PermissionUtils.hasPermission(mActivityWeakReference.get(), AgentWebPermissions.STORAGE)) {
            deniedPermissions.addAll(Arrays.asList(AgentWebPermissions.STORAGE));
        }
        return deniedPermissions;
    }

    protected void preDownload(String url) {
        Activity mActivity = mActivityWeakReference.get();
        if (null == mActivity || mActivity.isFinishing()) {
            return;
        }
        AbsAgentWebUIController mAgentWebUIController = this.mAgentWebUIController.get();
        if (mAgentWebUIController != null) {
            mAgentWebUIController.onDownloadPrompt(convertUrl2FileName(url), res -> {
                // 移动数据
                if (!isForceRequest(url) && NetUtils.checkNetworkType(mContext) > 1) {
                    showDialog(url);
                    return true;
                }
                performDownload(url);
                return true;
            });
        }
    }

    protected boolean isForceRequest(String url) {
        ResourceRequest resourceRequest = mDownloadTasks.get(url);
        if (null != resourceRequest) {
            return resourceRequest.getDownloadTask().isForceDownload();
        }
        return false;
    }

    protected void forceDownload(final String url) {
        ResourceRequest resourceRequest = mDownloadTasks.get(url);
        resourceRequest.setForceDownload(true);
        performDownload(url);
    }

    protected void showDialog(final String url) {
        Activity mActivity = mActivityWeakReference.get();
        if (null == mActivity || mActivity.isFinishing()) {
            return;
        }
        AbsAgentWebUIController mAgentWebUIController = this.mAgentWebUIController.get();
        if (null != mAgentWebUIController) {
            mAgentWebUIController.onForceDownloadAlert(createCallback(url));
        }
    }

    protected Handler.Callback createCallback(final String url) {
        return msg -> {
            forceDownload(url);
            return true;
        };
    }

    protected void performDownload(String url) {
        try {
            Timber.e("performDownload:" + url + " exist:" + DownloadImpl.getInstance().exist(url));
            // 该链接是否正在下载
            if (DownloadImpl.getInstance().exist(url)) {
                if (null != mAgentWebUIController.get()) {
                    mAgentWebUIController.get().onShowMessage(
                            mActivityWeakReference.get()
                                    .getString(R.string.scaffold_download_task_has_been_exist), "preDownload");
                }
                return;
            }
            ResourceRequest resourceRequest = mDownloadTasks.get(url);
            resourceRequest.addHeader("Cookie", AgentWebConfig.getCookiesByUrl(url));
            taskEnqueue(resourceRequest);
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
    }

    protected void taskEnqueue(ResourceRequest resourceRequest) {
        resourceRequest.enqueue(new DownloadListenerAdapter() {
            @Override
            public boolean onResult(Throwable throwable, Uri path, String url, Extra extra) {
                mDownloadTasks.remove(url);
                return super.onResult(throwable, path, url, extra);
            }
        });
    }

    public static DefaultDownloadImpl create(@NonNull Activity activity,
                                             @NonNull WebView webView,
                                             @Nullable PermissionInterceptor permissionInterceptor) {
        return new DefaultDownloadImpl(activity, webView, permissionInterceptor);
    }
}
