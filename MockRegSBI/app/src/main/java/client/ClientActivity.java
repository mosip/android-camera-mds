package client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.dto.CaptureDetail;
import nprime.reg.mocksbi.dto.CaptureRequestDeviceDetailDto;
import nprime.reg.mocksbi.dto.CaptureRequestDto;
import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceDiscoveryRequestDetail;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.DiscoverDto;
import nprime.reg.mocksbi.dto.Error;
import nprime.reg.mocksbi.secureLib.DeviceKeystore;
import nprime.reg.mocksbi.utility.DeviceConstants;

/**
 * @author NPrime Technologies
 */

public class ClientActivity extends AppCompatActivity {

    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_INFO = 2;
    private static final int REQUEST_CAPTURE = 3;

    private static final int HANDLER_DISPLAY_TOAST = 0;
    private static final int HANDLER_DISPLAY_CAPTURE_RESPONSE = 1;
    private static final int HANDLER_DISPLAY_EMPTY_SCREEN = 2;
    private static final int HANDLER_DISPLAY_INFO_RESPONSE = 3;
    private static final int HANDLER_DISPLAY_PROGRESS_BAR_SCREEN = 4;


    MaterialButton btnInfo, btnRCapture, btnDiscover, btnCapture;
    MaterialTextView textBox, manufacturer, modelId, deviceId, deviceStatus, textBoxLabel;
    ImageButton btnShareResponse;
    TableRow deviceIdRow;
    ConstraintLayout emptyScreen, responseScreen, progressBarScreen;

    static String appID = null;
    String serialNo = null;

    private String responseData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Toolbar toolbar = findViewById(R.id.toolbar_client);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        btnInfo = findViewById(R.id.info);
        btnRCapture = findViewById(R.id.rcapture);
        btnDiscover = findViewById(R.id.discover);
        btnCapture = findViewById(R.id.capture);
        textBoxLabel = findViewById(R.id.response_label);
        textBox = findViewById(R.id.textbox);
        manufacturer = findViewById(R.id.manufacturer);
        modelId = findViewById(R.id.model_id);
        deviceId = findViewById(R.id.device_id);
        deviceStatus = findViewById(R.id.device_status);
        deviceIdRow = findViewById(R.id.device_id_row);
        emptyScreen = findViewById(R.id.empty_layout);
        responseScreen = findViewById(R.id.response_layout);
        progressBarScreen = findViewById(R.id.client_progress_layout);
        btnShareResponse = findViewById(R.id.share_response);

        textBox.setMovementMethod(new ScrollingMovementMethod());

        initViews();
        btnInfo.setOnClickListener(view -> {
            if (null == appID) {
                discover();
            } else {
                textBox.setText("");
                info();
            }
        });

        btnRCapture.setOnClickListener(view -> {
            textBox.setText("");
            rCapture();
        });


        btnDiscover.setOnClickListener(view -> {
            textBox.setText("");
            discover();
        });

        btnCapture.setOnClickListener(view -> {
            textBox.setText("");
            rCapture();
        });

