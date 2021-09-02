package nprime.reg.sbi.googlevision;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import in.nprime.jp2.JP2Encoder;
import npr.util.BioFace;
import nprime.reg.sbi.face.MainActivity;
import nprime.reg.sbi.face.R;
import nprime.reg.sbi.faceCaptureApi.FaceCaptureResult;

public class CameraActivity extends AppCompatActivity implements blinkEvent, faceDetectedEvent {
    private ImageButton btnFlash = null;
    private ImageButton btnTakePic = null;
    private ImageButton btnFlipCam = null;
    private ImageView gesturesImg = null;
    private TextView statusMsg = null;
    ToneGenerator toneGenerator = null;
    private int focusLeft, focusTop, focusRight, focusBottom;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static long faceSteadyMills = 0;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private com.google.android.gms.common.images.Size viewSize = null;
    private int cameraFaceID = 1;// 0 for rear
    private GraphicOverlay mGraphicOverlay;
    private CameraSource mCameraSource = null;
    private Timer timer = null;
    private TimerTask timerTask;
    private CameraSourcePreview mTextureView;
    private String[] strMsgs = {"Blink Slowly","Please Smile","Turn Head Right","Move Head Up", "Look Straight"};
    int randA, randB;

    private static int faceCaptureTimeout = 60000;//60sec
    public static int faceMaxGestures = 1;
    private boolean gestureAcompleted =false;
    private boolean gestureBcompleted = false;
    private boolean gestureCcompleted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.dimAmount = 0.3f;
        params.x = 0; //20;
        params.y = 0; //40;
        params.setTitle("");
        this.getWindow().setAttributes(params);

        gesturesImg = (ImageView)findViewById(R.id.face_gif);

        cameraFaceID = MainActivity.sharedPreferences.getInt("CameraFacing", 1);

        /*Glide.with(this)
                //.asGif()
                .load(R.raw.gestures)
                .into(gesturesImg);*/

        faceCaptureTimeout = getIntent().getIntExtra("CaptureTimeout", faceCaptureTimeout);

        mTextureView = (CameraSourcePreview)findViewById(R.id.camera_src_preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.dwgOverlay);
        btnTakePic = (ImageButton) findViewById(R.id.btnPicture);
        btnFlash = (ImageButton) findViewById(R.id.btn_flash);
        btnFlipCam = (ImageButton) findViewById(R.id.btnFlip);
        statusMsg = (TextView)findViewById(R.id.tv_cam_status);
        statusMsg.setText("Face not Clear.");
        btnTakePic.setEnabled(false);
        btnTakePic.setImageResource(R.drawable.camera_shutter1);

