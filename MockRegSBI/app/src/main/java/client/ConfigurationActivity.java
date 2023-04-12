package client;

import static nprime.reg.mocksbi.constants.ClientConstants.FACE_DEVICE_STATUS;
import static nprime.reg.mocksbi.constants.ClientConstants.FACE_RESPONSE_DELAY;
import static nprime.reg.mocksbi.constants.ClientConstants.FACE_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.FINGER_DEVICE_STATUS;
import static nprime.reg.mocksbi.constants.ClientConstants.FINGER_RESPONSE_DELAY;
import static nprime.reg.mocksbi.constants.ClientConstants.FINGER_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.IRIS_DEVICE_STATUS;
import static nprime.reg.mocksbi.constants.ClientConstants.IRIS_RESPONSE_DELAY;
import static nprime.reg.mocksbi.constants.ClientConstants.IRIS_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.KEY_ALIAS;
import static nprime.reg.mocksbi.constants.ClientConstants.KEY_STORE_PASSWORD;
import static nprime.reg.mocksbi.constants.ClientConstants.LAST_UPLOAD_DATE;
import static nprime.reg.mocksbi.utility.DeviceConstants.DEFAULT_TIME_DELAY;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.utility.DateUtil;
import nprime.reg.mocksbi.utility.DeviceConstants;
import nprime.reg.mocksbi.utility.FileUtils;

