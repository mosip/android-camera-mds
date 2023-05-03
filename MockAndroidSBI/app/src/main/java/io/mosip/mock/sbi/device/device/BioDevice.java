package io.mosip.mock.sbi.device.device;

import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_FACE;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_INDEX;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_IRIS;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_LITTLE;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_MIDDLE;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_RING;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_LEFT_THUMB;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_INDEX;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_IRIS;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_LITTLE;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_RING;
import static io.mosip.mock.sbi.utility.utility.DeviceConstants.PROFILE_BIO_FILE_NAME_RIGHT_THUMB;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.common.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.mosip.mock.sbi.utility.utility.DeviceConstants;

public abstract class BioDevice {
    protected final Map<String, String> segmentUriMapping;
    protected Context appContext;

    public BioDevice(Context appContext) {
        this.appContext = appContext;

        segmentUriMapping = new HashMap<>();
        segmentUriMapping.put("", PROFILE_BIO_FILE_NAME_FACE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_INDEX, PROFILE_BIO_FILE_NAME_LEFT_INDEX);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_MIDDLE, PROFILE_BIO_FILE_NAME_LEFT_MIDDLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_RING, PROFILE_BIO_FILE_NAME_LEFT_RING);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_LITTLE, PROFILE_BIO_FILE_NAME_LEFT_LITTLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_INDEX, PROFILE_BIO_FILE_NAME_RIGHT_INDEX);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_MIDDLE, PROFILE_BIO_FILE_NAME_RIGHT_MIDDLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_RING, PROFILE_BIO_FILE_NAME_RIGHT_RING);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_LITTLE, PROFILE_BIO_FILE_NAME_RIGHT_LITTLE);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_THUMB, PROFILE_BIO_FILE_NAME_LEFT_THUMB);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_THUMB, PROFILE_BIO_FILE_NAME_RIGHT_THUMB);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_LEFT_IRIS, PROFILE_BIO_FILE_NAME_LEFT_IRIS);
        segmentUriMapping.put(DeviceConstants.BIO_NAME_RIGHT_IRIS, PROFILE_BIO_FILE_NAME_RIGHT_IRIS);
    }

    public Map<String, Uri> captureFaceModality() {
        Map<String, Uri> uris = new HashMap<>();
        uris.put("", getBioAttributeURI(segmentUriMapping.get("")));
        return uris;
    }

    public abstract Map<String, Uri> captureFingersModality(int deviceSubId, String[] bioSubType, String[] exception);

    public abstract Map<String, Uri> captureIrisModality(int deviceSubId, String[] bioSubType, String[] exception);

    protected abstract Uri getBioAttributeURI(String file);

    protected List<String> getSegmentsToCapture(List<String> defaultSubTypes, List<String> bioSubTypes, List<String> exceptions) {
        List<String> localCopy = new ArrayList<>(defaultSubTypes);
        if (exceptions != null) {
            localCopy.removeAll(exceptions);
        }

        List<String> segmentsToCapture = new ArrayList<>();
        if (bioSubTypes == null || bioSubTypes.isEmpty()) {
            segmentsToCapture.addAll(localCopy);
            return segmentsToCapture;
        } else {
            Random rand = new Random();
            for (String bioSubType : bioSubTypes) {
                if (localCopy.contains(bioSubType)) {
                    segmentsToCapture.add(bioSubType);
                } else if ("UNKNOWN".equals(bioSubType)) {
                    String randSubType = defaultSubTypes.get(rand.nextInt(defaultSubTypes.size()));
                    while (bioSubTypes.contains(randSubType) && bioSubTypes.size() <= localCopy.size()) {
                        randSubType = defaultSubTypes.get(rand.nextInt(defaultSubTypes.size()));
                    }
                    segmentsToCapture.add(randSubType);
                }
            }
        }
        return segmentsToCapture;
    }

    protected byte[] getIsoDataFromAssets(String assetFileName) {
        try (InputStream in = appContext.getAssets().open(assetFileName)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void saveByteArray(final byte[] data, final Uri uri) {
        final File file = new File(uri.getPath());

        try {
            final OutputStream os = Files.newOutputStream(file.toPath());
            os.write(data);
            os.flush();
            os.close();
        } catch (final Exception e) {
            throw new RuntimeException("Unable to store data.", e);
        }
    }

    protected File getTempFile(Context context) {
        try {
            return File.createTempFile("isoRecord_", ".dat", context.getCacheDir());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create photo file.");
        }
    }
}
