package com.android.ct7liang.wificonfig.b;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ct7liang.wificonfig.R;
import com.android.ct7liang.wificonfig.a.AConfigActivity;
import com.android.ct7liang.wificonfig.a.AirKissEncoder;
import com.android.ct7liang.wificonfig.a.SystemUtils;
import com.android.ct7liang.wificonfig.base.BaseActivity;
import com.ct7liang.tangyuan.utils.LogUtils;
import com.ct7liang.tangyuan.utils.ToastUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class BConfigActivity extends BaseActivity {

    private EditText etName;
    private EditText etPswd;
    private String ssid;
    private String bssid;
    private NetworkChangedReceiver networkChangedReceiver;
    private EsptouchAsyncTask4 mTask;

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
        initStatusBar();
        etName = findViewById(R.id.et_name);
        etPswd = findViewById(R.id.et_pswd);
        TextView tvCommit1 = findViewById(R.id.btn1);
        TextView tvCommit2 = findViewById(R.id.btn2);

        networkChangedReceiver = new NetworkChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangedReceiver, intentFilter);

        tvCommit1.setOnClickListener(this);
        tvCommit2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.btn1:
                Log.i("ct7liang","-------------");
                config(0);
                break;
            case R.id.btn2:
                Log.i("ct7liang","-------------");
                config(1);
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (SystemUtils.isConnectedWifi(this)){
            ssid = SystemUtils.getSSID(this);
            bssid = SystemUtils.getBssid(this);
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


    private void config(int type){
        if(TextUtils.isEmpty(ssid)){
            ToastUtils.showStatic(BConfigActivity.this, "SSID不能为空");
            return;
        }

        if(TextUtils.isEmpty(bssid)){
            ToastUtils.showStatic(BConfigActivity.this, "BSSID不能为空");
            return;
        }

        String password = etPswd.getText().toString().trim();
        if(TextUtils.isEmpty(password)){
            ToastUtils.showStatic(BConfigActivity.this, "WIFI密码不能为空");
            return;
        }

        Log.i("ct7liang","-------------");

        LogUtils.write(ssid + ", " + bssid + ", " + password);

        byte[] ssids = ByteUtil.getBytesByString(ssid);
        byte[] passwords = ByteUtil.getBytesByString(password);
        byte[] bssids = TouchNetUtil.parseBssid2bytes(bssid);
        byte[] deviceCount = "1".getBytes();
        byte[] broadcast = {(byte) (type)};

        if (mTask != null) {
            mTask.cancelEsptouch();
        }
        mTask = new EsptouchAsyncTask4(this);
        mTask.execute(ssids, bssids, passwords, deviceCount, broadcast);
    }

    private static class EsptouchAsyncTask4 extends AsyncTask<byte[], IEsptouchResult, List<IEsptouchResult>> {
        private WeakReference<BConfigActivity> mActivity;

        private final Object mLock = new Object();
        private ProgressDialog mProgressDialog;
        private AlertDialog mResultDialog;
        private IEsptouchTask mEsptouchTask;

        EsptouchAsyncTask4(BConfigActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mResultDialog != null) {
                mResultDialog.dismiss();
            }
            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
            Activity activity = mActivity.get();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage("Wifi配网中,请稍后");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    synchronized (mLock) {
                        if (mEsptouchTask != null) {
                            mEsptouchTask.interrupt();
                        }
                    }
                }
            });

            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getText(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            synchronized (mLock) {
                                if (mEsptouchTask != null) {
                                    mEsptouchTask.interrupt();
                                }
                            }
                        }
                    });
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(IEsptouchResult... values) {
            Context context = mActivity.get();
            if (context != null) {
                IEsptouchResult result = values[0];
                LogUtils.write("EspTouchResult: " + result);
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            BConfigActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                byte[] broadcastData = params[4];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();
                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, context);
                mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                mEsptouchTask.setEsptouchListener(new IEsptouchListener() {
                    @Override
                    public void onEsptouchResultAdded(IEsptouchResult result) {
                        EsptouchAsyncTask4.this.publishProgress();
                    }
                });
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }

        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            BConfigActivity activity = mActivity.get();
            activity.mTask = null;
            mProgressDialog.dismiss();
            if (result == null) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage("建立 Esptouch 任务失败, 端口可能被其他程序占用")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            // check whether the task is cancelled and no results received
            IEsptouchResult firstResult = result.get(0);
            if (firstResult.isCancelled()) {
                return;
            }
            // the task received some results including cancelled while
            // executing before receiving enough results

            if (!firstResult.isSuc()) {
                mResultDialog = new AlertDialog.Builder(activity)
                        .setMessage("配网失败,请重试或检查设备是否已连接网络")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                mResultDialog.setCanceledOnTouchOutside(false);
                return;
            }

            ArrayList<CharSequence> resultMsgList = new ArrayList<>(result.size());
            for (IEsptouchResult touchResult : result) {
                String message = "配网成功: BSSID = " + touchResult.getBssid() + ", 地址 = " + touchResult.getInetAddress().getHostAddress();
                resultMsgList.add(message);
            }
            CharSequence[] items = new CharSequence[resultMsgList.size()];
            mResultDialog = new AlertDialog.Builder(activity)
                    .setTitle("完成")
                    .setItems(resultMsgList.toArray(items), null)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            mResultDialog.setCanceledOnTouchOutside(false);
        }
    }


}
