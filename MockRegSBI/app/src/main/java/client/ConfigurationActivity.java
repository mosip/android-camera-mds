package client;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import nprime.reg.mocksbi.R;

public class ConfigurationActivity extends AppCompatActivity {

    private static final String KEY_ALIAS = "key_alias";
    private static final String KEY_STORE_PASSWORD = "key_store_password";
    private static final String FACE_SCORE = "face_score";
    private static final String IRIS_SCORE = "iris_score";
    private static final String FINGER_SCORE = "finger_score";

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

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        keyAliasEditText = findViewById(R.id.key_alias);
        keyStorePasswordEditText = findViewById(R.id.key_store_password);
        faceSlider = findViewById(R.id.slider_face_score);
        fingerSlider = findViewById(R.id.slider_finger_score);
        irisSlider = findViewById(R.id.slider_iris_score);
        faceScoreTextView = findViewById(R.id.tx_face_score);
        fingerScoreTextView = findViewById(R.id.tx_finger_score);
        irisScoreTextView = findViewById(R.id.tx_iris_score);

        faceSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                int intVal = (int) value;
                faceScoreTextView.setText(String.valueOf(intVal));
            }
        });

        fingerSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                int intVal = (int) value;
                fingerScoreTextView.setText(String.valueOf(intVal));
            }
        });

        irisSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                int intVal = (int) value;
                irisScoreTextView.setText(String.valueOf(intVal));
            }
        });

        resetScreen();
    }

    public void onSave(View view) {
        currentKeyAlias = keyAliasEditText.getText().toString();
        currentKeyPassword = keyStorePasswordEditText.getText().toString();
        currentFaceScore = (int) faceSlider.getValue();
        currentFingerScore = (int) fingerSlider.getValue();
        currentIrisScore = (int) irisSlider.getValue();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ALIAS, currentKeyAlias);
        editor.putString(KEY_STORE_PASSWORD, currentKeyPassword);
        editor.putInt(FACE_SCORE, currentFaceScore);
        editor.putInt(FINGER_SCORE, currentFingerScore);
        editor.putInt(IRIS_SCORE, currentIrisScore);
        editor.apply();

        //navigate back to previous activity
        finish();
    }

    public void onReset(View view) {
        resetScreen();
    }

    private void resetScreen(){
        currentKeyAlias = sharedPreferences.getString(KEY_ALIAS, "");
        currentKeyPassword = sharedPreferences.getString(KEY_STORE_PASSWORD, "");
        currentFaceScore = sharedPreferences.getInt(FACE_SCORE, 30);
        currentFingerScore = sharedPreferences.getInt(FINGER_SCORE, 30);
        currentIrisScore = sharedPreferences.getInt(IRIS_SCORE, 30);

        keyAliasEditText.setText(currentKeyAlias);
        keyStorePasswordEditText.setText(currentKeyPassword);
        faceSlider.setValue(currentFaceScore);
        fingerSlider.setValue(currentFingerScore);
        irisSlider.setValue(currentIrisScore);
    }

    public void onAddP12(View view) {
        Toast.makeText(this, "Implementation Pending", Toast.LENGTH_SHORT).show();
    }
}