package com.taobao.hotfix.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.taobao.hotfix.HotFixManager;
import com.taobao.hotfix.PatchLoadStatusListener;

public class MainActivity extends Activity {
    private TextView mStatusTv;
    private String mStatusStr;

    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initHotfix();
    }

    /**
     * 为了测试的方便, initialize放在activity中, 但是实际上initialize应该放在Application的onCreate方法中, 需要尽可能早的做初始化.
     */
    private void initHotfix() {
        HotFixManager.getInstance().setContext(this.getApplication())
                .setAppVersion(MainApplication.appVersion)
                .setAppId(MainApplication.appId)
                .setAesKey(null)
                .setSupportHotpatch(true)
                .setEnableDebug(true)
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onload(final int mode, final int code, final String info, final int handlePatchVersion) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Mode:")
                                        .append(mode)
                                        .append(" Code:")
                                        .append(code)
                                        .append(" Info:")
                                        .append(info)
                                        .append(" HandlePatchVersion:")
                                        .append(handlePatchVersion);
                                updateConsole(stringBuilder.toString());
                            }
                        });
                    }
                }).initialize();

        if (Build.VERSION.SDK_INT >= 23) {
            requestExternalStoragePermission();
        }
    }

    /**
     * 如果本地补丁放在了外部存储卡中, 6.0以上需要申请读外部存储卡权限才能够使用. 应用内部存储则不受影响
     */
    private void requestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    updateConsole("local external storage patch is invalid as not read external storage permission");
                }
                break;
            default:
        }
    }

    private void init() {
        mStatusTv = (TextView) this.findViewById(R.id.tv_status);
        mStatusStr = "";
    }

    /**
     * 更新监控台的输出信息
     *
     * @param content 更新内容
     */
    private void updateConsole(String content) {
        mStatusStr += content + "\n";
        if (mStatusTv != null) {
            mStatusTv.setText(mStatusStr);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_download:
                HotFixManager.getInstance().queryNewHotPatch();
                break;
            case R.id.btn_test:
                BaseBug.test(MainActivity.this);
                break;
            case R.id.btn_kill:
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
            case R.id.btn_clean_version:
                HotFixManager.getInstance().cleanPatches(true);
                break;
            case R.id.btn_clean_console:
                mStatusStr = "";
                updateConsole("");
                break;
            default:
        }
    }

}
