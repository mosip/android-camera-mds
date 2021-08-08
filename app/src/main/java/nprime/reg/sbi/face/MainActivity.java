package nprime.reg.sbi.face;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.textview.MaterialTextView;
import com.mdm.DataObjects.CaptureRequestDto;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import nprime.reg.sbi.faceCaptureApi.FaceCaptureResult;
import nprime.reg.sbi.googlevision.CameraActivity;
import nprime.reg.sbi.mds.DeviceMain;
import nprime.reg.sbi.mmc.MDServiceUtility;
import nprime.reg.sbi.scanner.ResponseGenerator.ResponseGenHelper;
import nprime.reg.sbi.utility.CommonDeviceAPI;
import nprime.reg.sbi.utility.DeviceConstants;
import nprime.reg.sbi.utility.Logger;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_PHONE_STATE = 1;
    private static final int PERMISSION_CAMERA = 2;

    private static final int REQUEST_CAPTURE = 1;
    private static final int REQUEST_INTERNAL_CAPTURE = 2;
    private static final int REQUEST_INIT_BEFORE_INFO = 3;
    private static final int REQUEST_INIT_BEFORE_DISCOVERY = 4;
    private static final int REQUEST_INIT_BEFORE_CAPTURE = 5;

    Button btnCapture, btnInit;
    MaterialTextView tvSerialNo;
    //TextView etSerialNo;

    public static Context context;
    private DeviceConstants.ServiceStatus currentStatus = DeviceConstants.ServiceStatus.NOTREADY;
    CaptureRequestDto captureRequestDto = null;
    String location = "com.nprime.reg.face";
    ObjectMapper ob;

    public static SharedPreferences sharedPreferences;
    public static boolean initSuccessful = false;

    private boolean skipOnStart = false;
    private boolean permissionGranted = false;
    private AlertDialog loadingAlertDialog = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        getWindow().setStatusBarColor(getResources().getColor(R.color.nprime_primary));

        context = this;
        //etSerialNo = (TextView) findViewById(R.id.tv_serialno);
        btnInit = (Button) findViewById(R.id.btn_init);
        btnCapture = (Button) findViewById(R.id.btn_capture);
        tvSerialNo = (MaterialTextView) findViewById(R.id.tv_serialno);

        if(null == DeviceMain.deviceMain) {
            DeviceMain.deviceMain = new DeviceMain();
        }

        tvSerialNo.setText(new CommonDeviceAPI().getSerialNumber());

        /*if(null != DeviceMain.deviceMain.mainSerialNumber && !DeviceMain.deviceMain.mainSerialNumber.isEmpty()){
            etSerialNo.setText(DeviceMain.deviceMain.mainSerialNumber);
        }*/

        btnInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if (ActivityCompat.checkSelfPermission(MDMainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MDMainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                    return;
                }else {
                    permissionGranted = true;
                }*/
                init();
            }
        });
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                    return;
                }else {
                    permissionGranted = true;
                }
                if(!isInitSuccessful()) {
                    Intent intent = new Intent(MainActivity.this, DeviceMain.class);
                    startActivity(intent);
                    return;
                }
                /*if(null != DeviceMain.deviceMain.mainSerialNumber && !DeviceMain.deviceMain.mainSerialNumber.isEmpty()){
                    etSerialNo.setText(DeviceMain.deviceMain.mainSerialNumber);
                }*/
                /*String captureInput = "{" +
                        "  \"url\": \"http://127.0.0.1:4501/capture\"," +
                        "  \"verb\": \"CAPTURE\"," +
                        "  \"headers\": null," +
                        "  \"body\": \"{\"env\":\"Developer\",\"purpose\":\"Auth\",\"specVersion\":\"0.9.5\",\"timeout\":10000,\"captureTime\":\"2021-05-31T08:31:14.550544Z\",\"domainUri\":\"default\",\"transactionId\":\"1622449874550\",\"bio\":[{\"type\":\"Face\",\"count\":0,\"bioSubType\":[],\"requestedScore\":40,\"deviceId\":\"12345\",\"deviceSubId\":0,\"previousHash\":\"E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855\"}],\"customOpts\":null}\"," +
                        "  \"streamUrl\": null" +
                        "}";*/
                //String captureInput = "{\"env\":\"Production\",\"purpose\":\"Registration\",\"specVersion\":\"0.9.5\",\"timeout\":10000,\"captureTime\":\"2021-07-31T08:31:14.550544Z\",\"transactionId\":\"1626879603493\",\"bio\":[{\"type\":\"Face\",\"count\":1,\"bioSubType\":[],\"requestedScore\":40,\"deviceId\":\"" + new CommonDeviceAPI().getSerialNumber() + "\",\"deviceSubId\":0,\"previousHash\":\"E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855\"}],\"customOpts\":null}";]
                String captureInput = "{\"env\":\"Developer\",\"purpose\":\"Registration\",\"specVersion\":\"0.9.5\",\"timeout\":10000,\"captureTime\":\"2021-07-21T15:00:03Z\",\"transactionId\":\"1626879603493\",\"bio\":[{\"type\":\"Face\",\"count\":1,\"exception\":[],\"requestedScore\":40,\"deviceId\":\"" + new CommonDeviceAPI().getSerialNumber() + "\",\"deviceSubId\":0,\"previousHash\":\"\",\"bioSubType\":[]}],\"customOpts\":null}";
                capture(REQUEST_INTERNAL_CAPTURE, captureInput);
            }
        });

        sharedPreferences = this.getSharedPreferences("NPrimeSBI", Context.MODE_PRIVATE);
        if(null == sharedPreferences.getString("SNAPSHOT_INFO", null)){
            try {
                InputStream in = null;
                in = getAssets().open("snapShot.info");
                byte[] keyfromDevice = readInputStream(in);
                if(null != keyfromDevice){
                    sharedPreferences.edit().putString("SNAPSHOT_INFO", Base64.encodeToString(keyfromDevice, Base64.DEFAULT)).apply();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(null == sharedPreferences.getString("KEY_ROTATION_INFO", null)){
            try {
                InputStream in = null;
                in = getAssets().open("keyRotation.info");
                byte[] keyfromDevice = readInputStream(in);
                if(null != keyfromDevice){
                    sharedPreferences.edit().putString("KEY_ROTATION_INFO", Base64.encodeToString(keyfromDevice, Base64.DEFAULT)).apply();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isInitSuccessful() {
        boolean status = false;
        /*if(!initSuccessful){
            status = false;
        }else {*/
            long lastInitTime = sharedPreferences.getLong("LastInitTimeMills", 0);
            if((new Date().getTime() - lastInitTime) < (24 * 60 * 60 * 1000)){
                status = true;
            }
        //}
        return status;
    }

    private void init() {
        //if(!initSuccessful) {
        Intent intent = new Intent(this, DeviceMain.class);
        startActivity(intent);
        //}
    }

    private byte[] readInputStream(InputStream in) {
        if (in == null) {
            return null;
        }

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream(in.available());
            byte[] buffer = new byte[16 * 1024];
            int bytesRead = in.read(buffer);
            while (bytesRead >= 0) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            return;
        }else {
            permissionGranted = true;
        }
        if(skipOnStart){
            skipOnStart = false;
            return;
        }
        handleIntents();
    }

    private void handleIntents() {
        if (currentStatus != DeviceConstants.ServiceStatus.BUSY){
            currentStatus = DeviceConstants.ServiceStatus.READY;
            /*if (0 != nRet){
                currentStatus = DeviceConstants.ServiceStatus.NOTREADY;

                if(!srNumber.isEmpty()){
                    DeviceMain.deviceMain.deviceCommonDeviceAPI.showAlwaysOnTopMessage("Iris Auth SBI", "Iris device changed, Please restart SBI.");
                    Logger.WriteLog(LOGTYPE.ERROR, "Iris device changed, Please restart SBI. Connected Serial Number : " + srNumber +
                            ", Earlier Serial Number : " + DeviceMain.deviceMain.mainSerialNumber);
                }
            }
            else{
                currentStatus = DeviceConstants.ServiceStatus.READY;
            }*/
        }
        String actionType = getIntent().getAction();
        switch (actionType) {
            case "sbi.reg.device": {
                if (!isInitSuccessful()) {
                    Intent intent = new Intent(this, DeviceMain.class);
                    startActivityForResult(intent, REQUEST_INIT_BEFORE_INFO);
                    return;
                }
                String szTs = new CommonDeviceAPI().getISOTimeStamp();
                String responseBody;

                String input = getIntent().getStringExtra("input");
                if (null != input && (input.toLowerCase().contains("{\"type\":\"face\"}") ||
                        input.toLowerCase().contains("{\"type\":\"biometric device\"}"))) {
                    String requestType = location;
                    responseBody = DeviceMain.deviceMain.discoverDevice(currentStatus, szTs, requestType);
                } else {
                    Map<String, Object> responseMap = new HashMap<>();
                    Map<String, Object> errorMap = new LinkedHashMap<>();
                    errorMap.put("errorCode", "301");
                    errorMap.put("errorInfo", "Invalid type");
                    responseMap.put("error", errorMap);
                    responseBody = new JSONObject(responseMap).toString();
                }
                generateResponse(responseBody, location, false);
                Logger.i(DeviceConstants.LOG_TAG, "Request : /device. MOSIPDISC completed");
                break;
            }
            case "com.nprime.reg.face.info": {
                if (!isInitSuccessful()) {
                    Intent intent = new Intent(this, DeviceMain.class);
                    startActivityForResult(intent, REQUEST_INIT_BEFORE_INFO);
                    return;
                }
                String szTs = new CommonDeviceAPI().getISOTimeStamp();

                boolean validityStatus = new MDServiceUtility().checkSnapshotValidityAndInit();
                if (!validityStatus) {
                    currentStatus = DeviceConstants.ServiceStatus.NOTREADY;
                }

                String requestType = location + ".info";
                String strDeviceInfo = DeviceMain.deviceMain.getDeviceDriverInfo(currentStatus, szTs, requestType);
                //String responseBody = strDeviceInfo;

                generateResponse(strDeviceInfo, location, false);
                Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                break;
            }
            case "com.nprime.reg.face.rcapture": {
                if (!isInitSuccessful()) {
                    Intent intent = new Intent(this, DeviceMain.class);
                    startActivityForResult(intent, REQUEST_INIT_BEFORE_CAPTURE);
                    return;
                }
                String input = getIntent().getStringExtra("input");
                if (null != input) {
                    capture(REQUEST_CAPTURE, input);
                }
                break;
            }
            default:
                //setResult(Activity.RESULT_CANCELED);
                //finish();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(PERMISSION_CAMERA == requestCode){
            if(PackageManager.PERMISSION_GRANTED == grantResults[0]){
                permissionGranted = true;
                handleIntents();
            }else{
                permissionGranted = false;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Denied")
                        .setMessage("Permission not granted. Unable to access camera")
                        .setCancelable(false)
                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).show();
            }
        }
    }

    private void capture(int captureRequestType, String input) {
        try {
            if(null == ob){
                ob = new ObjectMapper();
            }
            captureRequestDto = (CaptureRequestDto)(ob.readValue(input.getBytes(), CaptureRequestDto.class));
            currentStatus = DeviceConstants.ServiceStatus.BUSY;

            String szTs = new CommonDeviceAPI().getISOTimeStamp();

            //location = "CAPTURE".toLowerCase();
            String requestType = location + ".rcapture"; //"CAPTURE".toLowerCase();
            currentStatus = DeviceConstants.ServiceStatus.READY;

            invokeCaptureCompressEncryptSign(captureRequestType, requestType, captureRequestDto);
        }catch (Exception e){
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Failed to initiate capture");
        }
    }

    public void invokeCaptureCompressEncryptSign(int captureRequestType, String requestType, CaptureRequestDto captureReqDto) {

        String biometricRecordData = "";
        int qualityThreshold = 0;
        int captureTimeout = -1;
        FaceCaptureResult fcResult = null;

		/*CaptureRequestDeviceDetailDto captureReqDetailDto =  captureReqDto.mosipBioRequest.get(0);
		String exceptionData = null;
		if (captureReqDetailDto.exception.length > 0){
			exceptionData = captureReqDetailDto.exception[0];
		}

		if (null == exceptionData || exceptionData.isEmpty()){
			qualityThreshold = captureReqDetailDto.requestedScore;
		}*/

        captureTimeout = captureReqDto.timeout;

        Logger.d(DeviceConstants.LOG_TAG, "Inside Method, invokeCaptureCompressEncryptSign. Timeout : " + captureTimeout + " Requested Quality Score : " + qualityThreshold);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("CaptureTimeout", captureTimeout);
        intent.putExtra("QualityThreshold", qualityThreshold);
        startActivityForResult(intent, captureRequestType);
        //fcResult = invokeFaceCapture(captureTimeout, qualityThreshold);

        //biometricRecordData = ResponseGenHelper.getFaceCaptureBiometricsMOSIP(fcResult, captureReqDto);
        //}
        //return biometricRecordData;
    }

    private void generateResponse(String responseXml, String location, boolean isError)
    {
        //MOSIPDISC, MOSIPDINFO, CAPTURE, RCAPTURE, STREAM
        Intent intent = new Intent();
        intent.putExtra("Response", responseXml);
        intent.putExtra("Location", location);
        if(isError){
            setResult(Activity.RESULT_CANCELED, intent);
        }else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(REQUEST_CAPTURE == requestCode){
            skipOnStart = true;
            if(Activity.RESULT_OK == resultCode){
                showLoadingGif();
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            //Uri photoUri = data.getData();
                            //Bitmap bmpPhoto = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                            FaceCaptureResult captureResult = new FaceCaptureResult();
                            //captureResult.setCapturedImage(bmpPhoto);
                            captureResult.setIsoFaceRecord(data.getByteArrayExtra("IsoFaceRecord"));
                            captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                            captureResult.setQualityScore(data.getIntExtra("Quality", 0));

                            String responseXml = ResponseGenHelper.getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);
                            generateResponse(responseXml, location, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(null != loadingAlertDialog && loadingAlertDialog.isShowing()){
                                    loadingAlertDialog.dismiss();
                                }
                            }
                        });
                    }
                }.start();
            }else{
                FaceCaptureResult captureResult = new FaceCaptureResult();
                captureResult.setIsoFaceRecord(data.getByteArrayExtra("IsoFaceRecord"));
                captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                captureResult.setQualityScore(data.getIntExtra("Quality", 0));

                String responseXml = ResponseGenHelper.getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);
                generateResponse(responseXml, location, false);
            }
        }else if(REQUEST_INTERNAL_CAPTURE == requestCode){
            skipOnStart = true;
            if(Activity.RESULT_OK == resultCode){
                showLoadingGif();
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            //Uri photoUri = data.getData();
                            //Bitmap bmpPhoto = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

                            FaceCaptureResult captureResult = new FaceCaptureResult();
                            //captureResult.setCapturedImage(bmpPhoto);
                            captureResult.setIsoFaceRecord(data.getByteArrayExtra("IsoFaceRecord"));
                            captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                            captureResult.setQualityScore(data.getIntExtra("Quality", 0));

                            String responseXml = ResponseGenHelper.getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle("Response data")
                                            .setMessage(responseXml)
                                            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            }).setNeutralButton("Copy Response", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clip = ClipData.newPlainText("Data ", responseXml);
                                            clipboard.setPrimaryClip(clip);
                                            Toast.makeText(MainActivity.this, "Response copied to clip board", Toast.LENGTH_SHORT).show();
                                        }
                                    }).show();
                                    if(null != loadingAlertDialog && loadingAlertDialog.isShowing()){
                                        loadingAlertDialog.dismiss();
                                    }
                                }
                            });
                            //generateResponse(responseXml, location, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            }
        }else if(REQUEST_INIT_BEFORE_CAPTURE == requestCode){
            if(Activity.RESULT_OK != resultCode){
                skipOnStart = true;
            }
        }else if(REQUEST_INIT_BEFORE_DISCOVERY == requestCode){
            if(Activity.RESULT_OK != resultCode){
                skipOnStart = true;
            }
        }else if(REQUEST_INIT_BEFORE_INFO == requestCode){
            if(Activity.RESULT_OK != resultCode){
                skipOnStart = true;
            }
        }
    }

    private void showLoadingGif() {
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View view1 = inflater.inflate(R.layout.gif_loading_layout,null);
        ImageView gifImage = view1.findViewById(R.id.gif_img);
        Glide.with(view1).load(R.raw.blue_loading_tr2).into(gifImage);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view1).setCancelable(false);
        loadingAlertDialog = builder.create();
        loadingAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingAlertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        //loadingAlertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingAlertDialog.show();
    }
}