/**
 * @author Anshul.Vanawat
 */

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = ConfigurationActivity.class.getName();
    private static final String LAST_UPDATE = "Last update : ";

    private EditText keyAliasEditText;
    private EditText keyStorePasswordEditText;
    private Slider faceSlider;
    private Slider fingerSlider;
    private Slider irisSlider;
    private TextView faceScoreTextView;
    private TextView fingerScoreTextView;
    private TextView irisScoreTextView;
    private Spinner faceDeviceStatus;
    private Spinner fingerDeviceStatus;
    private Spinner irisDeviceStatus;
    private EditText faceResponseDelayEditText;
    private EditText fingerResponseDelayEditText;
    private EditText irisResponseDelayEditText;


    private String currentKeyAlias;
    private String currentKeyPassword;
    private int currentFaceScore;
    private int currentFingerScore;
    private int currentIrisScore;
    private String lastUploadDate;
    private String currentFaceDeviceStatus;
    private String currentFingerDeviceStatus;
    private String currentIrisDeviceStatus;
    private int currentFaceResponseDelay;
    private int currentFingerResponseDelay;
    private int currentIrisResponseDelay;


    SharedPreferences sharedPreferences;
    private FileChooserFragment fileChooserFragment;
    DateUtil dateUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        dateUtil = new DateUtil(this);

        keyAliasEditText = findViewById(R.id.key_alias);
        keyStorePasswordEditText = findViewById(R.id.key_store_password);
        faceSlider = findViewById(R.id.slider_face_score);
        fingerSlider = findViewById(R.id.slider_finger_score);
        irisSlider = findViewById(R.id.slider_iris_score);
        faceScoreTextView = findViewById(R.id.tx_face_score);
        fingerScoreTextView = findViewById(R.id.tx_finger_score);
        irisScoreTextView = findViewById(R.id.tx_iris_score);
        faceDeviceStatus = findViewById(R.id.face_device_status);
        fingerDeviceStatus = findViewById(R.id.finger_device_status);
        irisDeviceStatus = findViewById(R.id.iris_device_status);
        faceResponseDelayEditText = findViewById(R.id.face_response_delay_millis);
        fingerResponseDelayEditText = findViewById(R.id.finger_response_delay_millis);
        irisResponseDelayEditText = findViewById(R.id.iris_response_delay_millis);

        ArrayList<String> deviceStatus = new ArrayList<>();
        deviceStatus.add(DeviceConstants.ServiceStatus.READY.getStatus());
        deviceStatus.add(DeviceConstants.ServiceStatus.BUSY.getStatus());
        deviceStatus.add(DeviceConstants.ServiceStatus.NOTREADY.getStatus());
        deviceStatus.add(DeviceConstants.ServiceStatus.NOTREGISTERED.getStatus());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceStatus);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        faceDeviceStatus.setAdapter(adapter);
        fingerDeviceStatus.setAdapter(adapter);
        irisDeviceStatus.setAdapter(adapter);

        //default values
        currentKeyAlias = sharedPreferences.getString(KEY_ALIAS, "");
        currentKeyPassword = sharedPreferences.getString(KEY_STORE_PASSWORD, "");
        currentFaceScore = sharedPreferences.getInt(FACE_SCORE, 30);
        currentFingerScore = sharedPreferences.getInt(FINGER_SCORE, 30);
        currentIrisScore = sharedPreferences.getInt(IRIS_SCORE, 30);
        lastUploadDate = sharedPreferences.getString(LAST_UPLOAD_DATE, "");
        currentFaceDeviceStatus = sharedPreferences.getString(FACE_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentFingerDeviceStatus = sharedPreferences.getString(FINGER_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());
        currentIrisDeviceStatus = sharedPreferences.getString(IRIS_DEVICE_STATUS, DeviceConstants.ServiceStatus.READY.getStatus());

        currentFaceResponseDelay = sharedPreferences.getInt(FACE_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentFingerResponseDelay = sharedPreferences.getInt(FINGER_RESPONSE_DELAY, DEFAULT_TIME_DELAY);
        currentIrisResponseDelay = sharedPreferences.getInt(IRIS_RESPONSE_DELAY, DEFAULT_TIME_DELAY);

        Bundle bundle = new Bundle();
        bundle.putString(LAST_UPLOAD_DATE, lastUploadDate);

        FragmentManager fragmentManager = this.getSupportFragmentManager();
        fileChooserFragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragmentContainerView);
        if (fileChooserFragment != null)
            fileChooserFragment.setArguments(bundle);

        faceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = (int) value;
            faceScoreTextView.setText(String.valueOf(intVal));
        });

        fingerSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = (int) value;
            fingerScoreTextView.setText(String.valueOf(intVal));
        });

        irisSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = (int) value;
            irisScoreTextView.setText(String.valueOf(intVal));
        });

        resetScreen();
    }

    public void onSave(View view) {
        Uri fileUri = fileChooserFragment.getSelectedUri();

        if (fileUri != null && !saveFile(fileUri)) {
            Toast.makeText(this, "Failed to save file! Please try again.", Toast.LENGTH_LONG).show();
            return;
        } else {
            lastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());
        }

        currentKeyAlias = keyAliasEditText.getText().toString();
        currentKeyPassword = keyStorePasswordEditText.getText().toString();
        currentFaceScore = (int) faceSlider.getValue();
        currentFingerScore = (int) fingerSlider.getValue();
        currentIrisScore = (int) irisSlider.getValue();
        currentFaceDeviceStatus = faceDeviceStatus.getSelectedItem().toString();
        currentFingerDeviceStatus = fingerDeviceStatus.getSelectedItem().toString();
        currentIrisDeviceStatus = irisDeviceStatus.getSelectedItem().toString();
        currentFaceResponseDelay = Integer.parseInt(faceResponseDelayEditText.getText().toString());
        currentFingerResponseDelay = Integer.parseInt(fingerResponseDelayEditText.getText().toString());
        currentIrisResponseDelay = Integer.parseInt(irisResponseDelayEditText.getText().toString());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ALIAS, currentKeyAlias);
        editor.putString(KEY_STORE_PASSWORD, currentKeyPassword);
        editor.putInt(FACE_SCORE, currentFaceScore);
        editor.putInt(FINGER_SCORE, currentFingerScore);
        editor.putInt(IRIS_SCORE, currentIrisScore);
        editor.putString(LAST_UPLOAD_DATE, LAST_UPDATE + lastUploadDate);
        editor.putString(FACE_DEVICE_STATUS, currentFaceDeviceStatus);
        editor.putString(FINGER_DEVICE_STATUS, currentFingerDeviceStatus);
        editor.putString(IRIS_DEVICE_STATUS, currentIrisDeviceStatus);
        editor.putInt(FACE_RESPONSE_DELAY, currentFaceResponseDelay);
        editor.putInt(FINGER_RESPONSE_DELAY, currentFingerResponseDelay);
        editor.putInt(IRIS_RESPONSE_DELAY, currentIrisResponseDelay);

        editor.apply();

        //navigate back to previous activity
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onReset(View view) {
        resetScreen();
    }

    private void resetScreen() {
        keyAliasEditText.setText(currentKeyAlias);
        keyStorePasswordEditText.setText(currentKeyPassword);
        faceSlider.setValue(currentFaceScore);
        fingerSlider.setValue(currentFingerScore);
        irisSlider.setValue(currentIrisScore);
        fileChooserFragment.resetSelection(lastUploadDate);
        setSpinner(faceDeviceStatus, currentFaceDeviceStatus);
        setSpinner(fingerDeviceStatus, currentFingerDeviceStatus);
        setSpinner(irisDeviceStatus, currentIrisDeviceStatus);
        faceResponseDelayEditText.setText(String.format("%d", currentFaceResponseDelay));
        fingerResponseDelayEditText.setText(String.format("%d", currentFingerResponseDelay));
        irisResponseDelayEditText.setText(String.format("%d", currentIrisResponseDelay));
    }

    private boolean saveFile(Uri fileUri) {
        try {
            FileUtils.SaveFileInAppStorage(getApplicationContext(), fileUri, ClientConstants.P12_FILE_NAME);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e);
            return false;
        }
    }

    private void setSpinner(Spinner spinner, String value) {
        int position = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(value);
        spinner.setSelection(position);
    }
}