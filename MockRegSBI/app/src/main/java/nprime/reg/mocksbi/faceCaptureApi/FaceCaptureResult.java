package nprime.reg.mocksbi.faceCaptureApi;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author NPrime Technologies
 */

public class FaceCaptureResult
{
	public static final int	CAPTURE_SUCCESS = 0;
	public static final int CAPTURE_STARTED = 1;
	public static final int	CAPTURE_COMPLETED = 2;
	
	public static final int	CAPTURE_FAILED = -1;
	public static final int	CAPTURE_CANCELLED = -2;
	public static final int CAPTURE_TIMEOUT = -3;
	public static final int CAPTURE_NOT_STARTED = -4;
	public static final int PREVIEW_NOT_STARTED = -5;
	public static final int PREVIEW_INPROGRESS = -6;
	public static final int ISO_2011_INVALID = -7;
	
	Bitmap capturedImage = null;

	Map<String, byte[]> biometricRecords = new HashMap<>();

	private String modality;

	public int getBioSubId() {
		return bioSubId;
	}

	public void setBioSubId(int bioSubId) {
		this.bioSubId = bioSubId;
	}

	private int bioSubId;

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public Map<String, byte[]> getBiometricRecords() {
		return biometricRecords;
	}

	public void setBiometricRecords(Map<String, byte[]> biometricRecords) {
		this.biometricRecords = biometricRecords;
	}

	int qualityScore = 0;

	int status = CAPTURE_FAILED;
	
	public Bitmap getCapturedImage() {
		return capturedImage;
	}
	public void setCapturedImage(Bitmap capturedImage) {
		this.capturedImage = capturedImage;
	}
	

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
		
		if(CAPTURE_COMPLETED == status){
			this.status = CAPTURE_SUCCESS;
		}
	}
	
	public int getQualityScore() {
		return qualityScore;
	}
	public void setQualityScore(int qualityScore) {
		this.qualityScore = qualityScore;
	}
	
	
}
