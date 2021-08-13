/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nprime.reg.sbi.googlevision;

import android.graphics.Rect;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Tracks the eye positions and state over time, managing an underlying graphic which renders googly
 * eyes over the source video.<p>
 *
 * To improve eye tracking performance, it also helps to keep track of the previous landmark
 * proportions relative to the detected face and to interpolate landmark positions for future
 * updates if the landmarks are missing.  This helps to compensate for intermediate frames where the
 * face was detected but one or both of the eyes were not detected.  Missing landmarks can happen
 * during quick movements due to camera image blurring.
 */
class GooglyFaceTracker extends Tracker<Face> {

    public static boolean[] gestures = new boolean[4];
    public static int gestureCount = 0;
    private float lastMid_x = 0;
    private float lastMid_y = 0;
    public static float verticalNod_min = 0;
    public static float verticalNod_max = 0;
    public static float horizontalNod_min = 0;
    public static float horizontalNod_max = 0;
    public static float smile_min = 0;
    public static float smile_max = 0;
    public static boolean BLINK_COMPLETED = false;
    public static boolean SMILE_COMPLETED = false;
    public static boolean VERTICAL_NOD = false;
    public static boolean HORIZONTAL_NOD = false;
    private static final int NOD_THRESHOLD = 60;
    private static final int RECT_WIDTH_THRESHOLD = 350;
    private static final float EYE_CLOSED_THRESHOLD = 0.4f;
    private static final float SMILE_THRESHOLD = 0.4f;
    private blinkEvent blinkListner;
    private faceDetectedEvent faceListner;
    private boolean isFace = false;
    private FaceGraphic mFaceGraphic;
    private GraphicOverlay mOverlay;
    private Rect rect = null;
    private int facecnt = 0;
    // Record the previously seen proportions of the landmark locations relative to the bounding box
    // of the face.  These proportions can be used to approximate where the landmarks are within the
    // face bounding box if the eye landmark is missing in randA future update.
//    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    // Similarly, keep track of the previous eye open state so that it can be reused for
    // intermediate frames which lack eye landmarks and corresponding eye state.
    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;
    private int eyeCnt = 0;


    //==============================================================================================
    // Methods
    //==============================================================================================

    GooglyFaceTracker(blinkEvent blinklstr,faceDetectedEvent facelstr,GraphicOverlay overlay) {
        this.blinkListner = blinklstr;
        this.faceListner = facelstr;
        mOverlay = overlay;
        mFaceGraphic = new FaceGraphic(overlay);
    }

    /**
     * Resets the underlying googly eyes graphic and associated physics state.
     */
    @Override
    public void onNewItem(int id, Face face) {
    }

    /**
     * Updates the positions and state of eyes to the underlying graphic, according to the most
     * recent face detection results.  The graphic will render the eyes and simulate the motion of
     * the iris based upon these changes over time.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {

        mOverlay.add(mFaceGraphic);
        mFaceGraphic.updateFace(face);
        rect = mFaceGraphic.getFaceRect();
        if (facecnt<3) {
            facecnt++;
            return;
        }
        isFace = true;
        int frameWidth = detectionResults.getFrameMetadata().getWidth();
        int frameHeight = detectionResults.getFrameMetadata().getHeight();
        float faceWidth = face.getWidth();
        int rectWidth = rect.width();

        /*if(faceWidth < 0.5f * frameWidth){
            return;
        }*/

