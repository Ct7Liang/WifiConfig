package com.android.ct7liang.wificonfig.base;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import com.android.ct7liang.wificonfig.R;
import com.android.ct7liang.wificonfig.a.WindowUtils;
import com.ct7liang.tangyuan.BasisActivity;
import com.ct7liang.tangyuan.utils.ScreenUtil;

public abstract class BaseActivity extends BasisActivity {

    protected void setTitleBar(){
        findViewById(R.id.title_bar).setPadding(0, ScreenUtil.getUtils().getStatusHeight(mAct), 0, 0);
        findViewById(R.id.iv_back).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_back:
                finish();
                break;
        }
    }

    /**
     * 用于显示一些是失败信息或者提示信息
     * @param string 信息描述
     */
    protected void showInfoWindow(final String string){
        new WindowUtils().create(this, R.layout.window_info_ys_camera, 0, 0, new WindowUtils.OnContentViewInitListener() {
            @Override
            public void onContentViewInited(final Dialog dialog, View contentView) {
                TextView tv = contentView.findViewById(R.id.tv_explain);
                tv.setText(string);
                contentView.findViewById(R.id.commit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });
    }

}
