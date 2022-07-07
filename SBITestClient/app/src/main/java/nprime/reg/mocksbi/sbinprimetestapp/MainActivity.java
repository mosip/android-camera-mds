package nprime.reg.mocksbi.sbinprimetestapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.mdm.DataObjects.CaptureRequestDeviceDetailDto;
import com.mdm.DataObjects.CaptureRequestDto;
import com.mdm.DataObjects.DiscoverRequestDto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
/**
 * @author NPrime Technologies
 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_DISCOVER = 1;
    private static final int REQUEST_INFO = 2;
    private static final int REQUEST_CAPTURE = 3;

    MaterialButton btnDiscover, btnInfo, btnCapture, btnShare;
    MaterialTextView textBox;
    String appID = null;
    String serialNo = null;

    Context context;
    private String responseData = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnDiscover = findViewById(R.id.discover);
        btnInfo = findViewById(R.id.info);
        btnCapture = findViewById(R.id.capture);
        btnShare = findViewById(R.id.share);
        textBox = findViewById(R.id.textbox);
        textBox.setMovementMethod(new ScrollingMovementMethod());

        btnDiscover.setOnClickListener(view -> {
            responseData = null;
            textBox.setText("");
            discover();
        });

        btnInfo.setOnClickListener(view -> {
            responseData = null;
            textBox.setText("");
            info();
        });

        btnCapture.setOnClickListener(view -> {
            responseData = null;
            textBox.setText("");
            capture();
        });

        btnShare.setOnClickListener(view -> {
            try {
                //String data = textBox.getText().toString();
                if (null != responseData && !responseData.isEmpty()) {
                    String path = MainActivity.this.getFilesDir().getAbsolutePath();
                    File file = new File(path);
                    File txtFile = new File(file, "response.txt");

                    FileOutputStream fOut = new FileOutputStream(txtFile);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(responseData);
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();

                    Uri uri = FileProvider.getUriForFile(MainActivity.this, "nprime.reg.sbi.sbinprimetestapp.fileprovider", txtFile);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("plain/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(share, "Share file"));
                }else{
                    Toast.makeText(MainActivity.this, "perform discover/info/capture to get response", Toast.LENGTH_SHORT).show();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    private void initViews() {
        btnInfo.setEnabled(appID != null);
        btnCapture.setEnabled(serialNo != null);
    }

    private void discover(){
        try{
            Intent intent = new Intent();
            intent.setAction("sbi.reg.device");

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
            activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
            final boolean isIntentSafe = activities.size() > 0;

            if(isIntentSafe) {
                DiscoverRequestDto discoverRequestDto = new DiscoverRequestDto();
                discoverRequestDto.type = "Face";

                intent.putExtra("input", new ObjectMapper().writeValueAsBytes(discoverRequestDto));
                startActivityForResult(intent, REQUEST_DISCOVER);
            }else {
                Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void info(){
        Intent intent = new Intent();
        intent.setAction(appID + ".info");

        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
        activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final boolean isIntentSafe = activities.size() > 0;

        if(isIntentSafe) {
            String packageName;
            for (ResolveInfo activity : activities) {
                if(activity.activityInfo.applicationInfo.packageName.equals(appID)) {
                    packageName = activity.activityInfo.applicationInfo.packageName;
                    intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                    startActivityForResult(intent, REQUEST_INFO);
                    break;
                }
            }
        }else {
            Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void capture(){
        try {
            Intent intent = new Intent();
            intent.setAction(appID + ".rcapture");

            PackageManager packageManager = this.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
            activities.sort(new ResolveInfo.DisplayNameComparator(packageManager));
            final boolean isIntentSafe = activities.size() > 0;
            if(isIntentSafe) {
                if(null == serialNo){
                    Toast.makeText(MainActivity.this, "Perform info request", Toast.LENGTH_SHORT).show();
                    return;
                }
                CaptureRequestDto captureRequestDto = new CaptureRequestDto();
                captureRequestDto.env = "Production";
                captureRequestDto.purpose = "Registration";
                captureRequestDto.specVersion = "0.9.5";
                captureRequestDto.timeout = 10000;
                captureRequestDto.captureTime = "2021-07-18T17:56:11Z";
                captureRequestDto.domainUri = "https://extint1.mosip.net";
                captureRequestDto.transactionId = "1626630971975";
                CaptureRequestDeviceDetailDto bio = new CaptureRequestDeviceDetailDto();
                bio.type = "Face";
                bio.count = 1;
                bio.bioSubType = new String[]{"UNKNOWN"};
                bio.requestedScore = 40;
                bio.deviceId = serialNo;
                bio.deviceSubId = "0";
                bio.previousHash = "";
                List<CaptureRequestDeviceDetailDto> mosipBioRequest = new ArrayList<>();
                mosipBioRequest.add(bio);
                captureRequestDto.mosipBioRequest = mosipBioRequest;
                captureRequestDto.customOpts = null;

                String packageName;
                for (ResolveInfo activity : activities) {
                    if(activity.activityInfo.applicationInfo.packageName.equals(appID)) {
                        packageName = activity.activityInfo.applicationInfo.packageName;
                        intent.setComponent(new ComponentName(packageName, activity.activityInfo.name));
                        intent.putExtra("input", new ObjectMapper().writeValueAsBytes(captureRequestDto));
                        startActivityForResult(intent, REQUEST_CAPTURE);
                        break;
                    }
                }
            }else {
                Toast.makeText(MainActivity.this, "Supported apps not found", Toast.LENGTH_SHORT).show();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(REQUEST_DISCOVER == requestCode){
            if(Activity.RESULT_OK == resultCode){
                try{
                    if(null != data) {
                        if (data.hasExtra("Response")) {
                            String response = data.getStringExtra("Response");
                            JSONArray respJsonArray = new JSONArray(response);
                            JSONObject respJsonObject = respJsonArray.getJSONObject(0);
                            JSONObject errorObject = respJsonObject.getJSONObject("error");
                            if (0 == errorObject.getInt("errorCode")) {
                                appID = respJsonObject.getString("callbackId");
                                textBox.setText("Discover Response :\n" + response);
                                Toast.makeText(MainActivity.this, appID, Toast.LENGTH_SHORT).show();
                            }else {
                                textBox.setText(errorObject.toString());
                            }
                        }else {
                            textBox.setText("Response not found");
                        }
                    }else {
                        textBox.setText("Response not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            initViews();
        }else if(REQUEST_INFO == requestCode){
            if(Activity.RESULT_OK == resultCode){
                try {
                    if(null != data) {
                        if (data.hasExtra("Response")) {
                            String response = data.getStringExtra("Response");
                            responseData = response;
                            JSONArray respJsonArray = new JSONArray(response);
                            JSONObject errorObject = respJsonArray.getJSONObject(0).getJSONObject("error");
                            if (0 == errorObject.getInt("errorCode")) {
                                String deviceInfo = (respJsonArray.getJSONObject(0)).getString("deviceInfo");
                                byte[] payload = getPayloadBuffer(deviceInfo);

                                JSONObject infoObject = new JSONObject(new String(payload));
                                String digitalId = infoObject.getString("digitalId");
                                byte[] digitalIdPayload = getPayloadBuffer(digitalId);

                                JSONObject digitalIdObj = new JSONObject(new String(digitalIdPayload));
                                serialNo = digitalIdObj.getString("serialNo");
                                textBox.setText("Info Response :\n" + truncateData(response));
                            }else{
                                textBox.setText(response);
                            }
                        }else {
                            textBox.setText("Info response not found");
                        }
                    }else {
                        textBox.setText("Info rsponse not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            initViews();
        }else if(REQUEST_CAPTURE == requestCode){
            if(Activity.RESULT_OK == resultCode){
                try {
                    if(null != data) {
                        Uri uri = data.getData();
                        if (null != uri) {
                            InputStream respData = getContentResolver().openInputStream(uri);
                            BufferedReader r = new BufferedReader(new InputStreamReader(respData));
                            StringBuilder total = new StringBuilder();
                            for (String line; (line = r.readLine()) != null; ) {
                                total.append(line).append('\n');
                            }
                            String response = total.toString();
                            JSONObject resposeObject = new JSONObject(response);
                            if(resposeObject.has("biometrics")) {
                                responseData = response;
                                JSONArray biometricsArray = resposeObject.getJSONArray("biometrics");
                                JSONObject errObject = (biometricsArray.getJSONObject(0)).getJSONObject("error");
                                if (0 == errObject.getInt("errorCode")) {
                                    textBox.setText("Capture response :\n" + truncateData(responseData));
                                } else {
                                    textBox.setText(response);
                                }
                            }else{
                                textBox.setText(resposeObject.toString());
                            }
                        }else {
                            textBox.setText("Capture response not found");
                        }
                    }else {
                        textBox.setText("Capture response not found");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getPayloadBuffer(String responseToken) {
        byte[] payLoad = null;
        try {
            String[] responseTokenArray = responseToken.split("\\.");
            payLoad = java.util.Base64.getUrlDecoder().decode(responseTokenArray[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payLoad;
    }

    public void nprimeWebsite(View view) {
        Intent updateIntent = new Intent(Intent.ACTION_VIEW);
        updateIntent.setData(Uri.parse("https://www.nprime.in/"));
        startActivity(updateIntent);
    }

    private String truncateData(String data){
        if(null != data && data.length() > 1000){
            return data.substring(0, 900) + "....||...." + data.substring(data.length() - 90);
        }else {
            return data;
        }
    }
}