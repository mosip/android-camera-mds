package nprime.reg.sbi.faceCaptureApi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import in.nprime.jp2.JP2Encoder;
import npr.util.BioFace;
import nprime.reg.sbi.face.R;

import static android.content.ContentValues.TAG;

public class SimpleCaptureFragment extends Fragment {
    CaptureEvent captureEvent;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;
    private static boolean TEMPLATE_EXTRACTION_IN_PROCESS = false;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraCaptureSession mCaptureSession;

    private CameraDevice mCameraDevice;

    private ImageReader mImageReader;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private Size mPreviewSize;

    private String mCameraId;

    private int mSensorOrientation;

    private boolean mFlashSupported;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CaptureRequest mPreviewRequest;

    private int mState = STATE_PREVIEW;

    private Matrix mFaceDetectionMatrix;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private TextView mStatusMsg;
    private AutoFitTextureView mTextureView;
    private OverlayView mOverlayView;
    Button btnCapture;
    private boolean mSwappedDimensions;

    private static final int MAX_PREVIEW_WIDTH = 800;
    private static final int MAX_PREVIEW_HEIGHT = 600;

    private static int MaxWidth;
    private static int MaxHeight;
    //private static int faceWidthRequired;
    private Thread captureProcessThread;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public SimpleCaptureFragment(String cameraId, CaptureEvent captureEvent) {
        this.mCameraId = cameraId;
        this.captureEvent = captureEvent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_capture, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mStatusMsg = (TextView) view.findViewById(R.id.statuc_msg);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mOverlayView = (OverlayView) view.findViewById(R.id.overlay_view);
        btnCapture = (Button) view.findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mCaptureSession) {
                    lockFocus();
                }
            }
        });
    }


    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, 103);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            int centerX = 0;
            int centerY = 0;
            int faceWidth = 0;
            final Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            if (faces != null && mode != null) {
                if (faces.length > 0) {
                    for(int i = 0; i < faces.length; i++) {
                        if (faces[i].getScore() >= 1) {
                            Log.i("Test", "faces : " + faces.length + " , mode : " + mode);
                            int left = faces[i].getBounds().left;
                            int top = faces[i].getBounds().top;
                            int right = faces[i].getBounds().right;
                            int bottom = faces[i].getBounds().bottom;
                            Rect uRect = new Rect(left, top, right, bottom);
                            RectF rectF = new RectF(uRect);
                            mFaceDetectionMatrix.mapRect(rectF);

                            rectF.round(uRect);

                            Log.i("Test", "Activity rect" + i + " bounds: " + uRect);

                            centerX = uRect.centerX();
                            centerY = uRect.centerY();
                            faceWidth = uRect.width();
                            final Rect finalTempRect = uRect;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mOverlayView.setRect(finalTempRect);
                                    mOverlayView.requestLayout();
                                }
                            });
                            break;
                        }
                    }
                }
            }

            switch (mState) {
                case STATE_PREVIEW: {
                    if(null != captureProcessThread){
                        if(!captureProcessThread.isAlive()) {
                            captureProcess(centerX, centerY, faceWidth);
                        }
                    }else{
                        captureProcess(centerX, centerY, faceWidth);
                    }
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                            CaptureRequest.CONTROL_AE_STATE_INACTIVE == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private void captureProcess(final int finalCenterX, final int finalCenterY, final int finalFaceWidth){
        captureProcessThread = new Thread(){
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(finalCenterX == 0 || finalCenterY == 0 || finalFaceWidth == 0){
                            mStatusMsg.setText("Look into the camera");
                            return;
                        }
                        int faceWidthRequired = (int) (MaxWidth / 2);

                        if(finalFaceWidth > faceWidthRequired){
                            mStatusMsg.setText("Move back");
                        }/*else if(finalFaceWidth < MaxWidth / 5){
                            mStatusMsg.setText("Come close");
                        }*/
                        else { //Temp
                            mStatusMsg.setText("Stay steady");
                            if(null != mCaptureSession) {
                                lockFocus();
                            }
                        }
                        /*int x = finalCenterX - (MaxWidth/2);
                        int y = finalCenterY - (MaxHeight/2);
                        //int absX = Math.abs(x);
                        //int absY = Math.abs(y);

                        if(x > 25){
                            mStatusMsg.setText("Keep face at center\nMove face left");
                        }else if(x < -25){
                            mStatusMsg.setText("Keep face at center\nMove face Right");
                        }else if(y > 25){
                            mStatusMsg.setText("Keep face at center\nMove face Up");
                        }else if(y < -25){
                            mStatusMsg.setText("Keep face at center\nMove face Down");
                        }else {
                            mStatusMsg.setText("stay steady");
                            if(null != mCaptureSession && !TEMPLATE_EXTRACTION_IN_PROCESS) {
                                lockFocus();
                            }
                        }*/

                        /*if(absX < 50){
                            if(absY < 50){
                                int c = Math.abs((finalFaceWidth) - faceWidthRequired);
                                if(c < 20){
                                    if(null != mCaptureSession && !TEMPLATE_EXTRACTION_IN_PROCESS) {
                                        lockFocus();
                                    }
                                }else if(finalFaceWidth > faceWidthRequired){
                                    mStatusMsg.setText("Move back");
                                    //Toast.makeText(getContext(), "Move back", Toast.LENGTH_SHORT).show();
                                }else if(finalFaceWidth < faceWidthRequired){
                                    mStatusMsg.setText("Come close");
                                    //Toast.makeText(getContext(), "Come close", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                if(y > 0) {
                                    mStatusMsg.setText("Keep face at center\nMove face Up");
                                }else{
                                    mStatusMsg.setText("Keep face at center\nMove face Down");
                                }
                            }
                        }else{
                            if(x > 0){
                                mStatusMsg.setText("Keep face at center\nMove face left");
                            }else {
                                mStatusMsg.setText("Keep face at center\nMove face Right");
                            }
                            //mStatusMsg.setText("Move face to center");
                            //Toast.makeText(getContext(), "Move face to center", Toast.LENGTH_SHORT).show();
                        }*/
                    }
                });
            }
        };
        captureProcessThread.start();
    }

    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //showToast("Saved: " + mFile);
                    //Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            //mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            /*getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Unlock Focus", Toast.LENGTH_SHORT).show();
                }
            });*/
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            TEMPLATE_EXTRACTION_IN_PROCESS = true;
            /*getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Inside Image available", Toast.LENGTH_SHORT).show();
                }
            });*/
            Image image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            new Thread(){
                @Override
                public void run() {
                    TEMPLATE_EXTRACTION_IN_PROCESS = true;
                    extractTemplate(bytes);
                    TEMPLATE_EXTRACTION_IN_PROCESS = false;
                }
            }.start();
            image.close();
        }

    };

    private void extractTemplate(byte[] imageBytes) {
        try {
            //Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            int orientation = getExifOrientation(new ByteArrayInputStream(imageBytes));
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
            Bitmap loadedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            Bitmap bmp = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight());
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), rotateMatrix, true);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] rotatedImageBytes = outputStream.toByteArray();
            outputStream.close();

            JP2Encoder encoder = new JP2Encoder(bmp);
            encoder.setCompressionRatio(35);
            byte[] jp2bytes = encoder.encode();

            byte[] isoBytes = null;
            isoBytes = BioFace.generateFaceISO2011(jp2bytes, bmp.getHeight(), bmp.getWidth(), "02");
            Uri photoUri = Uri.fromFile(getTempFile(getContext()));
            saveByteArray(rotatedImageBytes, photoUri);
            captureSuccessFul(photoUri, isoBytes, 0);

            /*long strtTime = System.currentTimeMillis();
            int[] faceCount = new int[1];
            faceCount[0] = 1;
            nprTemplateStructure[] templateStructureArray = new nprTemplateStructure[faceCount[0]];
            int retCode = JavaNative.nprExtractTemplate(rotatedImageBytes, rotatedImageBytes.length, templateStructureArray, faceCount);
            if (0 == retCode && null != templateStructureArray[0]) {
                Uri photoUri = Uri.fromFile(getTempFile(getContext()));
                nprTemplateStructure templateStructure = templateStructureArray[0];
                saveByteArray(templateStructure.getFaceImage(), photoUri);
                long endTime = System.currentTimeMillis();
                final int timeTaken = (int)((endTime - strtTime)/1000);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Template Extraction Time : " + timeTaken, Toast.LENGTH_SHORT).show();
                    }
                });
                captureSuccessFul(photoUri, new String(templateStructure.getTemplateVersion()), templateStructure.getFaceTemplate());
            }*/
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private int getExifOrientation(InputStream inputStream) {
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

    public File getTempFile(Context context) {
        try {
            return File.createTempFile("captured_", ".jp2", context.getCacheDir());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create photo file.");
        }
    }


    private void captureSuccessFul(Uri photoUri, byte[] isoFaceRecord, int quality){
        captureEvent.captureSuccessful(photoUri, isoFaceRecord, quality);
        /*Intent intent = new Intent();
        intent.setData(photoUri);
        intent.putExtra("Status", FaceCaptureResult.CAPTURE_SUCCESS);
        intent.putExtra("ImageUri", photoUri);
        intent.putExtra("IsoFaceRecord", isoFaceRecord);
        intent.putExtra("Quality", quality);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();*/
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

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface surface = new Surface(texture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return;
                            }

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);
                                setAutoFlash(mPreviewRequestBuilder);

                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        boolean cameraAvailable = false;
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        for (String cameraId : cameraIdList) {
            if(cameraId.equals(mCameraId)){
                cameraAvailable = true;
                break;
            }
        }
        if(!cameraAvailable){
            Toast.makeText(getContext(), mCameraId + "not available", Toast.LENGTH_SHORT).show();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        //mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            //for (String cameraId : mCameraManager.getCameraIdList()) {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            int facing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            //if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
            StreamConfigurationMap map = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    /*if (map == null) {
                        continue;
                    }*/

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            //int[] faceDetectModes = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);

            int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mSwappedDimensions = false;

            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        mSwappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        mSwappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (mSwappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            // Check if the flash is supported.
            Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

            //mCameraId = cameraId;

            int orientationOffset = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Rect activeArraySizeRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            // Face Detection Matrix
            mFaceDetectionMatrix = new Matrix();
            // TODO - I guess that is not enough if we have a landscape layout too...
            mFaceDetectionMatrix.setRotate(orientationOffset);
            //mFaceDetectionMatrix.setRotate(270);

            Log.i("Test", "activeArraySizeRect1: (" + activeArraySizeRect + ") -> " + activeArraySizeRect.width() + ", " + activeArraySizeRect.height());
            Log.i("Test", "activeArraySizeRect2: " + mPreviewSize.getWidth() + ", " + mPreviewSize.getHeight());
            boolean cameraIsRotated = (orientationOffset == 90) || (orientationOffset == 270);
            boolean displayIsRotated = (displayRotation == Surface.ROTATION_90) ||
                    (displayRotation == Surface.ROTATION_270);
            float s1 = ((4.0f * width)/3.0f) / (float)activeArraySizeRect.width();//height / (float)activeArraySizeRect.width();
            float s2 = width / (float)activeArraySizeRect.height();

            MaxWidth = width;
            MaxHeight = (int) ((4.0f * width)/3.0f);
            //float s1 = mPreviewSize.getWidth() / (float)activeArraySizeRect.width();
            //float s2 = mPreviewSize.getHeight() / (float)activeArraySizeRect.height();
            boolean mirror = (facing == CameraCharacteristics.LENS_FACING_FRONT);
            boolean weAreinPortrait = true;
            int offsetDxDy = 100;
            if (mSwappedDimensions) {
                mFaceDetectionMatrix.postScale(mirror ? -s1 : s1, s2);
                //mFaceDetectionMatrix.setRotate(orientationOffset + 180);
                mFaceDetectionMatrix.postTranslate(width, mirror ? ((4.0f * width)/3.0f) : 0);
            } else {
                if (displayRotation == Surface.ROTATION_90) {
                    mFaceDetectionMatrix.setRotate(orientationOffset + 90);
                    mFaceDetectionMatrix.postScale(mirror ? -s1 : s1, s2);
                    mFaceDetectionMatrix.postTranslate(mPreviewSize.getWidth() + offsetDxDy, -offsetDxDy);
                } else if (displayRotation == Surface.ROTATION_270) {
                    mFaceDetectionMatrix.setRotate(orientationOffset + 270);
                    mFaceDetectionMatrix.postScale(mirror ? -s1 : s1, s2);
                    mFaceDetectionMatrix.postTranslate(-offsetDxDy, mPreviewSize.getHeight() + offsetDxDy);
                }
            }

            //}
            //}
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        /*int w = 600; //aspectRatio.getWidth();
        int h = 800;//aspectRatio.getHeight();

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        //aspect Ratio list
        List<Size> aspectRatioList = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() >= maxWidth && option.getHeight() >= maxHeight) {
                bigEnough.add(option);
                if (option.getHeight() == option.getWidth() * h / w){
                    aspectRatioList.add(option);
                }
            }
        }

        if (aspectRatioList.size() > 0) {
            return Collections.min(aspectRatioList, new CameraActivity.CompareSizesByArea());
        } else if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CameraActivity.CompareSizesByArea());
        } else {
            return Collections.max(Arrays.asList(choices), new CameraActivity.CompareSizesByArea());
        }*/

        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = 800;//aspectRatio.getWidth();
        int h = 600;//aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}