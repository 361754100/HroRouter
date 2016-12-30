package com.hro.router;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.hro.router.data.DataManager;
import com.hro.router.util.Constant;
import com.hro.router.util.StringUtil;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SettingsActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button settingSaveBtn = (Button)findViewById(R.id.settingSaveBtn);
        settingSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText alarm_phone_txt = (EditText)findViewById(R.id.alarm_phone);
                EditText alarm_dist_txt = (EditText)findViewById(R.id.alarm_dist);
                EditText tim_account_txt = (EditText)findViewById(R.id.tim_account);
                CheckBox ckBox = (CheckBox)findViewById(R.id.isMonitor);


                String alarm_phone = alarm_phone_txt.getText().toString();
                String alarm_dist = alarm_dist_txt.getText().toString();
                String tim_account = tim_account_txt.getText().toString();

                Constant.ALARM_PHONE = alarm_phone;
                Constant.TIM_ACCOUNT = tim_account;
                Constant.isMovingTrace = ckBox.isChecked();
                if(!"".equals(alarm_dist)) {
                    Constant.MAX_POINT_MOVING_DIST = Float.parseFloat(alarm_dist);
                }
                SettingsActivity.this.finish();
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
