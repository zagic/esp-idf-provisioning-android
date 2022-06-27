// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.espressif.AppConstants;
import com.espressif.certificateLoader.certificateLoader;
import com.espressif.provisioning.DeviceConnectionEvent;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.listeners.ResponseListener;
import com.espressif.provisioning.utils.MessengeHelper;
import com.espressif.wifi_provisioning.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import espressif.CustomConfig;

public class WiFiConfigActivity extends AppCompatActivity {

    private static final String TAG = WiFiConfigActivity.class.getSimpleName();

    private static Context mContext;
    private TextView tvTitle, tvBack, tvCancel;
    private CardView btnNext, btnCertProvision,btnVerifyProvision;
    private TextView txtNextBtn;
    private Spinner spinCertificates;
    private ArrayAdapter<String> certificatesAdapter;
    List<String> certificatesList;

    private int CertificateStepIndicator =0;

    private EditText etSsid, etPassword;
    private ESPProvisionManager provisionManager;
    private String targetDeviceName;
    private Handler mToastHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext =this.getApplicationContext();
        setContentView(R.layout.activity_wifi_config);

        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());
        initViews();
        EventBus.getDefault().register(this);

        mToastHandler =new Handler();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        provisionManager.getEspDevice().disconnectDevice();
        super.onBackPressed();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceConnectionEvent event) {

        Log.d(TAG, "On Device Connection Event RECEIVED : " + event.getEventType());

        switch (event.getEventType()) {

            case ESPConstants.EVENT_DEVICE_DISCONNECTED:
                if (!isFinishing()) {
                    showAlertForDeviceDisconnected();
                }
                break;
        }
    }

    private View.OnClickListener nextBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            String ssid = etSsid.getText().toString();
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(ssid)) {
                etSsid.setError(getString(R.string.error_ssid_empty));
                return;
            }

            goToProvisionActivity(ssid, password);
        }
    };

    private View.OnClickListener cancelBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            provisionManager.getEspDevice().disconnectDevice();
            finish();
        }
    };

    private void initViews() {

        tvTitle = findViewById(R.id.main_toolbar_title);
        tvBack = findViewById(R.id.btn_back);
        tvCancel = findViewById(R.id.btn_cancel);
        etSsid = findViewById(R.id.et_ssid_input);
        etPassword = findViewById(R.id.et_password_input);

        String deviceName = provisionManager.getEspDevice().getDeviceName();
        if (!TextUtils.isEmpty(deviceName)) {
            String msg = String.format(getString(R.string.setup_instructions), deviceName);
            TextView tvInstructionMsg = findViewById(R.id.setup_instructions_view);
            tvInstructionMsg.setText(msg);
        }

        tvTitle.setText(R.string.title_activity_wifi_config);
        tvBack.setVisibility(View.GONE);
        tvCancel.setVisibility(View.VISIBLE);
        tvCancel.setOnClickListener(cancelBtnClickListener);

        btnNext = findViewById(R.id.btn_next);
        txtNextBtn = findViewById(R.id.text_btn);
        txtNextBtn.setText(R.string.btn_next);
        btnNext.setOnClickListener(nextBtnClickListener);

        btnCertProvision = findViewById(R.id.btn_certificate_provisiong);
        TextView tmpView = (TextView) btnCertProvision.findViewById(R.id.text_btn);
        tmpView.setText("Set Certificates");
        btnCertProvision.setOnClickListener(certProvisionBtnClickListener);


        btnVerifyProvision = findViewById(R.id.btn_read_certificate);
        tmpView = (TextView) btnCertProvision.findViewById(R.id.text_btn);
        tmpView.setText("Set Certificates");
        btnVerifyProvision.setOnClickListener(certReadBtnClickListener);
        tmpView = (TextView) btnVerifyProvision.findViewById(R.id.text_btn);
        tmpView.setText("Verify Certificates");

        certificatesList = new ArrayList<>();
        List<certificateLoader.CertificateGroup> certificateGroups = certificateLoader.getCertificateGroups();
        for(certificateLoader.CertificateGroup cc: certificateGroups){
            if(cc.hasPrivateKey && cc.hasId && cc.hasCertificatePem) {
                certificatesList.add(cc.DeviceName);
            }
        }
        spinCertificates = findViewById(R.id.certificate_spinner);
        certificatesAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,certificatesList.toArray(new String[certificatesList.size()]));
        certificatesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCertificates.setAdapter(certificatesAdapter);

    }

    private void goToProvisionActivity(String ssid, String password) {

        finish();
        Intent provisionIntent = new Intent(getApplicationContext(), ProvisionActivity.class);
        provisionIntent.putExtras(getIntent());
        provisionIntent.putExtra(AppConstants.KEY_WIFI_SSID, ssid);
        provisionIntent.putExtra(AppConstants.KEY_WIFI_PASSWORD, password);
        startActivity(provisionIntent);
    }

    private void showAlertForDeviceDisconnected() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.error_title);
        builder.setMessage(R.string.dialog_msg_ble_device_disconnection);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }

    private View.OnClickListener certProvisionBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(certificatesList.size()==0){
                Toast.makeText(mContext,"No certificate selected",Toast.LENGTH_LONG).show();
                return;
            }
            targetDeviceName = certificatesAdapter.getItem(spinCertificates.getSelectedItemPosition());
            CertificateStepIndicator = 1;
            String tmp = certificateLoader.loadCertId(targetDeviceName);
            if(tmp==null){
                mToastHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        toggleCustomProvisionButton(true);
                        Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            provisionManager.getEspDevice().customDataProvision(tmp, CustomConfig.CustomCommand.ConfigCertID,certificateProvisionListener);
            toggleCustomProvisionButton(false);
        }
    };
    private View.OnClickListener certReadBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(certificatesList.size()==0){
                Toast.makeText(mContext,"No local certificate to compare",Toast.LENGTH_LONG).show();
                return;
            }
            CertificateStepIndicator =1;

            provisionManager.getEspDevice().customDataProvision(" ", CustomConfig.CustomCommand.ReadThingName,certificateReadListener);
            toggleCustomProvisionButton(false);
        }
    };

    private void toggleCustomProvisionButton(boolean isEnable){
        btnVerifyProvision.setClickable(isEnable);
        btnCertProvision.setClickable(isEnable);
    }


    ResponseListener certificateReadListener=new ResponseListener() {

        @Override
        public void onSuccess(byte[] returnData) {
 //           Log.d(TAG, "custom read success"+new String(returnData));
            if(CertificateStepIndicator==1) { /* Device name received */
                CertificateStepIndicator++;
                MessengeHelper.parsedCustomMessage tmpMsg = MessengeHelper.parseCustomMessage(returnData);
                if(tmpMsg ==null || tmpMsg.message ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Read failed",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                targetDeviceName = tmpMsg.message;
                provisionManager.getEspDevice().customDataProvision(" ", CustomConfig.CustomCommand.ReadCertID,certificateReadListener);
            }else if(CertificateStepIndicator==2){   /* Device id received */
                CertificateStepIndicator++;
                String localCert = certificateLoader.loadCertId(targetDeviceName);
                if(localCert ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"No local cert matched ",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                MessengeHelper.parsedCustomMessage tmpMsg = MessengeHelper.parseCustomMessage(returnData);
                if(tmpMsg ==null || tmpMsg.message ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Read failed",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                String remoteCert = tmpMsg.message;
                if(!remoteCert.equals(localCert)){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                provisionManager.getEspDevice().customDataProvision(" ", CustomConfig.CustomCommand.ReadCertPem,certificateReadListener);

            }else if(CertificateStepIndicator==3){  /* Certificate pem received */
                CertificateStepIndicator++;
                String localCert = certificateLoader.loadCertPem(targetDeviceName);
                if(localCert ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"No local cert matched ",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                MessengeHelper.parsedCustomMessage tmpMsg = MessengeHelper.parseCustomMessage(returnData);
                if(tmpMsg ==null || tmpMsg.message ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Read failed",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                String remoteCert = tmpMsg.message;
                if(!remoteCert.equals(localCert)){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                provisionManager.getEspDevice().customDataProvision(" ", CustomConfig.CustomCommand.ReadPrivateKey,certificateReadListener);

            }else {  /* Private key received */
                String localCert = certificateLoader.loadPrivateKey(targetDeviceName);
                if(localCert ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"No local cert matched ",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                MessengeHelper.parsedCustomMessage tmpMsg = MessengeHelper.parseCustomMessage(returnData);
                if(tmpMsg ==null || tmpMsg.message ==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Read failed",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                String remoteCert = tmpMsg.message;
                if(!remoteCert.equals(localCert)){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                mToastHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        toggleCustomProvisionButton(true);
                        Toast.makeText(mContext ,"Certificate match to:"+ targetDeviceName,Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG, "Read all matched");

            }

        }

        @Override
        public void onFailure(Exception e) {
            mToastHandler.post(new Runnable() {
                @Override
                public void run() {
                    toggleCustomProvisionButton(true);
                    Toast.makeText(mContext ,"Read failed",Toast.LENGTH_LONG).show();
                }
            });

        }
    };

    ResponseListener certificateProvisionListener =new ResponseListener() {
        @Override
        public void onSuccess(byte[] returnData) {
            Log.d(TAG, "custom config success");
            if(CertificateStepIndicator==1){
                CertificateStepIndicator++;
                String tmp = certificateLoader.loadCertPem(targetDeviceName);
                if(tmp==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                provisionManager.getEspDevice().customDataProvision(tmp, CustomConfig.CustomCommand.ConfigCertPem,certificateProvisionListener);

            }else if(CertificateStepIndicator==2){
                CertificateStepIndicator++;
                String tmp = certificateLoader.loadPrivateKey(targetDeviceName);
                if(tmp==null){
                    mToastHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleCustomProvisionButton(true);
                            Toast.makeText(mContext ,"Cert not matched",Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                provisionManager.getEspDevice().customDataProvision(tmp, CustomConfig.CustomCommand.ConfigPrivateKey,certificateProvisionListener);

            }else if(CertificateStepIndicator==3){
                CertificateStepIndicator++;

                provisionManager.getEspDevice().customDataProvision(targetDeviceName, CustomConfig.CustomCommand.ConfigThingName,certificateProvisionListener);

            }else {
                mToastHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        toggleCustomProvisionButton(true);
                        Toast.makeText(mContext ,"config success",Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG, "custom config all done");

            }
        }

        @Override
        public void onFailure(Exception e) {
            Log.d(TAG, "custom config failed");
            mToastHandler.post(new Runnable() {
                @Override
                public void run() {
                    toggleCustomProvisionButton(true);
                    Toast.makeText(mContext ,"config failed",Toast.LENGTH_LONG).show();
                }
            });
        }
    };
}