        faceListner.onfaceDetectedEvent(true, rect, face,
                mOverlay.getCameraInfo(), frameWidth, frameHeight);
        /*if(gestures[0] && !BLINK_COMPLETED){
            float leftOpenScore = face.getIsLeftEyeOpenProbability();
            boolean isLeftOpen;
            if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
                isLeftOpen = mPreviousIsLeftOpen;
            } else {
                isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
            }

            float rightOpenScore = face.getIsRightEyeOpenProbability();
            boolean isRightOpen;

            if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
                isRightOpen = mPreviousIsRightOpen;
            } else {
                isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD);
            }

            if (eyeCnt > 0) {  // || to && condition SSS
//            if ((mPreviousIsLeftOpen != isLeftOpen && !isLeftOpen) || (mPreviousIsRightOpen != isRightOpen && !isRightOpen)) {
                if ((mPreviousIsLeftOpen != isLeftOpen && !isLeftOpen) && (mPreviousIsRightOpen != isRightOpen && !isRightOpen)) {
                    BLINK_COMPLETED = true;
                    gestureCount += 1;
                    //if (blinkListner != null) blinkListner.onblinkEvent();
                }
            }
            else if (eyeCnt == 0 && (isLeftOpen || isRightOpen)) {
                eyeCnt = 1;
            }
            mPreviousIsLeftOpen = isLeftOpen;
            mPreviousIsRightOpen = isRightOpen;
        }
        if(gestures[1] && !SMILE_COMPLETED) {
            float smileScore = face.getIsSmilingProbability();
            //System.out.println("TMF20-face : "+ smileScore);
            smile_min = ((smileScore < smile_min) || smile_min == 0.0)  ? smileScore : smile_min;
            smile_max = smileScore > smile_max ? smileScore : smile_max;
            if(smileScore ==  Face.UNCOMPUTED_PROBABILITY){
               smile_min = 0;
               smile_max = 0;
            }
            //System.out.println("TMF20-face : " + (smile_max - smile_min) + "---smile max : " + smile_max + "  smile min : " + smile_min);
            if(((smile_max - smile_min) > SMILE_THRESHOLD) && (smile_min != 0.0f) && (smile_max != 0.0f)){
                SMILE_COMPLETED = true;
                gestureCount += 1;
                //System.out.println("TMF20-face : gesture count : " + gestureCount);
            }
        }
        if(gestures[2] && !HORIZONTAL_NOD && (rect.width() > RECT_WIDTH_THRESHOLD)){
            float x = rect.exactCenterX();
            if(0 == horizontalNod_min || 0 == horizontalNod_max){
                horizontalNod_min = x;
                horizontalNod_max = x;
            }else{
                if (x < horizontalNod_min) { horizontalNod_min = x; }
                if (x > horizontalNod_max) { horizontalNod_max = x; }
            }
            //System.out.println("TMF20-face : horizontal nod : " + (horizontalNod_max - horizontalNod_min) + "---horizontalNod max : " + horizontalNod_max + "  horizontalNod min : " + horizontalNod_min);
            if((horizontalNod_max - horizontalNod_min) > NOD_THRESHOLD ){
                HORIZONTAL_NOD = true;
                gestureCount += 1;
            }
        }
        if(gestures[3] && !VERTICAL_NOD && (rect.width() > RECT_WIDTH_THRESHOLD)) {
            float y = rect.exactCenterY();
            //System.out.println("TMF20-face : y :"+ rect.exactCenterY());
            if(0 == verticalNod_min || 0 == verticalNod_max){
                verticalNod_min = y;
                verticalNod_max = y;
            }else{
                if(y < verticalNod_min){ verticalNod_min = y; }
                if(y > verticalNod_max){ verticalNod_max = y; }
            }
            //System.out.println("TMF20-face : vertical nod : " + (verticalNod_max - verticalNod_min) + "---verticalNod max : " + verticalNod_max + "  verticalNod min : " + verticalNod_min);
            if((verticalNod_max - verticalNod_min) > NOD_THRESHOLD){
                VERTICAL_NOD = true;
                gestureCount += 1;
            }
        }
        if(gestureCount >= CameraActivity.faceMaxGestures *//*&& (SMILE_COMPLETED || BLINK_COMPLETED)*//*){
            //System.out.println("TMF20-face : gesture count [" + gestureCount + "] ---smile : " + SMILE_COMPLETED + " |  Blink : " + BLINK_COMPLETED + " | Horizontal nod :" + HORIZONTAL_NOD + " | Vertical nod :" + VERTICAL_NOD);
            float leftOpenScore = face.getIsLeftEyeOpenProbability();
            float rightOpenScore = face.getIsRightEyeOpenProbability();
            //float smileScore = face.getIsSmilingProbability();
            //System.out.println("TMF20-face : left eye score" + leftOpenScore + " | right eye score" + rightOpenScore);
            if(leftOpenScore > 0.7 && rightOpenScore > 0.7) {
                //gestureCount = 0;
                smile_max = 0;
                smile_min = 0;
                verticalNod_min = 0;
                verticalNod_max = 0;
                horizontalNod_min = 0;
                horizontalNod_max = 0;
                BLINK_COMPLETED = false;
                SMILE_COMPLETED = false;
                VERTICAL_NOD = false;
                HORIZONTAL_NOD = false;
                if (blinkListner != null) blinkListner.onblinkEvent();
            }
        }*/
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        if (isFace) {
            int frameWidth = detectionResults.getFrameMetadata().getWidth();
            int frameHeight = detectionResults.getFrameMetadata().getHeight();
            mPreviousIsLeftOpen = true;
            mPreviousIsRightOpen = true;
            eyeCnt = 0;
            facecnt=0;
            isFace= false;
            faceListner.onfaceDetectedEvent(false,null,null,
                    null, frameWidth, frameHeight);
        }
        mOverlay.remove(mFaceGraphic);
        rect = null;
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the googly eyes graphic from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
        rect=null;
    }


}