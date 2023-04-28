package io.mosip.mock.sbi.device.device;

import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_FINGER_SINGLE_SUB_TYPE_ID;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.DEVICE_IRIS_SINGLE_SUB_TYPE_ID;

import android.content.Context;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mosip.mock.sbi.utility.utility.DeviceConstants;

public class AuthBioDevice extends BioDevice {
    public AuthBioDevice(Context appContext) {
        super(appContext);
    }

    @Override
    public Map<String, Uri> captureFingersModality(int deviceSubId, String[] bioSubType, String[] exception) {
        List<String> segmentsToCapture = null;
        switch (deviceSubId) {
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_LEFT: // left
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_RIGHT: // right
            case DEVICE_FINGER_SLAP_SUB_TYPE_ID_THUMB: // thumbs
                break; // double not implemented for auth
            case DEVICE_FINGER_SINGLE_SUB_TYPE_ID:
                segmentsToCapture = getSegmentsToCapture(
                        Arrays.asList(
                                DeviceConstants.BIO_NAME_RIGHT_INDEX,
                                DeviceConstants.BIO_NAME_RIGHT_MIDDLE,
                                DeviceConstants.BIO_NAME_RIGHT_RING,
                                DeviceConstants.BIO_NAME_RIGHT_LITTLE,
                                DeviceConstants.BIO_NAME_LEFT_INDEX,
                                DeviceConstants.BIO_NAME_LEFT_MIDDLE,
                                DeviceConstants.BIO_NAME_LEFT_RING,
                                DeviceConstants.BIO_NAME_LEFT_LITTLE,
                                DeviceConstants.BIO_NAME_LEFT_THUMB,
                                DeviceConstants.BIO_NAME_RIGHT_THUMB),
                        bioSubType == null ? null : Arrays.asList(bioSubType),
                        exception == null ? null : Arrays.asList(exception));
                break;
        }

        Map<String, Uri> uris = new HashMap<>();
        if (segmentsToCapture == null || segmentsToCapture.isEmpty()) {
            return uris;
        }

        segmentsToCapture.forEach(segment -> uris.put(segment,
                getBioAttributeURI(segmentUriMapping.get(segment))));
        return uris;
    }

    @Override
    public Map<String, Uri> captureIrisModality(int deviceSubId, String[] bioSubType, String[] exception) {
        List<String> segmentsToCapture = null;
        switch (deviceSubId) {
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_LEFT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_RIGHT:
            case DEVICE_IRIS_DOUBLE_SUB_TYPE_ID_BOTH:
                break; // double not implemented for auth
            case DEVICE_IRIS_SINGLE_SUB_TYPE_ID:
                segmentsToCapture = getSegmentsToCapture(Arrays.asList(
                                DeviceConstants.BIO_NAME_LEFT_IRIS,
                                DeviceConstants.BIO_NAME_RIGHT_IRIS),
                        bioSubType == null ? null : Arrays.asList(bioSubType),
                        exception == null ? null : Arrays.asList(exception));
                break;
        }
        Map<String, Uri> uris = new HashMap<>();
        if (segmentsToCapture == null || segmentsToCapture.isEmpty()) {
            return uris;
        }

        segmentsToCapture.forEach(segment -> uris.put(segment,
                getBioAttributeURI(segmentUriMapping.get(segment))));
        return uris;
    }

    @Override
    protected Uri getBioAttributeURI(String file) {
        byte[] isoRecord = getIsoDataFromAssets(DeviceConstants.DeviceUsage.Authentication.getDeviceUsage() + "/" + file);
        Uri isoUri = Uri.fromFile(getTempFile(appContext));
        saveByteArray(isoRecord, isoUri);
        return isoUri;
    }
}
