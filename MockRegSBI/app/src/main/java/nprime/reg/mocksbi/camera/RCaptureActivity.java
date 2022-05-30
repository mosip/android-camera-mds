package nprime.reg.mocksbi.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Base64;

import nprime.reg.mocksbi.face.R;
import nprime.reg.mocksbi.faceCaptureApi.FaceCaptureResult;

/**
 * @author NPrime Technologies
 */

public class RCaptureActivity extends AppCompatActivity {

    private static final long PREVIEW_TIME_DELAY = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcapture);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    ImageView img = findViewById(R.id.img);
                    img.setImageResource(R.drawable.img1);
                    String encodedIso = getIsoDataFromAssets("encodedIsoImg1.txt");
                    byte[] isoFaceRecord = Base64.getUrlDecoder().decode(encodedIso.trim());
                    captureSuccessful(isoFaceRecord, 30);
                }catch (Exception e){
                    e.printStackTrace();
                    captureFailed(-301, e.getMessage());
                }
            }
        }, PREVIEW_TIME_DELAY);
    }

    public void captureSuccessful(byte[] isoFaceRecord, int quality) {
        Uri isoUri = Uri.fromFile(getTempFile(RCaptureActivity.this));
        saveByteArray(isoFaceRecord, isoUri);
        Intent intent = new Intent();
        intent.setData(isoUri);
        intent.putExtra("Status", FaceCaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("Quality", quality);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void captureFailed(int status, String msg) {
        Intent intent = new Intent();
        intent.putExtra("Status", status);
        intent.putExtra("msg", msg);
        //intent.putExtra("IsoFaceRecord", "".getBytes());
        intent.putExtra("Quality", 0);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    public File getTempFile(Context context) {
        try {
            return File.createTempFile("isoRecord_", ".dat", context.getCacheDir());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create photo file.");
        }
    }

    public static void saveByteArray(final byte[] data, final Uri uri) {
        final File file = new File(uri.getPath());

        try {
            final OutputStream os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            os.close();
        } catch (final Exception e) {
            throw new RuntimeException("Unable to store data.", e);
        }
    }

    private String getIsoDataFromAssets(String assetFileName){
        String encodedIsoData = null;
        try {
            InputStream respData = getAssets().open(assetFileName);
            BufferedReader r = new BufferedReader(new InputStreamReader(respData));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            encodedIsoData = total.toString();
        }catch (IOException e){
            e.printStackTrace();
        }
        return encodedIsoData;
    }
}