package nprime.reg.sbi.faceCaptureApi;

import android.net.Uri;

public interface CaptureEvent {
    void captureSuccessful(Uri photoUri, byte[] isoFaceRecord, int quality);
    void captureFailed(int status, String msg);
}
