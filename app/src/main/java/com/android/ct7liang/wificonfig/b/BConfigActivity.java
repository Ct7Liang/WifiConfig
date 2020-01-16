package com.android.ct7liang.wificonfig.b;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.android.ct7liang.wificonfig.R;
import com.android.ct7liang.wificonfig.base.BaseActivity;
import com.ct7liang.tangyuan.utils.ToastUtils;
import com.tencent.mm.plugin.exdevice.jni.C2JavaExDevice;

import cn.com.startai.airkisssender.StartaiAirkissManager;

public class BConfigActivity extends BaseActivity {

//    https://github.com/luobinxin/airkisssdk

    private EditText etName;
    private EditText etPswd;
    private String ssid;
    private NetworkChangedReceiver networkChangedReceiver;
    private View loadingView;

    @Override
    public int setLayout() {
        return R.layout.activity_bconfig;
    }

    @Override
    protected void setStatusBar() {
        setTitleBar();
    }

    @Override
    public void initSurface() {

        loadingView = findViewById(R.id.view_loading);

        loadingView.setOnClickListener(this);

        findViewById(R.id.cancel).setOnClickListener(this);

        initStatusBar();
        etName = findViewById(R.id.et_name);
        etName.setEnabled(false);
        etPswd = findViewById(R.id.et_pswd);
        TextView tvCommit1 = findViewById(R.id.btn1);

        networkChangedReceiver = new NetworkChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangedReceiver, intentFilter);

        tvCommit1.setOnClickListener(this);

        StartaiAirkissManager.getInstance().setAirKissListener(new C2JavaExDevice.OnAirKissListener() {
            @Override
            public void onAirKissSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingView.setVisibility(View.GONE);
                        showInfoWindow("配网成功");
                    }
                });

            }

            @Override
            public void onAirKissFailed(int error) {
                StartaiAirkissManager.getInstance().stopAirKiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingView.setVisibility(View.GONE);
                        showInfoWindow("配网完成");
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.btn1:

                if ("未检测到有WIFI连接".equals(ssid)){
                    showInfoWindow("请打开Wifi开关,连接wifi");
                    return;
                }

                if ("获取ssid失败,请手动输入当前wifi名称".equals(ssid)){
                    showInfoWindow("请输入当前已连接Wifi的名称");
                    return;
                }

                String pswd = etPswd.getText().toString().trim();
                if (TextUtils.isEmpty(pswd)){
                    showInfoWindow("密码不能为空");
                    return;
                }

                hideInput();

                loadingView.setVisibility(View.VISIBLE);
                StartaiAirkissManager.getInstance().startAirKiss(pswd, ssid, "".getBytes(), 1000 * 60, 0, 5);
                break;
            case R.id.cancel:
                showInfoWindow("配网已取消");
                StartaiAirkissManager.getInstance().stopAirKiss();
                loadingView.setVisibility(View.GONE);
                break;

            case R.id.view_loading:

                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (SystemUtils.isConnectedWifi(this)){
            ssid = SystemUtils.getSSID(this);
            if (ssid.equals("") || ssid.contains("<unknown ssid>")){
                ssid = "获取ssid失败,请手动输入当前wifi名称";
                etName.setText("获取ssid失败,请手动输入当前wifi名称");
                etName.setEnabled(true);
            }else{
                etName.setText(ssid);
            }
        }else{
            ssid = "未检测到有WIFI连接";
            etName.setText("未检测到有WIFI连接");
            etName.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangedReceiver);
    }

    public class NetworkChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SystemUtils.isConnectedWifi(context)){
                ToastUtils.showStatic(BConfigActivity.this, "检测到已经连接WIFI");
                ssid = SystemUtils.getSSID(context);
                if (ssid.equals("") || ssid.contains("<unknown ssid>")){
                    ssid = "获取ssid失败,请手动输入当前wifi名称";
                    etName.setText("获取ssid失败,请手动输入当前wifi名称");
                    etName.setEnabled(true);
                }else{
                    etName.setText(ssid);
                }
            }else{
                ToastUtils.showStatic(BConfigActivity.this, "未检测到有WIFI连接");
                ssid = "未检测到有WIFI连接";
                etName.setText("未检测到有WIFI连接");
                etName.setEnabled(false);
            }
        }
    }

    /**
     * 隐藏键盘
     */
    protected void hideInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View v = getWindow().peekDecorView();
        if (null != v) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

}