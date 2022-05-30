package nprime.reg.mocksbi.mds;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.DataObjects.CaptureRequestDto;
import com.mdm.DataObjects.DiscoverRequestDto;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import nprime.reg.mocksbi.camera.RCaptureActivity;
import nprime.reg.mocksbi.face.R;
import nprime.reg.mocksbi.faceCaptureApi.FaceCaptureResult;
import nprime.reg.mocksbi.scanner.ResponseGenerator.ResponseGenHelper;
import nprime.reg.mocksbi.utility.CommonDeviceAPI;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.Logger;

/**
 * @author NPrime Technologies
 */

public class MDServiceActivity extends AppCompatActivity {
    private static final int RequestCodeCapture = 1;

    private static final int PERMISSION_CAMERA = 2;

    private ObjectMapper ob;

    public static long lastInitTimestamp = 0;
    public static Context applicationContext;

    CaptureRequestDto captureRequestDto = null;
    private DeviceConstants.ServiceStatus currentStatus = DeviceConstants.ServiceStatus.NOTREADY;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdservice);

        if (ActivityCompat.checkSelfPermission(MDServiceActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MDServiceActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
            return;
        }
        applicationContext = this.getApplicationContext();
        handleIntents();
    }

    private void handleIntents() {
        if (currentStatus != DeviceConstants.ServiceStatus.BUSY){
            currentStatus = DeviceConstants.ServiceStatus.READY;
        }
        String actionType = getIntent().getAction();
        switch (actionType) {
            case "sbi.reg.device": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();
                    String responseBody;
                    DiscoverRequestDto discoverRequestDto = null;
                    byte[] input = getIntent().getByteArrayExtra("input");

                    if (null != input) {
                        if (null == ob) {
                            ob = new ObjectMapper();
                        }
                        try {
                            discoverRequestDto = ob.readValue(input, DiscoverRequestDto.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (null != discoverRequestDto && (discoverRequestDto.type.equalsIgnoreCase("face") ||
                            discoverRequestDto.type.equalsIgnoreCase("biometric device"))) {
                        String requestType = getPackageName();//"nprime.reg.mocksbi.face";
                        responseBody = discoverDevice(currentStatus, szTs, requestType);
                    } else {
                        Map<String, Object> responseMap = new HashMap<>();
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "301");
                        errorMap.put("errorInfo", "Invalid type");
                        responseMap.put("error", errorMap);
                        responseBody = new JSONObject(responseMap).toString();
                    }
                    generateResponse(responseBody, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /device. MOSIPDISC completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.face" + ".info";
                    String strDeviceInfo = getDeviceDriverInfo(currentStatus, szTs, requestType);

                    generateResponse(strDeviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.rcapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input);
                    }else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        String rCaptureResponse = new JSONObject(responseMap).toString();
                        generateResponse(rCaptureResponse, true);
                    }
                }).start();
                break;
            }
            default:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
        }
    }


    //TODO clean shared files
    private void cleanUriFileData() {
        try {
            File outputPath = new File(this.getFilesDir(), "output/");
            if (outputPath.exists()) {
                String folder = getCallingPackage().replaceAll("\\.", "-");
                File clientFolderPath = new File(outputPath.getPath(), folder + "/");
                if (clientFolderPath.exists()) {
                    File[] files = clientFolderPath.listFiles();
                    if (null != files && files.length > 0) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void capture(byte[] input) {
        try {
            if (ActivityCompat.checkSelfPermission(MDServiceActivity.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MDServiceActivity.this,
                        new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                return;
            }
            if(null == ob){
                ob = new ObjectMapper();
            }
            captureRequestDto = ob.readValue(input, CaptureRequestDto.class);
            currentStatus = DeviceConstants.ServiceStatus.BUSY;

            //currentStatus = DeviceConstants.ServiceStatus.READY;
            startCameraActivity(captureRequestDto.timeout);
            //invokeCaptureCompressEncryptSign(captureRequestDto);
        }catch (Exception e){
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Failed to initiate capture");
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("errorCode", "101");
            errorMap.put("errorInfo", "Invalid input data");
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("error", errorMap);
            String rCaptureResponse = new JSONObject(responseMap).toString();
            generateResponse(rCaptureResponse, true);
        }
    }

    private void startCameraActivity(int captureTimeout) {
        Intent intent = new Intent(this, RCaptureActivity.class);
        intent.putExtra("CaptureTimeout", captureTimeout);
        startActivityForResult(intent, RequestCodeCapture);
    }

    private void generateResponse(String responseXml, boolean isError)
    {
        Intent intent = new Intent();
        intent.putExtra("Response", responseXml);
        if(isError || (null == responseXml || responseXml.isEmpty())){
            setResult(Activity.RESULT_CANCELED, intent);
        }else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void generateRCaptureResponse(String responseXml, boolean isError)
    {
        Intent intent = new Intent();
        if(null != responseXml && !responseXml.isEmpty()) {
            try {
                File outputPath = new File(this.getFilesDir(), "output/");
                if (!outputPath.exists()) {
                    outputPath.mkdir();
                }
                String folder = getCallingPackage().replaceAll("\\.","-");
                File clientFolderPath = new File(outputPath.getPath(), folder + "/");
                if(!clientFolderPath.exists()){
                    clientFolderPath.mkdir();
                }
                File file = new File(clientFolderPath, "resp_" + System.currentTimeMillis() + ".txt");
                final OutputStream os = new FileOutputStream(file);
                os.write(responseXml.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                Uri respUri = FileProvider.getUriForFile(MDServiceActivity.this, "nprime.reg.mocksbi.face.fileprovider", file);
                getApplicationContext().grantUriPermission(getCallingPackage(), respUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(respUri);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(isError || (null == responseXml || responseXml.isEmpty())){
            setResult(Activity.RESULT_CANCELED, intent);
        }else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private String discoverDevice(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType){
        return ResponseGenHelper.getDeviceDiscovery(currentStatus, szTimeStamp, requestType);
    }

    public String getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType) {
        return ResponseGenHelper.getDeviceDriverInfo(currentStatus, szTimeStamp, requestType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(RequestCodeCapture == requestCode){
            if(Activity.RESULT_OK == resultCode){
                new Thread(() -> {
                    try {
                        Uri isoUri = data.getData();
                        InputStream is = getContentResolver().openInputStream(isoUri);
                        byte[] isoRecord = readBytes(is);

                        FaceCaptureResult captureResult = new FaceCaptureResult();
                        captureResult.setIsoFaceRecord(isoRecord);
                        captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                        captureResult.setQualityScore(data.getIntExtra("Quality", 0));

                        String responseXml = ResponseGenHelper
                                .getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);
                        generateRCaptureResponse(responseXml, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }else{
                FaceCaptureResult captureResult = new FaceCaptureResult();
                captureResult.setIsoFaceRecord("".getBytes(StandardCharsets.UTF_8));
                captureResult.setStatus(FaceCaptureResult.CAPTURE_CANCELLED);
                captureResult.setQualityScore(0);
                if(null != data) {
                    captureResult.setIsoFaceRecord("".getBytes(StandardCharsets.UTF_8));
                    captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                    captureResult.setQualityScore(data.getIntExtra("Quality", 0));
                }

                String responseXml = ResponseGenHelper
                        .getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);
                generateRCaptureResponse(responseXml, false);
            }
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}