package client;

import static client.FileChooserFragment.ARG_LAST_UPLOAD_DATE;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import io.mosip.mock.sbi.R;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class KeyCredentialFragment extends Fragment {

    public static final String KEY_TYPE_DEVICE = "Device Key";
    public static final String KEY_TYPE_FTM = "FTM Key";
    private TextView keyTextView;
    private EditText keyAliasEditText;
    private EditText passwordEditText;
    private FileChooserFragment fileChooserFragment;

    // the fragment initialization parameters
    public static final String ARG_KEY_LABEL = "keyLabel";
    public static final String ARG_KEY_ALIAS = "keyAlias";
    public static final String ARG_PASSWORD = "password";

    public KeyCredentialFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_key_credential, container, false);

        keyTextView = rootView.findViewById(R.id.key_label_textview);
        keyAliasEditText = rootView.findViewById(R.id.key_alias_edittext);
        passwordEditText = rootView.findViewById(R.id.key_store_password_edittext);
        FragmentManager fragmentManager = this.getChildFragmentManager();
        fileChooserFragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fileChooserFragment);

        if (getArguments() == null) {
            return rootView;
        }

        keyTextView.setText(getArguments().getString(ARG_KEY_LABEL));
        keyAliasEditText.setText(getArguments().getString(ARG_KEY_ALIAS));
        passwordEditText.setText(getArguments().getString(ARG_PASSWORD));
        if (fileChooserFragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString(ARG_LAST_UPLOAD_DATE, getArguments().getString(ARG_LAST_UPLOAD_DATE));
            fileChooserFragment.setArguments(bundle);
        }

        return rootView;
    }

    public String getKeyAlias() {
        return keyAliasEditText.getText().toString();
    }

    public String getPassword() {
        return passwordEditText.getText().toString();
    }

    public Uri getSelectedUri() {
        return fileChooserFragment.getSelectedUri();
    }

    public void setValues(String keyAlias, String password, String lastUpdated) {
        if (keyAliasEditText != null)
            keyAliasEditText.setText(keyAlias);
        if (passwordEditText != null)
            passwordEditText.setText(password);
        if (fileChooserFragment != null)
            fileChooserFragment.resetSelection(lastUpdated);
    }
}