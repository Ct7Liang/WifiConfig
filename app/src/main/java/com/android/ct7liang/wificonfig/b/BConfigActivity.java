package com.android.ct7liang.wificonfig.b;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.ct7liang.wificonfig.R;
import com.android.ct7liang.wificonfig.a.SystemUtils;
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

        initStatusBar();
        etName = findViewById(R.id.et_name);
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
                        showInfoWindow("配网失败");
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

                String pswd = etPswd.getText().toString().trim();

                if (TextUtils.isEmpty(pswd)){
                    showInfoWindow("密码不能为空");
                    return;
                }

                loadingView.setVisibility(View.VISIBLE);

                StartaiAirkissManager.getInstance().startAirKiss(pswd, ssid, "".getBytes(), 1000 * 60, 0, 5);

                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (SystemUtils.isConnectedWifi(this)){
            ssid = SystemUtils.getSSID(this);
            etName.setText(ssid);
        }else{
            ssid = null;
            etName.setText("未检测到有WIFI连接");
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
                ssid = SystemUtils.getSSID(context);
                etName.setText(ssid);
                ToastUtils.showStatic(BConfigActivity.this, "检测到已经连接WIFI");
            }else{
                ssid = null;
                etName.setText("未检测到有WIFI连接");
                ToastUtils.showStatic(BConfigActivity.this, "未检测到有WIFI连接");
            }
        }
    }

}