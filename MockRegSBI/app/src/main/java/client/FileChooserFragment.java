package client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.File;

import nprime.reg.mocksbi.constants.ClientConstants;
import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.utility.FileUtils;

/**
 * @author Anshul.Vanawat
 */

public class FileChooserFragment extends Fragment {

    private static final String TAG = FileChooserFragment.class.getName();

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;
    private static final int MY_RESULT_CODE_FILE_CHOOSER = 2000;

    private TextView editTextPath;
    Uri selectedFileUri;

    private static final String LOG_TAG = "AndroidExample";

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_chooser, container, false);

        editTextPath = rootView.findViewById(R.id.last_upload_date);
        String lastUploadedDate = getArguments().getString(ClientConstants.LAST_UPLOAD_DATE);
        editTextPath.setText(lastUploadedDate);

        Button buttonBrowse = rootView.findViewById(R.id.button_browse);
        buttonBrowse.setOnClickListener(view -> askPermissionAndBrowseFile());
        return rootView;
    }

    private void askPermissionAndBrowseFile() {
        // With Android Level >= 23, you have to ask the user
        // for permission to access External Storage.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Level 23

            // Check if we have Call permission
            int permission = ActivityCompat.checkSelfPermission(this.getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_REQUEST_CODE_PERMISSION
                );
                return;
            }
        }
        this.doBrowseFile();
    }

    private void doBrowseFile() {
        Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFileIntent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");
        startActivityForResult(chooseFileIntent, MY_RESULT_CODE_FILE_CHOOSER);
    }

    // When you have the request results
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_REQUEST_CODE_PERMISSION: {
                // Note: If request is cancelled, the result arrays are empty.
                // Permissions granted (CALL_PHONE).
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.doBrowseFile();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_RESULT_CODE_FILE_CHOOSER:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        selectedFileUri = data.getData();
                        String filePath = FileUtils.getPath(this.getContext(), selectedFileUri);
                        File file = new File(filePath);
                        editTextPath.setText(file.getName());
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Uri getSelectedUri() {
        return selectedFileUri;
    }

    public void resetSelection(String lastUploadDate) {
        selectedFileUri = null;
        if (editTextPath != null)
            editTextPath.setText(lastUploadDate);
    }
}