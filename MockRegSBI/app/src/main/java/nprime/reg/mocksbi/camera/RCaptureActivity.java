package nprime.reg.mocksbi.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import java.io.*;
import java.util.*;

import com.google.android.gms.common.util.IOUtils;
import nprime.reg.mocksbi.R;
import nprime.reg.mocksbi.faceCaptureApi.FaceCaptureResult;

/**
 * @author NPrime Technologies
 */

public class RCaptureActivity extends AppCompatActivity {

    private static final long PREVIEW_TIME_DELAY = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcapture);
        String modality = getIntent().getStringExtra("modality");
        int bioSubId = getIntent().getIntExtra("bioSubId", 1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Uri> uris = new HashMap<>();
                    switch (modality.toLowerCase()) {
                        case "face":
                            ((ImageView)findViewById(R.id.img)).setImageResource(R.drawable.face);
                            uris.put("face", getBioAttributeURI("Face.iso"));
                            break;
                        case "finger":
                            switch (bioSubId) {
                                case 1:
                                    ((ImageView)findViewById(R.id.img)).setImageResource(R.drawable.left);
                                    uris.put("Left IndexFinger", getBioAttributeURI("Left_Index.iso"));
                                    uris.put("Left MiddleFinger", getBioAttributeURI("Left_Middle.iso"));
                                    uris.put("Left RingFinger", getBioAttributeURI("Left_Ring.iso"));
                                    uris.put("Left LittleFinger", getBioAttributeURI("Left_Little.iso"));
                                    break;
                                case 2:
                                    ((ImageView)findViewById(R.id.img)).setImageResource(R.drawable.right);
                                    uris.put("Right IndexFinger", getBioAttributeURI("Right_Index.iso"));
                                    uris.put("Right MiddleFinger", getBioAttributeURI("Right_Middle.iso"));
                                    uris.put("Right RingFinger", getBioAttributeURI("Right_Ring.iso"));
                                    uris.put("Right LittleFinger", getBioAttributeURI("Right_Little.iso"));
                                    break;
                                case 3:
                                    ((ImageView)findViewById(R.id.img)).setImageResource(R.drawable.thumbs);
                                    uris.put("Left Thumb", getBioAttributeURI("Left_Thumb.iso"));
                                    uris.put("Right Thumb", getBioAttributeURI("Right_Thumb.iso"));
                                    break;
                            }
                            break;
                        case "iris":
                            ((ImageView)findViewById(R.id.img)).setImageResource(R.drawable.iris);
                            uris.put("Left", getBioAttributeURI("Left_Iris.iso"));
                            uris.put("Right", getBioAttributeURI("Right_Iris.iso"));
                            break;
                    }

                    captureSuccessful(uris, modality, 30, bioSubId);
                }catch (Exception e){
                    e.printStackTrace();
                    captureFailed(-301, e.getMessage());
                }
            }
        }, PREVIEW_TIME_DELAY);
    }

    private Uri getBioAttributeURI(String file) {
        byte[] isoRecord = getIsoDataFromAssets(file);
        Uri isoUri = Uri.fromFile(getTempFile(RCaptureActivity.this));
        saveByteArray(isoRecord, isoUri);
        return isoUri;
    }

    public void captureSuccessful(Map<String, Uri> uris, String modality, int quality, int bioSubId) {
        Intent intent = new Intent();
        for(String attribute : uris.keySet()) {
            intent.putExtra(attribute, uris.get(attribute));
        }
        intent.putExtra("modality", modality);
        intent.putExtra("Status", FaceCaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("Quality", quality);
        intent.putExtra("bioSubId", bioSubId);
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

    private byte[] getIsoDataFromAssets(String assetFileName){
       try(InputStream in = getAssets().open(assetFileName)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }
}