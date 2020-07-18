package top.xuqingquan.web.publics;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import top.xuqingquan.utils.FileUtils;
import top.xuqingquan.utils.RealPath;
import top.xuqingquan.utils.Timber;
import top.xuqingquan.web.R;
import top.xuqingquan.web.nokernel.Action;
import top.xuqingquan.web.nokernel.ActionActivity;
import top.xuqingquan.web.nokernel.AgentWebPermissions;
import top.xuqingquan.web.nokernel.FileParcel;
import top.xuqingquan.web.nokernel.PermissionInterceptor;
import top.xuqingquan.web.nokernel.WebConfig;
import top.xuqingquan.web.nokernel.WebUtils;

import static top.xuqingquan.utils.PermissionUtils.getDeniedPermissions;
import static top.xuqingquan.utils.PermissionUtils.hasPermission;
import static top.xuqingquan.web.nokernel.ActionActivity.KEY_ACTION;
import static top.xuqingquan.web.nokernel.ActionActivity.KEY_FILE_CHOOSER_INTENT;
import static top.xuqingquan.web.nokernel.ActionActivity.KEY_FROM_INTENTION;
import static top.xuqingquan.web.nokernel.ActionActivity.KEY_URI;

public class FileChooser {
    /**
     * Activity
     */
    private Activity mActivity;
    /**
     * ValueCallback
     */
    private android.webkit.ValueCallback<Uri> sysUriValueCallback;
    /**
     * ValueCallback<Uri[]> After LOLLIPOP
     */
    private android.webkit.ValueCallback<Uri[]> sysUriValueCallbacks;
    /**
     * ValueCallback
     */
    private com.tencent.smtt.sdk.ValueCallback<Uri> x5UriValueCallback;
    /**
     * ValueCallback<Uri[]> After LOLLIPOP
     */
    private com.tencent.smtt.sdk.ValueCallback<Uri[]> x5UriValueCallbacks;
    /**
     * Activity Request Code
     */
    private static final int REQUEST_CODE = 0x254;
    /**
     * 当前系统是否高于 Android 5.0 ；
     */
    private boolean mIsAboveLollipop;
    /**
     * android.webkit.WebChromeClient.FileChooserParams 封装了 Intent ，mAcceptType  等参数
     */
    private android.webkit.WebChromeClient.FileChooserParams sysFileChooserParams;
    /**
     * android.webkit.WebChromeClient.FileChooserParams 封装了 Intent ，mAcceptType  等参数
     */
    private com.tencent.smtt.sdk.WebChromeClient.FileChooserParams x5FileChooserParams;
    /**
     * 如果是通过 JavaScript 打开文件选择器 ，那么 mJsChannelCallback 不能为空
     */
    private JsChannelCallback mJsChannelCallback;
    /**
     * 是否为Js Channel
     */
    private boolean mJsChannel;
    /**
     * TAG
     */
    private static final String TAG = FileChooser.class.getSimpleName();
    /**
     * 当前 android.webkit.WebView
     */
    private android.webkit.WebView sysWebView;
    private com.tencent.smtt.sdk.WebView x5WebView;
    /**
     * 是否为 Camera State
     */
    private boolean mCameraState = false;
    /**
     * 是否调用摄像头后  调用的是摄像模式  默认是拍照
     */
    private boolean mVideoState = false;
    /**
     * 权限拦截
     */
    private PermissionInterceptor mPermissionInterceptor;
    /**
     * FROM_INTENTION_CODE 用于表示当前Action
     */
    private int FROM_INTENTION_CODE = 21;
    /**
     * 当前 AbsAgentWebUIController
     */
    private WeakReference<AbsAgentWebUIController> mAgentWebUIController;
    /**
     * 选择文件类型
     */
    private String mAcceptType;
    /**
     * 修复某些特定手机拍照后，立刻获取照片为空的情况
     */
    private final static int MAX_WAIT_PHOTO_MS = 8 * 1000;

