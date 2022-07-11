package client;

import static nprime.reg.mocksbi.constants.ClientConstants.FACE_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.FINGER_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.IRIS_SCORE;
import static nprime.reg.mocksbi.constants.ClientConstants.KEY_ALIAS;
import static nprime.reg.mocksbi.constants.ClientConstants.KEY_STORE_PASSWORD;
import static nprime.reg.mocksbi.constants.ClientConstants.LAST_UPLOAD_DATE;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.utility.DateUtil;
import nprime.reg.mocksbi.utility.FileUtils;

/**
 * @author Anshul.Vanawat
 */

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = ConfigurationActivity.class.getName();
    private static final String LAST_UPLOADED_STRING = "Last Upload : ";

    private EditText keyAliasEditText;
    private EditText keyStorePasswordEditText;
    private Slider faceSlider;
    private Slider fingerSlider;
    private Slider irisSlider;
    private TextView faceScoreTextView;
    private TextView fingerScoreTextView;
    private TextView irisScoreTextView;

    private String currentKeyAlias;
    private String currentKeyPassword;
    private int currentFaceScore;
    private int currentFingerScore;
    private int currentIrisScore;
    private String lastUploadDate;

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

        currentKeyAlias = sharedPreferences.getString(KEY_ALIAS, "");
        currentKeyPassword = sharedPreferences.getString(KEY_STORE_PASSWORD, "");
        currentFaceScore = sharedPreferences.getInt(FACE_SCORE, 30);
        currentFingerScore = sharedPreferences.getInt(FINGER_SCORE, 30);
        currentIrisScore = sharedPreferences.getInt(IRIS_SCORE, 30);
        lastUploadDate = sharedPreferences.getString(LAST_UPLOAD_DATE, "");

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
        }

        currentKeyAlias = keyAliasEditText.getText().toString();
        currentKeyPassword = keyStorePasswordEditText.getText().toString();
        currentFaceScore = (int) faceSlider.getValue();
        currentFingerScore = (int) fingerSlider.getValue();
        currentIrisScore = (int) irisSlider.getValue();
        lastUploadDate = dateUtil.getDateTime(System.currentTimeMillis());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ALIAS, currentKeyAlias);
        editor.putString(KEY_STORE_PASSWORD, currentKeyPassword);
        editor.putInt(FACE_SCORE, currentFaceScore);
        editor.putInt(FINGER_SCORE, currentFingerScore);
        editor.putInt(IRIS_SCORE, currentIrisScore);
        editor.putString(LAST_UPLOAD_DATE, LAST_UPLOADED_STRING + lastUploadDate);
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
}