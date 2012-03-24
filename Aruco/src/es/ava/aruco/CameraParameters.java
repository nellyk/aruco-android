package es.ava.aruco;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import es.ava.aruco.exceptions.CPException;

public class CameraParameters {

	private Mat cameraMatrix;
	private Mat distorsionMatrix;
	private Size camSize;
	
	public CameraParameters(){
		// TODO sample for debug in my htc desire
		cameraMatrix = new Mat(3,3,CvType.CV_32FC1);
		cameraMatrix.put(0, 0,
				2.4634878668323222e+03,                     0., 1.3115898157817151e+03,
				                    0., 2.4515981478104272e+03, 7.7127636431427027e+02,
				                    0.,                     0.,                      1. );
		distorsionMatrix = new Mat(5,1,CvType.CV_32FC1);
		distorsionMatrix.put(0,0,
				1.2330078495750021e-01, -2.2284860800065850e-01,
				-2.5943373042754116e-05, -8.4022079973864469e-04,
				5.4942029257895009e-01 );
	}

	public CameraParameters(CameraParameters other){
		// TODO implement the copy constructor
		if(other != null){
			cameraMatrix = new Mat(3,3,CvType.CV_32FC1);
			cameraMatrix.put(0, 0,
					2.4634878668323222e+03,                     0., 1.3115898157817151e+03,
					                    0., 2.4515981478104272e+03, 7.7127636431427027e+02,
					                    0.,                     0.,                      1. );
			distorsionMatrix = new Mat(5,1,CvType.CV_32FC1);
			distorsionMatrix.put(0,0,
					1.2330078495750021e-01, -2.2284860800065850e-01,
					-2.5943373042754116e-05, -8.4022079973864469e-04,
					5.4942029257895009e-01 );
		}
	}
	
    /**Indicates whether this object is valid
     */
    public boolean isValid(){
        return cameraMatrix.rows()!=0 && cameraMatrix.cols()!=0  && 
        	distorsionMatrix.rows()!=0 && distorsionMatrix.cols()!=0;
//        	&& camSize.width!=-1 && camSize.height!=-1;// TODO
    }
    
	public void readFromFile(String fileName){
		
	}
	
	public Mat getCameraMatrix(){
		return cameraMatrix;
	}
	
	public Mat getDistCoeff(){
		return distorsionMatrix;
	}
	
	public void resize(Size size) throws CPException{
	    if (!isValid()) 
	    	throw new CPException("invalid object CameraParameters::resize");
	    if (size == camSize)
	    	return;
	    //now, read the camera size
	    //resize the camera parameters to fit this image size
	    float AxFactor= (float)(size.width)/ (float)(camSize.width);
	    float AyFactor= (float)(size.height)/ (float)(camSize.height);
		float[] current = new float[9];
	    cameraMatrix.get(0, 0, current);
		float[] buff = {current[0]*AxFactor, current[1],          current[2]*AxFactor,
				        current[3],          current[4]*AyFactor, current[5],
				        current[6],          current[7],          current[8]};
		cameraMatrix.put(0, 0, buff);
	}
}
