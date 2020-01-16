package com.android.ct7liang.wificonfig.a;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.ct7liang.wificonfig.R;
import com.android.ct7liang.wificonfig.b.SystemUtils;
import com.android.ct7liang.wificonfig.base.BaseActivity;
import com.ct7liang.tangyuan.utils.LogUtils;
import com.ct7liang.tangyuan.utils.ToastUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class AConfigActivity extends BaseActivity {

    private EditText etName;
    private EditText etPswd;
    private String ssid;
    private NetworkChangedReceiver networkChangedReceiver;

    @Override
    public int setLayout() {
        return R.layout.activity_aconfig;
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
        TextView tvCommit = findViewById(R.id.btn);

        networkChangedReceiver = new NetworkChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangedReceiver, intentFilter);

        tvCommit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.btn:
                checkConfig();
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
                ToastUtils.showStatic(AConfigActivity.this, "检测到已经连接WIFI");
            }else{
                ssid = null;
                etName.setText("未检测到有WIFI连接");
                ToastUtils.showStatic(AConfigActivity.this, "未检测到有WIFI连接");
            }
        }
    }

    private void checkConfig(){
        String pswd = etPswd.getText().toString().trim();
        if (TextUtils.isEmpty(ssid)){
            ToastUtils.showStatic(mAct, "SSID不能为空, 检查是否已成功连接到网络");
            return;
        }
        if (TextUtils.isEmpty(pswd)){
            ToastUtils.showStatic(mAct, "Wifi密码不能为空");
            return;
        }
        LogUtils.write("ssid: " + ssid);
        new AirKissTask(this, new AirKissEncoder(ssid, pswd)).execute();
    }

    private class AirKissTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnDismissListener {
        private static final int PORT = 10000;
        private final byte DUMMY_DATA[] = new byte[1500];
        private static final int REPLY_BYTE_CONFIRM_TIMES = 5;
        private ProgressDialog mDialog;
        private Context mContext;
        private DatagramSocket mSocket;
        private char mRandomChar;
        private AirKissEncoder mAirKissEncoder;
        private volatile boolean mDone = false;

        public AirKissTask(Activity activity, AirKissEncoder encoder) {
            mContext = activity;
            mDialog = new ProgressDialog(mContext);
            mDialog.setOnDismissListener(this);
            mRandomChar = encoder.getRandomChar();
            mAirKissEncoder = encoder;
        }

        @Override
        protected void onPreExecute() {
            this.mDialog.setMessage("Wifi配网中,请稍后");
            this.mDialog.show();

            new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[15000];
                    try {
                        DatagramSocket udpServerSocket = new DatagramSocket(PORT);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        int replyByteCounter = 0;
                        udpServerSocket.setSoTimeout(5*1000);
                        while (true) {
                            if (getStatus() == Status.FINISHED){
                                break;
                            }
                            try {
                                udpServerSocket.receive(packet);
                                byte receivedData[] = packet.getData();
                                for (byte b : receivedData) {
                                    if (b == mRandomChar){
                                        replyByteCounter++;
                                    }
                                }
                                if (replyByteCounter > REPLY_BYTE_CONFIRM_TIMES) {
                                    mDone = true;
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        udpServerSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void sendPacketAndSleep(int length) {
            try {
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA, length, InetAddress.getByName("255.255.255.255"), PORT);
                mSocket.send(pkg);
                Thread.sleep(4);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mSocket = new DatagramSocket();
                mSocket.setBroadcast(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int encoded_data[] = mAirKissEncoder.getEncodedData();
            for (int i = 0; i < encoded_data.length; ++i) {
                sendPacketAndSleep(encoded_data[i]);
                if (i % 200 == 0) {
                    if (isCancelled() || mDone){
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onCancelled(Void params) {
            showInfoWindow("配网操作已取消");
        }

        @Override
        protected void onPostExecute(Void params) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
            showInfoWindow(mDone?"配网成功":"配网失败,请重试或检查设备是否已连接网络");
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mDone){
                return;
            }
            this.cancel(true);
        }
    }

}