        createCameraSource();

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                        @Override
                        public void onPictureTaken(final byte[] bytes) {
                            try {
                                int orientation = getExifOrientation(new ByteArrayInputStream(bytes));
                                // convert byte array into bitmap
                                Bitmap loadedImage;
                                loadedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                Matrix rotateMatrix = new Matrix();
                                switch (orientation) {
                                    case 2:
                                        rotateMatrix.setScale(-1, 1);
                                        break;
                                    case 3:
                                        rotateMatrix.setRotate(180);
                                        break;
                                    case 4:
                                        rotateMatrix.setRotate(180);
                                        rotateMatrix.postScale(-1, 1);
                                        break;
                                    case 5:
                                        rotateMatrix.setRotate(90);
                                        rotateMatrix.postScale(-1, 1);
                                        break;
                                    case 6:
                                        rotateMatrix.setRotate(90);
                                        break;
                                    case 7:
                                        rotateMatrix.setRotate(-90);
                                        rotateMatrix.postScale(-1, 1);
                                        break;
                                    case 8:
                                        rotateMatrix.setRotate(-90);
                                        break;
                                }
//                                      rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
//                                    loadedImage.getWidth(), loadedImage.getHeight(),
//                                    rotateMatrix, false);

                                Rect rect = new Rect();
                                rect.left = focusLeft;
                                rect.right = focusRight;
                                rect.top = focusTop;
                                rect.bottom = focusBottom;
                                Rect trect = new Rect(0, 0, loadedImage.getWidth(), loadedImage.getHeight());
                                rect = convertRectToCamera2(trect, rect, viewSize);
                                focusBottom = rect.bottom;
                                focusLeft = rect.left;
                                focusRight = rect.right;
                                focusTop = rect.top;
                                Bitmap tstbmp = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight());

                                tstbmp = Bitmap.createBitmap(tstbmp, 0, 0, tstbmp.getWidth(), tstbmp.getHeight(), rotateMatrix, true);
                                //SharedPreferences sharedPrefscheck = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                int qua_val = 30;//sharedPrefscheck.getInt("Quality", 30);
                                int size_val = 6;//sharedPrefscheck.getInt("Size", 6);
                                Matrix matrix = new Matrix();
                                float size;
                                size = (float) ((size_val * 60) + 120) / (float) tstbmp.getWidth();
                                matrix.postScale(size, size); //0.5f,0.5f);
                                tstbmp = Bitmap.createBitmap(tstbmp, 0, 0, tstbmp.getWidth(), tstbmp.getHeight(), matrix, true);
                                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                                tstbmp.compress(Bitmap.CompressFormat.JPEG, qua_val, ostream);
                                byte[] imageBytes = ostream.toByteArray();
                                ostream.close();

                                JP2Encoder encoder = new JP2Encoder(tstbmp);
                                encoder.setCompressionRatio(10);
                                byte[] jp2bytes = encoder.encode();

                                byte[] isoBytes = null;
                                isoBytes = BioFace.generateFaceISO2011(jp2bytes, tstbmp.getHeight(), tstbmp.getWidth(), "02");
                                Uri photoUri = Uri.fromFile(getTempFile(CameraActivity.this));
                                saveByteArray(imageBytes, photoUri);
                                captureSuccessful(photoUri, isoBytes, qua_val);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        btnFlipCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    reset();
                    if (cameraFaceID == 0) {
                        cameraFaceID = 1;
                    } else {
                        cameraFaceID = 0;
                    }
                    flashmode = false;
                    btnFlash.setImageResource(R.drawable.flash_off);

                    if (mCameraSource != null) {
                        mCameraSource.release();
                        mCameraSource = null;
                    }
                    MainActivity.sharedPreferences.edit().putInt("CameraFacing", cameraFaceID).apply();
                    createCameraSource();
                    startCameraSource();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashOnButton();
            }
        });
    }

    public File getTempFile(Context context) {
        try {
            return File.createTempFile("captured_", ".jp2", context.getCacheDir());
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

    public void captureSuccessful(Uri photoUri, byte[] isoFaceRecord, int quality) {
        Intent intent = new Intent();
        intent.setData(photoUri);
        intent.putExtra("Status", FaceCaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("IsoFaceRecord", isoFaceRecord);
        intent.putExtra("Quality", quality);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

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
        if (mCameraSource != null) {
            mCameraSource.stop();
            mCameraSource.release();
        }
        captureFailed(FaceCaptureResult.CAPTURE_CANCELLED, "capture cancelled");
        //finish();
    }

    private static Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }

    private Camera camera = null;
    boolean flashmode=false;
    private void flashOnButton() {
        camera=getCamera(mCameraSource);
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode?Camera.Parameters.FLASH_MODE_TORCH :Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
                if(flashmode){
                    btnFlash.setImageResource(R.drawable.flash_on);
                    showToast("Flash Switched ON");
                }
                else {
                    btnFlash.setImageResource(R.drawable.flash_off);
                    showToast("Flash Switched Off");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextureView.stop();
            }
        });
    }
    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, 9001);
            dlg.show();
        }
        if (mCameraSource != null) {
            try {
                mTextureView.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                showToast("Unable to start camera source.");
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceSteadyMills = 0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startCameraSource();
            }
        });

    }

    private Rect convertRectToCamera2(Rect crop_rect, Rect rect, com.google.android.gms.common.images.Size viewSize) {
        // CameraController.Area is always [-1000, -1000] to [1000, 1000] for the viewable region
        // but for CameraController2, we must convert to be relative to the crop region
        double left_f = (double) rect.left / (double) viewSize.getWidth();
        double top_f = (double) rect.top / (double) viewSize.getHeight();
        double right_f = (double) rect.right / (double) viewSize.getWidth();
        double bottom_f = (double) rect.bottom / (double) viewSize.getHeight();
        int left = (int) (crop_rect.left + left_f * (crop_rect.width() - 1));
        int right = (int) (crop_rect.left + right_f * (crop_rect.width() - 1));
        int top = (int) (crop_rect.top + top_f * (crop_rect.height() - 1));
        int bottom = (int) (crop_rect.top + bottom_f * (crop_rect.height() - 1));
        left = Math.max(left, crop_rect.left);
        right = Math.max(right, crop_rect.left);
        top = Math.max(top, crop_rect.top);
        bottom = Math.max(bottom, crop_rect.top);
        left = Math.min(left, crop_rect.right);
        right = Math.min(right, crop_rect.right);
        top = Math.min(top, crop_rect.bottom);
        bottom = Math.min(bottom, crop_rect.bottom);
        return  new Rect(left, top, right, bottom);
    }

    private static int getExifOrientation(InputStream inputStream) {
        int orientation = 1;
        try {
            // Extract metadata.
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            // Log each directory.
            for (Directory directory : metadata.getDirectories()) {

                // Log all tags.
                for (Tag tag : directory.getTags()) {
                    if (tag.getTagName().equals("Orientation") && tag.getDescription().contains("Right side, top"))
                        orientation = 6;
                    else if (tag.getTagName().equals("Orientation") && tag.getDescription().contains("Left side, bottom"))
                        orientation = 8;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return orientation;
    }

    public void onblinkEvent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //showToast("blink");
                //blinkOK = true;
                //btnTakePic.setEnabled(true);
                //btnTakePic.setImageResource(R.drawable.shutter_close);
                //progressBar.setProgress(100);
                //textView.setText("Pl take snap now.");
                btnTakePic.callOnClick();
            }
        });
    }

    public void onfaceDetectedEvent(final boolean isFace, Rect rect1, Face face,
                                    Size size, int frameWidth, int frameHeight) {
        viewSize = size;
        //isFaceDetect = isFace;
        if (isFace) {
            focusLeft = rect1.left;
            focusTop = rect1.top;
            focusRight = rect1.right;
            focusBottom = rect1.bottom;
        } else {
            reset();
            focusLeft = focusTop = focusRight = focusBottom = 0;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFace) {
                    //blinkOK = false;
                    //showToast("please focus on face.");
                    //btnTakePic.setEnabled(false);
                    //btnTakePic.setImageResource(R.drawable.snap_dis);
                    //progressBar.setProgress(25);
                    statusMsg.setText("Face Not clear");
                    faceSteadyMills = 0;
                    //textView.setText("please focus the camera on face.");
                }
                else {
                    if(face.getWidth() < 0.5f * frameWidth){
                        statusMsg.setText("Move Closer");
                        faceSteadyMills = 0;
                    }else if(face.getWidth() > 0.75f * frameWidth){
                        statusMsg.setText("Move back");
                        faceSteadyMills = 0;
                    } else {
                        float faceCenterX = face.getPosition().x + (0.5f * face.getWidth());
                        float faceCenterY = face.getPosition().y + (0.5f * face.getHeight());
                        float frameCenterX = 0.5f * frameWidth;
                        float frameCenterY = 0.5f * frameHeight;
                        float buffer = 0.1f * frameWidth;
                        if(Math.abs(frameCenterX - faceCenterX) > buffer
                                || Math.abs(frameCenterY - faceCenterY) > buffer){
                            statusMsg.setText("Center your face");
                        }else {
                            statusMsg.setText("Stay steady");
                            if (0 == faceSteadyMills) {
                                faceSteadyMills = System.currentTimeMillis();
                            } else if (System.currentTimeMillis() - faceSteadyMills > 1000) {
                                btnTakePic.callOnClick();
                            }
                        }
                    }
                    /*if(GooglyFaceTracker.gestureCount >= faceMaxGestures){
                        if(!gestureCcompleted) {
                            statusMsg.setText(strMsgs[4]);
                            updateGif(4);
                            gestureCcompleted = true;
                            *//*try {
                                toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 1000);
                                toneGenerator.startTone(ToneGenerator.TONE_DTMF_S, 150);
                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*//*
                        }
                    }else {
                        if (0 == GooglyFaceTracker.gestureCount && !gestureAcompleted) {
                            gestureAcompleted = true;
                            statusMsg.setText(strMsgs[randA]);
                            updateGif(randA);
                            GooglyFaceTracker.gestures[randB] = false;
                        }
                        if (1 == GooglyFaceTracker.gestureCount && !gestureBcompleted) {
                            statusMsg.setText(strMsgs[randB]);
                            GooglyFaceTracker.gestures[randB] = true;
                            updateGif(randB);
                            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 1000);
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_S, 150);
                            gestureBcompleted = true;
                        }
                    }*/
                }
            }
        });
    }

    public void showToast(final String text) {

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

    }

    //==============================================================================================
    // Detector
    //==============================================================================================

    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays randA low storage
     * warning if it was not possible to download the face library.
     */
    @NonNull
    private FaceDetector createFaceDetector(Context context) {
        // For both front facing and rear facing modes, the detector is initialized to do landmark
        // detection (to find the eyes), classification (to determine if the eyes are open), and
        // tracking.
        //
        // Use of "fast mode" enables faster detection for frontward faces, at the expense of not
        // attempting to detect faces at more varied angles (e.g., faces in profile).  Therefore,
        // faces that are turned too far won't be detected under fast mode.
        //
        // For front facing mode only, the detector will use the "prominent face only" setting,
        // which is optimized for tracking randA single relatively large face.  This setting allows the
        // detector to take some shortcuts to make tracking faster, at the expense of not being able
        // to track multiple faces.
        //
        // Setting the minimum face size not only controls how large faces must be in order to be
        // detected, it also affects performance.  Since it takes longer to scan for smaller faces,
        // we increase the minimum face size for the rear facing mode randA little bit in order to make
        // tracking faster (at the expense of missing smaller faces).  But this optimization is less
        // important for the front facing case, because when "prominent face only" is enabled, the
        // detector stops scanning for faces after it has found the first (large) face.
        boolean mIsFrontFacing = cameraFaceID == 1 ;
        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setMinFaceSize(0.5f)
                .build();

        Detector.Processor<Face> processor;
//        if (mIsFrontFacing) {
        // For front facing mode, randA single tracker instance is used with an associated focusing
        // processor.  This configuration allows the face detector to take some shortcuts to
        // speed up detection, in that it can quit after finding randA single face and can assume
        // that the nextIrisPosition face position is usually relatively close to the last seen
        // face position.
        Tracker<Face> tracker = new GooglyFaceTracker(this, this, mGraphicOverlay);

        processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
//        } else {
//            // For rear facing mode, randA factory is used to create per-face tracker instances.  A
//            // tracker is created for each face and is maintained as long as the same face is
//            // visible, enabling per-face state to be maintained over time.  This is used to store
//            // the iris position and velocity for each face independently, simulating the motion of
//            // the eyes of any number of faces over time.
//            //
//            // Both the front facing mode and the rear facing mode use the same tracker
//            // implementation, avoiding the need for any additional code.  The only difference
//            // between these cases is the choice of Processor: one that is specialized for tracking
//            // randA single face or one that can handle multiple faces.  Here, we use MultiProcessor,
//            // which is randA standard component of the mobile vision API for managing multiple items.
//            MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
//                @Override
//                public Tracker<Face> create(Face face) {
//                    return new GooglyFaceTracker(null,null);
//                }
//            };
//            processor = new MultiProcessor.Builder<>(factory).build();
//        }

        detector.setProcessor(processor);

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on randA device, GMS will
            // download randA native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Toast.makeText(this, "Face detector dependencies are not yet available.", Toast.LENGTH_LONG).show();
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Low storage", Toast.LENGTH_LONG).show();
            }
        }
        return detector;
    }

    private void createCameraSource() {
        reset();
        GooglyFaceTracker.gestures[0] = false;
        GooglyFaceTracker.gestures[1] = false;
        GooglyFaceTracker.gestures[2] = false;
        GooglyFaceTracker.gestures[3] = false;
        /*do {
            Random rand = new Random();
            randA = rand.nextInt(2);
            randB = rand.nextInt(4);
        }while (randA == randB || 5 == (randA + randB));*/
        randA = 0;
        randB = 1;

        /*if(randB < randA){
            int temp = randA;
            randA = randB;
            randB = temp;
        }*/
        //msg = "";
        //msg = strMsgs[randA] /*+ " , " + strMsgs[randB]*/;
        GooglyFaceTracker.gestures[randA] = true;
        //GooglyFaceTracker.gestures[randB] = true;

        Context context = getApplicationContext();
        FaceDetector detector = createFaceDetector(context);

        // The camera source is initialized to use either the front or rear facing camera.  We use randA
        // relatively low resolution for the camera preview, since this is sufficient for this app
        // and the face detector will run faster at lower camera resolutions.
        //
        // However, note that there is randA speed/accuracy trade-off with respect to choosing the
        // camera resolution.  The face detector will run faster with lower camera resolutions,
        // but may miss smaller faces, landmarks, or may not correctly detect eyes open/closed in
        // comparison to using higher camera resolutions.  If you have any of these issues, you may
        // want to increase the resolution.
        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(cameraFaceID)
                .setRequestedPreviewSize(640, 480)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build();
        startCameraTimer();
    }
    public void startCameraTimer() {
        try {
            if(null != timer){
                timer.purge();
                timer.cancel();
                timer = null;
            }
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (mCameraSource != null) {
                            mCameraSource.stop();
                            //mCameraSource.release();
                        }
                        if (timer != null) {
                            timer.cancel();
                            timer = null;
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM,1000);
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_S,300);
                    captureFailed(FaceCaptureResult.CAPTURE_TIMEOUT, "Capture Timeout");
                    //finish();
                }
            };
            timer.schedule(timerTask, faceCaptureTimeout);
        }catch (Exception ex){
            ex.getMessage();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(null != toneGenerator){
                    toneGenerator.release();
                }
                if(null != timer){
                    timer.purge();
                    timer.cancel();
                }
                if (mCameraSource != null) {
                    try {
                        mCameraSource.stop();
                    } catch (Exception ex) { ex.printStackTrace();
                    }
                    try {
                        mCameraSource.release();
                    } catch (Exception ex) { ex.printStackTrace();
                    }
                }
            }
        });
    }
    private void reset(){
        GooglyFaceTracker.gestureCount = 0;
        GooglyFaceTracker.verticalNod_min = 0;
        GooglyFaceTracker.verticalNod_max = 0;
        GooglyFaceTracker.horizontalNod_max = 0;
        GooglyFaceTracker.horizontalNod_min = 0;
        GooglyFaceTracker.smile_min = 0;
        GooglyFaceTracker.smile_max = 0;
        GooglyFaceTracker.SMILE_COMPLETED = false;
        GooglyFaceTracker.VERTICAL_NOD = false;
        GooglyFaceTracker.HORIZONTAL_NOD = false;
        GooglyFaceTracker.BLINK_COMPLETED = false;

        gestureBcompleted = false;
        gestureAcompleted = false;
        gestureCcompleted = false;
    }
    private void updateGif(int number){
        try {
            switch (number) {
                case 0: {
                    /*if (!RDMainActivity.sharedPreferences.getBoolean("GESTURES_MUTED",false)) {
                        player = new MediaPlayer();
                        AssetFileDescriptor afd = getAssets().openFd("Blink.mp3");
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                    }*/
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.blink)
                            .into(gesturesImg);
                    break;
                }
                case 1: {
                    /*if (!RDMainActivity.sharedPreferences.getBoolean("GESTURES_MUTED",false)) {
                        player = new MediaPlayer();
                        AssetFileDescriptor afd = getAssets().openFd("Smile.mp3");
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                    }*/
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.smile)
                            .into(gesturesImg);
                    break;
                }
                /*case 2: {
                    AssetFileDescriptor afd = null;
                    Random r= new Random();
                    int a = r.nextInt(2);
                    if(0 == a) {
                        statusMsg.setText("Turn Head Right");
                        afd = getAssets().openFd("TurnHeadRight.mp3");
                    }else {
                        statusMsg.setText("Turn Head Left");
                        afd = getAssets().openFd("TurnHeadLeft.mp3");
                    }
                    if(null!= afd && (!RDMainActivity.sharedPreferences.getBoolean("GESTURES_MUTED",false))) {
                        player = new MediaPlayer();
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                    }
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.horizontal)
                            .into(gesturesImg);
                    break;
                }
                case 3: {
                    if (!RDMainActivity.sharedPreferences.getBoolean("GESTURES_MUTED", false)) {
                        player = new MediaPlayer();
                        AssetFileDescriptor afd = getAssets().openFd("MoveHeadUp.mp3");
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                    }
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.vertical)
                            .into(gesturesImg);
                    break;
                }*/
                case 4: {
                    /*if (!RDMainActivity.sharedPreferences.getBoolean("GESTURES_MUTED",false)) {
                        player = new MediaPlayer();
                        AssetFileDescriptor afd = getAssets().openFd("LookStraight.mp3");
                        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        player.prepare();
                        player.start();
                    }*/
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.look_straight)
                            .into(gesturesImg);
                    break;
                }
                default: {
                    Glide.with(this)
                            //.asGif()
                            .load(R.raw.gestures)
                            .into(gesturesImg);
                    break;
                }
            }
        }
        catch(Exception ex){

        }
    }
}