    private FileChooser(Builder builder) {
        this.mActivity = builder.mActivity;
        this.mIsAboveLollipop = builder.mIsAboveLollipop;
        this.mJsChannel = builder.mJsChannel;
        if (WebConfig.enableTbs()) {
            this.x5UriValueCallback = builder.x5UriValueCallback;
            this.x5UriValueCallbacks = builder.x5UriValueCallbacks;
            this.x5FileChooserParams = builder.x5FileChooserParams;
            this.x5WebView = builder.x5WebView;
            this.mAgentWebUIController = new WeakReference<>(AgentWebUtils.getAgentWebUIControllerByWebView(this.x5WebView));
        } else {
            this.sysUriValueCallback = builder.sysUriValueCallback;
            this.sysUriValueCallbacks = builder.sysUriValueCallbacks;
            this.sysFileChooserParams = builder.sysFileChooserParams;
            this.sysWebView = builder.sysWebView;
            this.mAgentWebUIController = new WeakReference<>(AgentWebUtils.getAgentWebUIControllerByWebView(this.sysWebView));
        }
        if (this.mJsChannel) {
            this.mJsChannelCallback = JsChannelCallback.create(builder.mJsChannelCallback);
        }
        this.mPermissionInterceptor = builder.mPermissionInterceptor;
        this.mAcceptType = builder.mAcceptType;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void openFileChooser() {
        if (!WebUtils.isUIThread()) {
            WebUtils.runInUiThread(this::openFileChooser);
            return;
        }

        openFileChooserInternal();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void fileChooser() {
        if (getDeniedPermissions(mActivity, AgentWebPermissions.STORAGE).isEmpty()) {
            touchOffFileChooserAction();
        } else {
            Action mAction = Action.createPermissionsAction(AgentWebPermissions.STORAGE);
            mAction.setFromIntention(FROM_INTENTION_CODE >> 2);
            ActionActivity.setPermissionListener(mPermissionListener);
            ActionActivity.start(mActivity, mAction);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void touchOffFileChooserAction() {
        Action mAction = new Action();
        mAction.setAction(Action.ACTION_FILE);
        ActionActivity.setChooserListener(getChooserListener());
        mActivity.startActivity(new Intent(mActivity, ActionActivity.class).putExtra(KEY_ACTION, mAction)
                .putExtra(KEY_FILE_CHOOSER_INTENT, getFileChooserIntent()));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Intent getFileChooserIntent() {
        if (WebConfig.enableTbs()) {
            if (mIsAboveLollipop && x5FileChooserParams != null) {
                Intent mIntent = x5FileChooserParams.createIntent();
                if (mIntent != null) {
                    return mIntent;
                }
            }
        } else {
            if (mIsAboveLollipop && sysFileChooserParams != null) {
                Intent mIntent = sysFileChooserParams.createIntent();
                if (mIntent != null) {
                    return mIntent;
                }
            }
        }
        Intent i = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i.setAction(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            i.setAction(Intent.ACTION_GET_CONTENT);
        }
        i.addCategory(Intent.CATEGORY_OPENABLE);
        if (TextUtils.isEmpty(this.mAcceptType)) {
            i.setType("*/*");
        } else {
            i.setType(this.mAcceptType);
        }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(i, "");
    }

    private ActionActivity.ChooserListener getChooserListener() {
        return (requestCode, resultCode, data) -> {
            Timber.i("request:" + requestCode + "  resultCode:" + resultCode);
            onIntentResult(requestCode, resultCode, data);
        };
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openFileChooserInternal() {
        boolean needVideo = false;
        // 是否直接打开文件选择器
        if (WebConfig.enableTbs()) {
            if (this.mIsAboveLollipop && this.x5FileChooserParams != null && this.x5FileChooserParams.getAcceptTypes() != null) {
                boolean needCamera = false;
                String[] types = this.x5FileChooserParams.getAcceptTypes();
                for (String typeTmp : types) {
                    Timber.i("typeTmp:" + typeTmp);
                    if (TextUtils.isEmpty(typeTmp)) {
                        continue;
                    }
                    if (typeTmp.contains("*/") || typeTmp.contains("image/")) {
                        needCamera = true;
                        break;
                    }
                    if (typeTmp.contains("video/")) {  //调用摄像机拍摄  这是录像模式
                        needVideo = true;
                        mVideoState = true;
                    }
                }
                if (!needCamera && !needVideo) {
                    touchOffFileChooserAction();
                    return;
                }
            }
        } else {
            if (this.mIsAboveLollipop && this.sysFileChooserParams != null && this.sysFileChooserParams.getAcceptTypes() != null) {
                boolean needCamera = false;
                String[] types = this.sysFileChooserParams.getAcceptTypes();
                for (String typeTmp : types) {
                    Timber.i("typeTmp:" + typeTmp);
                    if (TextUtils.isEmpty(typeTmp)) {
                        continue;
                    }
                    if (typeTmp.contains("*/") || typeTmp.contains("image/")) {
                        needCamera = true;
                        break;
                    }
                    if (typeTmp.contains("video/")) {  //调用摄像机拍摄  这是录像模式
                        needVideo = true;
                        mVideoState = true;
                    }
                }
                if (!needCamera && !needVideo) {
                    touchOffFileChooserAction();
                    return;
                }
            }
        }
        if (!TextUtils.isEmpty(this.mAcceptType) && !this.mAcceptType.contains("*/") && !this.mAcceptType.contains("image/")) {
            touchOffFileChooserAction();
            return;
        }
        Timber.i("controller:" + this.mAgentWebUIController.get() + "   mAcceptType:" + mAcceptType);
        if (this.mAgentWebUIController.get() != null) {
            if (WebConfig.enableTbs()) {
                this.mAgentWebUIController
                        .get()
                        .onSelectItemsPrompt(this.x5WebView, x5WebView.getUrl(),
                                new String[]{mActivity.getString(R.string.scaffold_camera),
                                        mActivity.getString(R.string.scaffold_file)}, getCallBack());
            } else {
                this.mAgentWebUIController
                        .get()
                        .onSelectItemsPrompt(this.sysWebView, sysWebView.getUrl(),
                                new String[]{mActivity.getString(R.string.scaffold_camera),
                                        mActivity.getString(R.string.scaffold_file)}, getCallBack());
            }
            Timber.i("open");
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Handler.Callback getCallBack() {
        return msg -> {
            switch (msg.what) {
                case 0:
                    mCameraState = true;
                    onCameraAction();
                    break;
                case 1:
                    mCameraState = false;
                    fileChooser();
                    break;
                default:
                    cancel();
                    break;
            }
            return true;
        };
    }


    private void onCameraAction() {
        if (mActivity == null) {
            return;
        }
        if (mPermissionInterceptor != null) {
            if (WebConfig.enableTbs()) {
                if (mPermissionInterceptor.intercept(FileChooser.this.x5WebView.getUrl(), AgentWebPermissions.CAMERA, "camera")) {
                    cancel();
                    return;
                }
            } else {
                if (mPermissionInterceptor.intercept(FileChooser.this.sysWebView.getUrl(), AgentWebPermissions.CAMERA, "camera")) {
                    cancel();
                    return;
                }
            }
        }
        Action mAction = new Action();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> deniedPermissions = checkNeedPermission();
            if (!deniedPermissions.isEmpty()) {
                mAction.setAction(Action.ACTION_PERMISSION);
                mAction.setPermissions(deniedPermissions.toArray(new String[]{}));
                mAction.setFromIntention(FROM_INTENTION_CODE >> 3);
                ActionActivity.setPermissionListener(this.mPermissionListener);
                ActionActivity.start(mActivity, mAction);
            } else {
                openCameraAction();
            }
        } else {
            openCameraAction();
        }

    }

    private List<String> checkNeedPermission() {
        List<String> deniedPermissions = new ArrayList<>();
        if (!hasPermission(mActivity, AgentWebPermissions.CAMERA)) {
            deniedPermissions.add(AgentWebPermissions.CAMERA[0]);
        }
        if (!hasPermission(mActivity, AgentWebPermissions.STORAGE)) {
            deniedPermissions.addAll(Arrays.asList(AgentWebPermissions.STORAGE));
        }
        return deniedPermissions;
    }

    private void openCameraAction() {
        Action mAction = new Action();
        if (mVideoState) {  //调用摄像
            mAction.setAction(Action.ACTION_VIDEO);
        } else {
            mAction.setAction(Action.ACTION_CAMERA);
        }
        ActionActivity.setChooserListener(this.getChooserListener());
        ActionActivity.start(mActivity, mAction);
    }

    private ActionActivity.PermissionListener mPermissionListener = (permissions, grantResults, extras) -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean tag = hasPermission(mActivity, permissions);
            permissionResult(tag, extras.getInt(KEY_FROM_INTENTION));
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void permissionResult(boolean grant, int from_intention) {
        if (from_intention == FROM_INTENTION_CODE >> 2) {
            if (grant) {
                touchOffFileChooserAction();
            } else {
                cancel();
                if (null != mAgentWebUIController.get()) {
                    mAgentWebUIController
                            .get()
                            .onPermissionsDeny(
                                    AgentWebPermissions.STORAGE,
                                    AgentWebPermissions.ACTION_STORAGE,
                                    "Open file chooser");
                }
                Timber.i("permission denied");
            }
        } else if (from_intention == FROM_INTENTION_CODE >> 3) {
            if (grant) {
                openCameraAction();
            } else {
                cancel();
                if (null != mAgentWebUIController.get()) {
                    mAgentWebUIController
                            .get()
                            .onPermissionsDeny(
                                    AgentWebPermissions.CAMERA,
                                    AgentWebPermissions.ACTION_CAMERA,
                                    "Take photo");
                }
                Timber.i("permission denied");
            }
        }
    }

    private void onIntentResult(int requestCode, int resultCode, Intent data) {
        Timber.i("request:" + requestCode + "  result:" + resultCode + "  data:" + data);
        if (REQUEST_CODE != requestCode) {
            return;
        }
        //用户已经取消
        if (resultCode == Activity.RESULT_CANCELED || data == null) {
            cancel();
            return;
        }
        if (resultCode != Activity.RESULT_OK) {
            cancel();
            return;
        }
        //通过Js获取文件
        if (mJsChannel) {
            convertFileAndCallback(mCameraState ? new Uri[]{data.getParcelableExtra(KEY_URI)} : processData(data));
            return;
        }
        //5.0以上系统通过input标签获取文件
        if (mIsAboveLollipop) {
            aboveLollipopCheckFilesAndCallback(mCameraState ? new Uri[]{data.getParcelableExtra(KEY_URI)} : processData(data), mCameraState);
            return;
        }
        //4.4以下系统通过input标签获取文件
        if (WebConfig.enableTbs()) {
            if (x5UriValueCallback == null) {
                cancel();
                return;
            }
        } else {
            if (sysUriValueCallback == null) {
                cancel();
                return;
            }
        }
        if (mCameraState) {
            if (WebConfig.enableTbs()) {
                x5UriValueCallback.onReceiveValue(data.getParcelableExtra(KEY_URI));
            } else {
                sysUriValueCallback.onReceiveValue(data.getParcelableExtra(KEY_URI));
            }
        } else {
            belowLollipopUriCallback(data);
        }
    }

    private void cancel() {
        if (mJsChannel) {
            mJsChannelCallback.call(null);
            return;
        }
        if (WebConfig.enableTbs()) {
            if (x5UriValueCallback != null) {
                x5UriValueCallback.onReceiveValue(null);
            }
            if (x5UriValueCallbacks != null) {
                x5UriValueCallbacks.onReceiveValue(null);
            }
        } else {
            if (sysUriValueCallback != null) {
                sysUriValueCallback.onReceiveValue(null);
            }
            if (sysUriValueCallbacks != null) {
                sysUriValueCallbacks.onReceiveValue(null);
            }
        }
    }


    private void belowLollipopUriCallback(Intent data) {
        if (data == null) {
            if (WebConfig.enableTbs()) {
                if (x5UriValueCallback != null) {
                    x5UriValueCallback.onReceiveValue(Uri.EMPTY);
                }
            } else {
                if (sysUriValueCallback != null) {
                    sysUriValueCallback.onReceiveValue(Uri.EMPTY);
                }
            }
            return;
        }
        Uri mUri = data.getData();
        if (WebConfig.enableTbs()) {
            Timber.i("belowLollipopUriCallback  -- >uri:" + mUri + "  sysUriValueCallback:" + sysUriValueCallback);
        } else {
            Timber.i("belowLollipopUriCallback  -- >uri:" + mUri + "  x5UriValueCallback:" + x5UriValueCallback);
        }
        if (WebConfig.enableTbs()) {
            if (x5UriValueCallback != null) {
                x5UriValueCallback.onReceiveValue(mUri);
            }
        } else {
            if (sysUriValueCallback != null) {
                sysUriValueCallback.onReceiveValue(mUri);
            }
        }
    }

    private Uri[] processData(Intent data) {
        if (data == null) {
            return null;
        }
        Uri[] datas = null;
        Uri target = data.getData();
        if (target != null) {
            try {
                String path = RealPath.getPath(mActivity, data.getData());
                return new Uri[]{Uri.fromFile(new File(path))};
            } catch (Throwable t) {
                return new Uri[]{target};
            }
        }
        ClipData mClipData = data.getClipData();
        if (mClipData != null && mClipData.getItemCount() > 0) {
            datas = new Uri[mClipData.getItemCount()];
            for (int i = 0; i < mClipData.getItemCount(); i++) {
                ClipData.Item mItem = mClipData.getItemAt(i);
                datas[i] = mItem.getUri();

            }
        }
        return datas;


    }

    private void convertFileAndCallback(final Uri[] uris) {
        if (uris == null || uris.length == 0) {
            mJsChannelCallback.call(null);
            return;
        }
        String[] paths = FileUtils.uriToPath(mActivity, uris);
        if (paths == null || paths.length == 0) {
            mJsChannelCallback.call(null);
            return;
        }
        int sum = 0;
        for (String path : paths) {
            if (TextUtils.isEmpty(path)) {
                continue;
            }
            File mFile = new File(path);
            if (!mFile.exists()) {
                continue;
            }
            sum += mFile.length();
        }

        if (sum > WebConfig.MAX_FILE_LENGTH) {
            if (mAgentWebUIController.get() != null) {
                mAgentWebUIController.get().onShowMessage(mActivity.getString(R.string.scaffold_max_file_length_limit, (WebConfig.MAX_FILE_LENGTH / 1024 / 1024) + ""), TAG.concat("|convertFileAndCallback"));
            }
            mJsChannelCallback.call(null);
            return;
        }
        new CovertFileThread(this.mJsChannelCallback, paths).start();
    }

    /**
     * 经过多次的测试，在小米 MIUI ， 华为 ，多部分为 Android 6.0 左右系统相机获取到的文件
     * length为0 ，导致前端 ，获取到的文件， 作预览的时候不正常 ，等待5S左右文件又正常了 ， 所以这里做了阻塞等待处理，
     */
    private void aboveLollipopCheckFilesAndCallback(final Uri[] datas, boolean isCamera) {
        String[] paths = FileUtils.uriToPath(mActivity, datas);
        if (WebConfig.enableTbs()) {
            if (x5UriValueCallbacks == null) {
                return;
            }
            if (!isCamera) {
                x5UriValueCallbacks.onReceiveValue(datas == null ? new Uri[]{} : datas);
                return;
            }
            if (mAgentWebUIController.get() == null) {
                x5UriValueCallbacks.onReceiveValue(null);
                return;
            }
            if (paths == null || paths.length == 0) {
                x5UriValueCallbacks.onReceiveValue(null);
                return;
            }
        } else {
            if (sysUriValueCallbacks == null) {
                return;
            }
            if (!isCamera) {
                sysUriValueCallbacks.onReceiveValue(datas == null ? new Uri[]{} : datas);
                return;
            }

            if (mAgentWebUIController.get() == null) {
                sysUriValueCallbacks.onReceiveValue(null);
                return;
            }
            if (paths == null || paths.length == 0) {
                sysUriValueCallbacks.onReceiveValue(null);
                return;
            }
        }
        final String path = paths[0];
        mAgentWebUIController.get().onLoading(mActivity.getString(R.string.scaffold_loading));
        if (WebConfig.enableTbs()) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new WaitPhotoRunnable(path, new AboveLCallback(x5UriValueCallbacks, datas, mAgentWebUIController)));
        } else {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new WaitPhotoRunnable(path, new AboveLCallback(sysUriValueCallbacks, datas, mAgentWebUIController)));
        }

    }

    private static final class AboveLCallback implements Handler.Callback {
        private android.webkit.ValueCallback<Uri[]> sysValueCallback;
        private com.tencent.smtt.sdk.ValueCallback<Uri[]> x5ValueCallback;
        private Uri[] mUris;
        private WeakReference<AbsAgentWebUIController> controller;

        private AboveLCallback(android.webkit.ValueCallback<Uri[]> valueCallbacks, Uri[] uris, WeakReference<AbsAgentWebUIController> controller) {
            this.sysValueCallback = valueCallbacks;
            this.mUris = uris;
            this.controller = controller;
        }

        private AboveLCallback(com.tencent.smtt.sdk.ValueCallback<Uri[]> valueCallbacks, Uri[] uris, WeakReference<AbsAgentWebUIController> controller) {
            this.x5ValueCallback = valueCallbacks;
            this.mUris = uris;
            this.controller = controller;
        }

        @Override
        public boolean handleMessage(final Message msg) {
            WebUtils.runInUiThread(AboveLCallback.this::safeHandleMessage);
            return false;
        }

        private void safeHandleMessage() {
            if (WebConfig.enableTbs()) {
                if (x5ValueCallback != null) {
                    x5ValueCallback.onReceiveValue(mUris);
                }
            } else {
                if (sysValueCallback != null) {
                    sysValueCallback.onReceiveValue(mUris);
                }
            }
            if (controller != null && controller.get() != null) {
                controller.get().onCancelLoading();
            }
        }
    }

    private static final class WaitPhotoRunnable implements Runnable {
        private String path;
        private Handler.Callback mCallback;

        private WaitPhotoRunnable(String path, Handler.Callback callback) {
            this.path = path;
            this.mCallback = callback;
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(path) || !new File(path).exists()) {
                if (mCallback != null) {
                    mCallback.handleMessage(Message.obtain(null, -1));
                }
                return;
            }
            int ms = 0;
            while (ms <= MAX_WAIT_PHOTO_MS) {
                ms += 300;
                SystemClock.sleep(300);
                File mFile = new File(path);
                if (mFile.length() > 0) {
                    if (mCallback != null) {
                        mCallback.handleMessage(Message.obtain(null, 1));
                        mCallback = null;
                    }
                    break;
                }
            }
            if (ms > MAX_WAIT_PHOTO_MS) {
                Timber.i("WaitPhotoRunnable finish!");
                if (mCallback != null) {
                    mCallback.handleMessage(Message.obtain(null, -1));
                }
            }
            mCallback = null;
            path = null;

        }
    }

    // 必须执行在子线程, 会阻塞直到文件转换完成;
    private static Queue<FileParcel> convertFile(String[] paths) throws Throwable {
        if (paths == null || paths.length == 0) {
            return null;
        }
        int tmp = Runtime.getRuntime().availableProcessors() + 1;
        int result = Math.min(paths.length, tmp);
        Executor mExecutor = Executors.newFixedThreadPool(result);
        final Queue<FileParcel> mQueue = new LinkedBlockingQueue<>();
        CountDownLatch mCountDownLatch = new CountDownLatch(paths.length);
        int i = 1;
        for (String path : paths) {
            Timber.i("path:" + path);
            if (TextUtils.isEmpty(path)) {
                mCountDownLatch.countDown();
                continue;
            }
            mExecutor.execute(new EncodeFileRunnable(path, mQueue, mCountDownLatch, i++));
        }
        mCountDownLatch.await();
        if (!((ThreadPoolExecutor) mExecutor).isShutdown()) {
            ((ThreadPoolExecutor) mExecutor).shutdownNow();
        }
        Timber.i("convertFile isShutDown:" + (((ThreadPoolExecutor) mExecutor).isShutdown()));
        return mQueue;
    }


    static class EncodeFileRunnable implements Runnable {
        private String filePath;
        private Queue<FileParcel> mQueue;
        private CountDownLatch mCountDownLatch;
        private int id;

        EncodeFileRunnable(String filePath, Queue<FileParcel> queue, CountDownLatch countDownLatch, int id) {
            this.filePath = filePath;
            this.mQueue = queue;
            this.mCountDownLatch = countDownLatch;
            this.id = id;
        }

        @Override
        public void run() {
            InputStream is = null;
            ByteArrayOutputStream os = null;
            try {
                File mFile = new File(filePath);
                if (mFile.exists()) {
                    is = new FileInputStream(mFile);
                    //noinspection ConstantConditions
                    if (is == null) {
                        return;
                    }
                    os = new ByteArrayOutputStream();
                    byte[] b = new byte[1024];
                    int len;
                    while ((len = is.read(b, 0, 1024)) != -1) {
                        os.write(b, 0, len);
                    }
                    mQueue.offer(new FileParcel(id, mFile.getAbsolutePath(), Base64.encodeToString(os.toByteArray(), Base64.DEFAULT)));
                    Timber.i("enqueue");
                } else {
                    Timber.i("File no exists");
                }

            } catch (Throwable e) {
                Timber.i("throwwable");
                e.printStackTrace();
            } finally {
                FileUtils.closeIO(is);
                FileUtils.closeIO(os);
                mCountDownLatch.countDown();
            }


        }
    }

    private static String convertFileParcelObjectsToJson(Collection<FileParcel> collection) {

        if (collection == null || collection.size() == 0) {
            return null;
        }
        Iterator<FileParcel> mFileParcels = collection.iterator();
        JSONArray mJSONArray = new JSONArray();
        try {
            while (mFileParcels.hasNext()) {
                JSONObject jo = new JSONObject();
                FileParcel mFileParcel = mFileParcels.next();
                jo.put("contentPath", mFileParcel.getContentPath());
                jo.put("fileBase64", mFileParcel.getFileBase64());
                jo.put("id", mFileParcel.getId());
                mJSONArray.put(jo);
            }
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
        return mJSONArray + "";
    }

    static class CovertFileThread extends Thread {

        private WeakReference<JsChannelCallback> mJsChannelCallback;
        private String[] paths;

        private CovertFileThread(JsChannelCallback JsChannelCallback, String[] paths) {
            super("agentweb-thread");
            this.mJsChannelCallback = new WeakReference<>(JsChannelCallback);
            this.paths = paths;
        }

        @Override
        public void run() {
            try {
                Queue<FileParcel> mQueue = convertFile(paths);
                String result = convertFileParcelObjectsToJson(mQueue);
                if (mJsChannelCallback != null && mJsChannelCallback.get() != null) {
                    mJsChannelCallback.get().call(result);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    static class JsChannelCallback {
        WeakReference<Handler.Callback> callback;

        JsChannelCallback(Handler.Callback callback) {
            this.callback = new WeakReference<>(callback);
        }

        public static JsChannelCallback create(Handler.Callback callback) {
            return new JsChannelCallback(callback);
        }

        void call(String value) {
            if (this.callback != null && this.callback.get() != null) {
                this.callback.get().handleMessage(Message.obtain(null, "JsChannelCallback".hashCode(), value));
            }
        }
    }

    static Builder newBuilder(Activity activity, android.webkit.WebView webView) {
        return new Builder().setActivity(activity).setWebView(webView);
    }

    static Builder newBuilder(Activity activity, com.tencent.smtt.sdk.WebView webView) {
        return new Builder().setActivity(activity).setWebView(webView);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private Activity mActivity;
        private android.webkit.ValueCallback<Uri> sysUriValueCallback;
        private android.webkit.ValueCallback<Uri[]> sysUriValueCallbacks;
        private com.tencent.smtt.sdk.ValueCallback<Uri> x5UriValueCallback;
        private com.tencent.smtt.sdk.ValueCallback<Uri[]> x5UriValueCallbacks;
        private boolean mIsAboveLollipop = false;
        private android.webkit.WebChromeClient.FileChooserParams sysFileChooserParams;
        private com.tencent.smtt.sdk.WebChromeClient.FileChooserParams x5FileChooserParams;
        private boolean mJsChannel = false;
        private android.webkit.WebView sysWebView;
        private com.tencent.smtt.sdk.WebView x5WebView;
        private PermissionInterceptor mPermissionInterceptor;
        private String mAcceptType = "*/*";
        private Handler.Callback mJsChannelCallback;

        Builder setAcceptType(String acceptType) {
            this.mAcceptType = acceptType;
            return this;
        }

        Builder setPermissionInterceptor(PermissionInterceptor permissionInterceptor) {
            mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public Builder setActivity(Activity activity) {
            mActivity = activity;
            return this;
        }

        Builder setUriValueCallback(android.webkit.ValueCallback<Uri> uriValueCallback) {
            sysUriValueCallback = uriValueCallback;
            mIsAboveLollipop = false;
            mJsChannel = false;
            sysUriValueCallbacks = null;
            return this;
        }

        Builder setUriValueCallbacks(android.webkit.ValueCallback<Uri[]> uriValueCallbacks) {
            sysUriValueCallbacks = uriValueCallbacks;
            mIsAboveLollipop = true;
            sysUriValueCallback = null;
            mJsChannel = false;
            return this;
        }

        Builder setUriValueCallback(com.tencent.smtt.sdk.ValueCallback<Uri> uriValueCallback) {
            x5UriValueCallback = uriValueCallback;
            mIsAboveLollipop = false;
            mJsChannel = false;
            x5UriValueCallbacks = null;
            return this;
        }

        Builder setUriValueCallbacks(com.tencent.smtt.sdk.ValueCallback<Uri[]> uriValueCallbacks) {
            x5UriValueCallbacks = uriValueCallbacks;
            mIsAboveLollipop = true;
            x5UriValueCallback = null;
            mJsChannel = false;
            return this;
        }

        Builder setFileChooserParams(android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {
            sysFileChooserParams = fileChooserParams;
            return this;
        }

        Builder setFileChooserParams(com.tencent.smtt.sdk.WebChromeClient.FileChooserParams fileChooserParams) {
            x5FileChooserParams = fileChooserParams;
            return this;
        }

        Builder setJsChannelCallback(Handler.Callback jsChannelCallback) {
            this.mJsChannelCallback = jsChannelCallback;
            mJsChannel = true;
            sysUriValueCallback = null;
            sysUriValueCallbacks = null;
            return this;
        }

        Builder setWebView(android.webkit.WebView webView) {
            sysWebView = webView;
            return this;
        }

        Builder setWebView(com.tencent.smtt.sdk.WebView webView) {
            x5WebView = webView;
            return this;
        }

        FileChooser build() {
            return new FileChooser(this);
        }
    }


}
