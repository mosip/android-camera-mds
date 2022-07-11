package nprime.reg.mocksbi.mds;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.DataObjects.CaptureRequestDto;
import com.mdm.DataObjects.DeviceInformation;
import com.mdm.DataObjects.DiscoverRequestDto;

import nprime.reg.mocksbi.dto.CaptureResponse;
import nprime.reg.mocksbi.dto.DeviceInfoResponse;
import nprime.reg.mocksbi.dto.Error;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import nprime.reg.mocksbi.camera.RCaptureActivity;
import nprime.reg.mocksbi.R;
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
    private static final int URI_EXPIRY_TIME_IN_SEC = 10;	

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
            case "io.sbi.device": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();
                    Object responseBody = null;
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

                    if (null != discoverRequestDto) {
                        switch (discoverRequestDto.type.toLowerCase()) {
                            case "face" :
                                responseBody = discoverDevice(currentStatus, szTs, "nprime.reg.mocksbi.face", DeviceConstants.BioType.Face);
                                break;
                            case "finger":
                                responseBody = discoverDevice(currentStatus, szTs, "nprime.reg.mocksbi.finger",
                                        DeviceConstants.BioType.Finger);
                                break;
                            case "iris":
                                responseBody = discoverDevice(currentStatus, szTs, "nprime.reg.mocksbi.iris",
                                        DeviceConstants.BioType.Iris);
                                break;
                            case "biometric device":
                                break;
                        }

                    } else {
                        responseBody = Arrays.asList(new DeviceInfoResponse(null, new Error("301", "Invalid type")));
                    }
                    generateResponse(responseBody, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /device. MOSIPDISC completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.face" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentStatus, szTs, requestType, DeviceConstants.BioType.Face);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.finger.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.finger" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentStatus, szTs, requestType, DeviceConstants.BioType.Finger);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.iris.Info": {
                new Thread(() -> {
                    String szTs = new CommonDeviceAPI().getISOTimeStamp();

                    String requestType = "nprime.reg.mocksbi.iris" + ".info";
                    List<DeviceInfoResponse> deviceInfo = getDeviceDriverInfo(currentStatus, szTs, requestType, DeviceConstants.BioType.Iris);

                    generateResponse(deviceInfo, false);
                    Logger.i(DeviceConstants.LOG_TAG, "Request : /info. MOSIPDINFO completed");
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.face.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Face);
                    }else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
                    }
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.finger.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Finger);
                    }else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
                    }
                }).start();
                break;
            }
            case "nprime.reg.mocksbi.iris.rCapture": {
                new Thread(() -> {
                    cleanUriFileData();
                    byte[] input = getIntent().getByteArrayExtra("input");
                    if (null != input) {
                        capture(input, DeviceConstants.BioType.Iris);
                    }else {
                        Map<String, Object> errorMap = new LinkedHashMap<>();
                        errorMap.put("errorCode", "101");
                        errorMap.put("errorInfo", "Invalid input");
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("error", errorMap);
                        generateResponse(new JSONObject(responseMap), true);
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

    private void capture(byte[] input, DeviceConstants.BioType bioType) {
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
            startCameraActivity(captureRequestDto.timeout, bioType,
                    captureRequestDto.mosipBioRequest.get(0).deviceSubId);
            //invokeCaptureCompressEncryptSign(captureRequestDto);
        }catch (Exception e){
            e.printStackTrace();
            Logger.e(DeviceConstants.LOG_TAG, "Failed to initiate capture");
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("errorCode", "101");
            errorMap.put("errorInfo", "Invalid input data");
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("error", errorMap);
            generateResponse(responseMap, true);
        }
    }

    private void startCameraActivity(int captureTimeout, DeviceConstants.BioType bioType, String bioSubId) {
        Intent intent = new Intent(this, RCaptureActivity.class);
        intent.putExtra("CaptureTimeout", captureTimeout);
        intent.putExtra("modality", bioType.getType());
        intent.putExtra("bioSubId", Integer.parseInt(bioSubId));
        startActivityForResult(intent, RequestCodeCapture);
    }

    private void generateResponse(Object responseXml, boolean isError) {
        Intent intent = new Intent();
        try {
            intent.putExtra("response", new ObjectMapper().writeValueAsBytes(responseXml));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if(isError || (null == responseXml)){
            setResult(Activity.RESULT_CANCELED, intent);
        }else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void generateRCaptureResponse(CaptureResponse responseXml, boolean isError)
    {
        Intent intent = new Intent();
        if(null != responseXml) {
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
                os.write(ob.writeValueAsBytes(responseXml));
                os.flush();
                os.close();
                Uri respUri = FileProvider.getUriForFile(MDServiceActivity.this, "nprime.reg.mocksbi.fileprovider", file);
                getApplicationContext().grantUriPermission(getCallingPackage(), respUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("response", respUri);
				
                //Initiating One Time work request to revoke permission and delete file
                initWorkRequest(respUri, file.getAbsolutePath());				
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(isError || (null == responseXml)){
            setResult(Activity.RESULT_CANCELED, intent);
        }else {
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    private void initWorkRequest(Uri uri, String fileName){
        Data inputData = new Data.Builder()
                .putString("FileName", fileName)
                .putString("Uri", uri.toString())
                .build();
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest
                .Builder(FileUriWorker.class)
                .setInputData(inputData)
                .setInitialDelay(URI_EXPIRY_TIME_IN_SEC, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
    }
	
    private List<DeviceInformation> discoverDevice(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType, DeviceConstants.BioType bioType){
        return ResponseGenHelper.getDeviceDiscovery(currentStatus, szTimeStamp, requestType, bioType);
    }

    public List<DeviceInfoResponse> getDeviceDriverInfo(DeviceConstants.ServiceStatus currentStatus, String szTimeStamp, String requestType,
                                                        DeviceConstants.BioType bioType) {
        return ResponseGenHelper.getDeviceDriverInfo(currentStatus, szTimeStamp, requestType, bioType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(RequestCodeCapture == requestCode){
            if(Activity.RESULT_OK == resultCode){
                new Thread(() -> {
                    try {
                        FaceCaptureResult captureResult = new FaceCaptureResult();
                        captureResult.setModality(data.getStringExtra("modality"));
                        captureResult.setBioSubId(data.getIntExtra("bioSubId", 1));
                        switch (captureResult.getModality().toLowerCase()) {
                            case "face":
                                try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("face"))) {
                                    captureResult.getBiometricRecords().put("", readBytes(is));
                                }
                                break;
                            case "finger":
                                switch (captureResult.getBioSubId()) {
                                    case 1:
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left IndexFinger"))) {
                                            captureResult.getBiometricRecords().put("Left IndexFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left MiddleFinger"))) {
                                            captureResult.getBiometricRecords().put("Left MiddleFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left RingFinger"))) {
                                            captureResult.getBiometricRecords().put("Left RingFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left LittleFinger"))) {
                                            captureResult.getBiometricRecords().put("Left LittleFinger", readBytes(is));
                                        }
                                        break;
                                    case 2:
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right IndexFinger"))) {
                                            captureResult.getBiometricRecords().put("Right IndexFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right MiddleFinger"))) {
                                            captureResult.getBiometricRecords().put("Right MiddleFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right RingFinger"))) {
                                            captureResult.getBiometricRecords().put("Right RingFinger", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right LittleFinger"))) {
                                            captureResult.getBiometricRecords().put("Right LittleFinger", readBytes(is));
                                        }
                                        break;
                                    case 3:
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left Thumb"))) {
                                            captureResult.getBiometricRecords().put("Left Thumb", readBytes(is));
                                        }
                                        try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right Thumb"))) {
                                            captureResult.getBiometricRecords().put("Right Thumb", readBytes(is));
                                        }
                                        break;
                                }
                                break;
                            case "iris":
                                try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Left"))) {
                                    captureResult.getBiometricRecords().put("Left", readBytes(is));
                                }
                                try(InputStream is = getContentResolver().openInputStream(data.getParcelableExtra("Right"))) {
                                    captureResult.getBiometricRecords().put("Right", readBytes(is));
                                }
                                break;
                        }

                        captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                        captureResult.setQualityScore(data.getIntExtra("Quality", 0));

                        CaptureResponse responseXml = ResponseGenHelper
                                .getRCaptureBiometricsMOSIP(captureResult, captureRequestDto);
                        generateRCaptureResponse(responseXml, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }else{
                FaceCaptureResult captureResult = new FaceCaptureResult();
                captureResult.setStatus(FaceCaptureResult.CAPTURE_CANCELLED);
                captureResult.setQualityScore(0);
                if(null != data) {
                    captureResult.setStatus(data.getIntExtra("Status", FaceCaptureResult.CAPTURE_CANCELLED));
                    captureResult.setQualityScore(data.getIntExtra("Quality", 0));
                }

                CaptureResponse responseXml = ResponseGenHelper
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