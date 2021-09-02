package nprime.reg.sbi.faceCaptureApi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import nprime.reg.sbi.face.R;

public class FaceCaptureActivity extends AppCompatActivity implements CaptureEvent{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);
        if (null == savedInstanceState) {
            Fragment fragment = new SimpleCaptureFragment("1", this);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    public void captureSuccessful(Uri photoUri, byte[] isoFaceRecord, int quality) {
        Intent intent = new Intent();
        intent.setData(photoUri);
        intent.putExtra("Status", FaceCaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("IsoFaceRecord", isoFaceRecord);
        intent.putExtra("Quality", quality);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void captureFailed(int status, String msg) {
        Intent intent = new Intent();
        intent.putExtra("Status", status);
        intent.putExtra("msg", msg);
        intent.putExtra("IsoFaceRecord", "".getBytes());
        intent.putExtra("Quality", 0);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        captureFailed(FaceCaptureResult.CAPTURE_CANCELLED, "capture cancelled");
    }
}