        btnShareResponse.setOnClickListener(view -> {
            try {
                //String data = textBox.getText().toString();
                if (null != responseData && !responseData.isEmpty()) {
                    String path = ClientActivity.this.getFilesDir().getAbsolutePath();
                    File file = new File(path);
                    File txtFile = new File(file, "response.txt");

                    FileOutputStream fOut = new FileOutputStream(txtFile);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(responseData);
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();

                    Uri uri = FileProvider.getUriForFile(ClientActivity.this, "nprime.reg.mocksbi.fileprovider", txtFile);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("plain/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(share, "Share file"));
                } else {
                    showEmptyScreen();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        discover();
    }

    private void initViews() {
        btnRCapture.setEnabled(serialNo != null);
        btnCapture.setEnabled(serialNo != null);
    }

    private void discover() {
        try {
            Intent intent = new Intent();
            intent.setAction("io.sbi.device");

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            final boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                String packageName = null;
                for (ResolveInfo activity : activities) {
                    if (activity.activityInfo.applicationInfo.packageName.equals("nprime.reg.mocksbi")) {
                        packageName = activity.activityInfo.applicationInfo.packageName;
                        intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                        DeviceDiscoveryRequestDetail discoverRequestDto = new DeviceDiscoveryRequestDetail();
                        discoverRequestDto.type = "Finger";

                        intent.putExtra("input", new ObjectMapper().writeValueAsBytes(discoverRequestDto));
                        startActivityForResult(intent, REQUEST_DISCOVER);
                        break;
                    }
                }
                if (null == packageName) {
                    Toast.makeText(ClientActivity.this, "Supported app not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void info() {
        Intent intent = new Intent();
        intent.setAction(appID + ".Info");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            String packageName;
            for (ResolveInfo activity : activities) {
                if (appID.startsWith(activity.activityInfo.applicationInfo.packageName)) {
                    packageName = activity.activityInfo.applicationInfo.packageName;
                    intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                    startActivityForResult(intent, REQUEST_INFO);
                    break;
                }
            }
        } else {
            Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void rCapture() {
        try {
            Intent intent = new Intent();
            intent.setAction(appID + ".rCapture");

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
            final boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe) {
                if (null == serialNo) {
                    Toast.makeText(ClientActivity.this, "Perform info request", Toast.LENGTH_SHORT).show();
                    return;
                }
                CaptureRequestDto captureRequestDto = new CaptureRequestDto();
                captureRequestDto.env = DeviceConstants.ENVIRONMENT;
                captureRequestDto.purpose = DeviceConstants.DeviceUsage.Registration.toString();
                captureRequestDto.specVersion = DeviceConstants.MDSVERSION;
                captureRequestDto.timeout = 10000;
                captureRequestDto.captureTime = "2021-07-18T17:56:11Z";
                captureRequestDto.domainUri = DeviceConstants.DOMAIN_URI;
                captureRequestDto.transactionId = "1626630971975";
                CaptureRequestDeviceDetailDto bio = new CaptureRequestDeviceDetailDto();
                bio.type = "Finger";
                bio.count = "4";
                bio.bioSubType = new String[]{"UNKNOWN"};
                bio.requestedScore = 40;
                bio.deviceId = serialNo;
                bio.deviceSubId = "2";
                bio.previousHash = "";
                List<CaptureRequestDeviceDetailDto> mosipBioRequest = new ArrayList<>();
                mosipBioRequest.add(bio);
                captureRequestDto.bio = mosipBioRequest;
                captureRequestDto.customOpts = null;

                String packageName;
                for (ResolveInfo activity : activities) {
                    if (appID.startsWith(activity.activityInfo.applicationInfo.packageName)) {
                        packageName = activity.activityInfo.applicationInfo.packageName;
                        intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                        intent.putExtra("input", new ObjectMapper().writeValueAsBytes(captureRequestDto));
                        startActivityForResult(intent, REQUEST_CAPTURE);
                        break;
                    }
                }
            } else {
                Toast.makeText(ClientActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showResponse(String responseLabel, String data) {
        emptyScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.GONE);
        responseScreen.setVisibility(View.VISIBLE);
        textBoxLabel.setText(responseLabel);
        if (data.length() > 1000) {
            data = data.substring(0, 900) + "....||...." + data.substring(data.length() - 90, data.length());
        }
        textBox.setText(data);
    }

    private void showEmptyScreen() {
        textBoxLabel.setText("Response : ");
        textBox.setText("");
        responseScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.GONE);
        emptyScreen.setVisibility(View.VISIBLE);
        responseData = null;
    }

    private void showProgressBarScreen() {
        emptyScreen.setVisibility(View.GONE);
        responseScreen.setVisibility(View.GONE);
        progressBarScreen.setVisibility(View.VISIBLE);
        textBox.setText("");
        //responseData = null;
    }

    public void sendMessage(int what, Object obj) {
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        handler.sendMessage(message);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_DISPLAY_CAPTURE_RESPONSE:
                    String captureData = (String) msg.obj;
                    showResponse("Capture Response", captureData);
                    break;
                case HANDLER_DISPLAY_TOAST:
                    String toastText = (String) msg.obj;
                    Toast.makeText(ClientActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    break;
                case HANDLER_DISPLAY_EMPTY_SCREEN:
                    showEmptyScreen();
                    break;
                case HANDLER_DISPLAY_INFO_RESPONSE:
                    String infoData = (String) msg.obj;
                    showResponse("Info Response", infoData);
                    break;
                case HANDLER_DISPLAY_PROGRESS_BAR_SCREEN:
                    showProgressBarScreen();
                    break;
                default:
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_DISCOVER == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                try {
                    if (null != data) {
                        if (data.hasExtra("response")) {
                            byte[] response = data.getByteArrayExtra("response");
                            ObjectMapper ob = new ObjectMapper();
                            List<DiscoverDto> list = ob.readValue(response,
                                    new TypeReference<List<DiscoverDto>>() {
                                    });

                            if (!list.isEmpty()) {
                                String encodedDigitalID = list.get(0).digitalId;
                                if (!encodedDigitalID.isEmpty()) {
                                    byte[] digitalIdBytes = Base64.getUrlDecoder().decode(encodedDigitalID);
                                    JSONObject digitalIDObj = new JSONObject(new String(digitalIdBytes));
                                    if (digitalIDObj.has("make")) {
                                        manufacturer.setText(digitalIDObj.getString("make"));
                                    }
                                    if (digitalIDObj.has("model")) {
                                        modelId.setText(digitalIDObj.getString("model"));
                                    }
                                    if (list.get(0).deviceStatus != null) {
                                        deviceStatus.setText(list.get(0).deviceStatus);
                                    }
                                    String strDeviceId = digitalIDObj.getString("serialNo");
                                    if (!strDeviceId.isEmpty()) {
                                        deviceIdRow.setVisibility(View.VISIBLE);
                                        deviceId.setText(strDeviceId);
                                    } else {
                                        deviceIdRow.setVisibility(View.GONE);
                                    }
                                    appID = list.get(0).callbackId;
                                    showResponse("Discover response :", ob.writeValueAsString(list.get(0)));
                                } else {
                                    Toast.makeText(ClientActivity.this, "Digital ID error", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(ClientActivity.this, "Discover failed - No Devices", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ClientActivity.this, "Discover failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ClientActivity.this, "Discover failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            initViews();
        } else if (REQUEST_INFO == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                try {
                    if (null != data) {
                        if (data.hasExtra("response")) {
                            byte[] response = data.getByteArrayExtra("response");
                            List<DeviceInfoResponse> list = new ObjectMapper().readValue(response,
                                    new TypeReference<List<DeviceInfoResponse>>() {
                                    });
                            Error errorObject = list.get(0).error;
                            if ("0".equals(errorObject.errorCode)) {
                                String deviceInfo = list.get(0).deviceInfo;
                                byte[] payload = getPayloadBufferFromJwt(deviceInfo);

                                JSONObject infoObject = new JSONObject(new String(payload));
                                String digitalId = infoObject.getString("digitalId");
                                byte[] digitalIdPayload = getPayloadBufferFromJwt(digitalId);

                                JSONObject digitalIdObj = new JSONObject(new String(digitalIdPayload));
                                serialNo = digitalIdObj.getString("serialNo");
                                if (!serialNo.isEmpty()) {
                                    deviceIdRow.setVisibility(View.VISIBLE);
                                    deviceId.setText(serialNo);
                                }
                                if (infoObject.has("deviceStatus")) {
                                    deviceStatus.setText(infoObject.getString("deviceStatus"));
                                }
                                showResponse("Info response :", list.get(0).toString());
                                responseData = list.get(0).toString();
                            } else {
                                showResponse("Info response", list.get(0).toString());
                                deviceId.setText("");
                                deviceIdRow.setVisibility(View.GONE);
                                deviceStatus.setText("Not Ready");
                                responseData = list.get(0).toString();
                            }
                        } else {
                            showEmptyScreen();
                            Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showEmptyScreen();
                        Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            initViews();
        } else if (REQUEST_CAPTURE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                if (null != data) {
                    Uri uri = data.getParcelableExtra("response");
                    if (null != uri) {
                        new Thread(() -> {
                            try {
                                sendMessage(HANDLER_DISPLAY_PROGRESS_BAR_SCREEN, null);
                                InputStream respData = getContentResolver().openInputStream(uri);
                                byte[] bytes = IOUtils.toByteArray(respData);
                                CaptureResponse resposeObject = new ObjectMapper().readValue(bytes, CaptureResponse.class);
                                if (resposeObject.biometrics != null && !resposeObject.biometrics.isEmpty()) {
                                    List<CaptureDetail> biometrics = resposeObject.biometrics;
                                    Error errObject = (biometrics.get(0)).error;
                                    responseData = resposeObject.toString();
                                    sendMessage(HANDLER_DISPLAY_CAPTURE_RESPONSE, responseData);
                                    if (!errObject.errorCode.equals("0")) {
                                        sendMessage(HANDLER_DISPLAY_TOAST, errObject.toString());
                                    }
                                } else {
                                    responseData = resposeObject.toString();
                                    sendMessage(HANDLER_DISPLAY_CAPTURE_RESPONSE, responseData);
                                }
                            } catch (Exception e) {
                                sendMessage(HANDLER_DISPLAY_EMPTY_SCREEN, null);
                                sendMessage(HANDLER_DISPLAY_TOAST, "Capture error");
                                e.printStackTrace();
                            }
                        }).start();
                    } else {
                        showEmptyScreen();
                        Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showEmptyScreen();
                    Toast.makeText(ClientActivity.this, "Response Not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                showEmptyScreen();
            }
        }
    }

    public byte[] getPayloadBufferFromJwt(String responseToken) {
        byte[] payLoad = null;
        try {
            String[] responseTokenArray = responseToken.split("\\.");
            payLoad = Base64.getUrlDecoder().decode(responseTokenArray[1]);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return payLoad;
    }

    public void nprimeWebsite(View view) {
        Intent updateIntent = new Intent(Intent.ACTION_VIEW);
        updateIntent.setData(Uri.parse("https://www.nprime.in/"));
        startActivity(updateIntent);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, ConfigurationActivity.class);
        startActivity(intent);
    }

    public void validateCertificate(View view) {
        DeviceKeystore keystore = new DeviceKeystore(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String keyAlias = sharedPreferences.getString(ClientConstants.REG_KEY_ALIAS, "");
        String keystorePwd = sharedPreferences.getString(ClientConstants.REG_KEY_STORE_PASSWORD, "");
        String ftm_keyAlias = sharedPreferences.getString(ClientConstants.FTM_KEY_ALIAS, "");
        String ftm_keystorePwd = sharedPreferences.getString(ClientConstants.FTM_KEY_STORE_PASSWORD, "");

        if (keystore.checkCertificateCredentials(ClientConstants.REG_P12_FILE_NAME, keyAlias, keystorePwd)) {
            Toast.makeText(this, "Device key credentials are valid.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Device key validation failed.", Toast.LENGTH_SHORT).show();
        }

        if (keystore.checkCertificateCredentials(ClientConstants.FTM_P12_FILE_NAME, ftm_keyAlias, ftm_keystorePwd)) {
            Toast.makeText(this, "FTM key credentials are valid.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "FTM key validation failed.", Toast.LENGTH_SHORT).show();
        }
    }
}