package nprime.reg.sbi.googlevision;


import android.graphics.Rect;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.face.Face;

/**
 * Created by Rajeev debnath on 30-03-2017.
 */

public interface faceDetectedEvent {
    void onfaceDetectedEvent(boolean isface, Rect rect, Face face,
                             Size size, int frameWidth, int frameHeight);
}
