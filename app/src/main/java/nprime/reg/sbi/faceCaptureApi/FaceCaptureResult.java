package nprime.reg.sbi.faceCaptureApi;

import android.graphics.Bitmap;

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
	byte[] isoFaceRecord = null;
	int qualityScore = 0;

	int status = CAPTURE_FAILED;
	
	public Bitmap getCapturedImage() {
		return capturedImage;
	}
	public void setCapturedImage(Bitmap capturedImage) {
		this.capturedImage = capturedImage;
	}
	
	
	public byte[] getIsoFaceRecord() {
		return isoFaceRecord;
	}
	public void setIsoFaceRecord(byte[] isoFaceRecord) {
		this.isoFaceRecord = isoFaceRecord;
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
