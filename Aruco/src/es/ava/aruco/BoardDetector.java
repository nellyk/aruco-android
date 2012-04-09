package es.ava.aruco;

import java.util.Vector;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class BoardDetector {
	
	public float detect(Vector<Marker> detectedMarkers, BoardConfiguration conf, Board bDetected,
			CameraParameters cp, float markerSizeMeters) throws CvException{
		return detect(detectedMarkers, conf, bDetected, 
				cp.getCameraMatrix(), cp.getDistCoeff(), markerSizeMeters);
	}
	
	private float detect(Vector<Marker> detectedMarkers, BoardConfiguration conf, Board bDetected,
			Mat camMatrix, Mat distCoeffs, float markerSizeMeters) throws CvException{
		bDetected.clear();
		// find among the detected markers those who belong to the board configuration
		int height = conf.height;
		int width = conf.width;
		int[][] detected = new int[width][height];// indices of the markers in the vector detectedMarkers
		for(int i=0;i<width;i++)
			for(int j=0;j<height;j++)
				detected[i][j] = -1;
		int nMarkInBoard = 0;// number of detected markers
		for(int i=0;i<detectedMarkers.size();i++){
			boolean found = false;
			int id = detectedMarkers.get(i).id;
			// find it
	        for(int j=0;j<height && ! found;j++)
	            for(int k=0;k<width && ! found;k++)
				if(conf.markersId[j][k] == id){
					detected[j][k] = i;
					nMarkInBoard++;
					found = true;
					bDetected.add(detectedMarkers.get(i));
					if(markerSizeMeters > 0)
						bDetected.lastElement().ssize = markerSizeMeters;
				}
		}
		bDetected.conf = conf;
		if(markerSizeMeters!=-1)
			bDetected.markerSizeMeters = markerSizeMeters;
		// calculate extrinsics
		if(camMatrix.rows()!=0 && markerSizeMeters>0 && detectedMarkers.size()>1){
			// create necessary matrix
			Mat objPoints = new Mat(4*nMarkInBoard, 3, CvType.CV_32FC1);
			Mat imgPoints = new Mat(4*nMarkInBoard, 2, CvType.CV_32FC1);
			// size in meters of the distance between markers
			float markerDistanceMeters = (conf.markerDistancePix) * markerSizeMeters / (conf.markerSizePix);
			
			int currIndex = 0;
	        for(int y=0;y<height;y++)
	            for(int x=0;x<width;x++) {
					if(detected[y][x] != -1){
						Vector<Point> points = detectedMarkers.get(detected[y][x]);
						// set image points
						double[] buffer = new double[8];
						for(int p=0;p<4;p++){
							buffer[p*2] = points.get(p).x;
							buffer[p*2+1] = points.get(p).y;
						}
						imgPoints.put(currIndex, 0, buffer);
						
						// translation to put the origin in the center
	                    float TX=-(  ((detected.length-1)*(markerDistanceMeters+markerSizeMeters) +markerSizeMeters)/2);
	                    float TY=-(  ((detected.length-1)*(markerDistanceMeters+markerSizeMeters) +markerSizeMeters)/2);
	                    //points in real reference system. We see the center in the bottom-left corner
	                    float AY=x*(markerDistanceMeters+markerSizeMeters ) +TY;
	                    float AX=y*(markerDistanceMeters+markerSizeMeters ) +TX;
	                    objPoints.put(currIndex, 0, 
	                    		AX,                  AY,                  0,
	                    		AX,                  AY+markerSizeMeters, 0,
	                    		AX+markerSizeMeters, AY+markerSizeMeters, 0,
	                    		AX+markerSizeMeters, AY,                  0);
	                    currIndex+=4;
					}
			}
	        // TODO get the opencv calls out of the loops
	        Calib3d.solvePnP(objPoints, imgPoints, camMatrix, distCoeffs, bDetected.Rvec, bDetected.Tvec);
//	        Utils.rotateXAxis(bDetected.Rvec); rotated later, in getModelViewMatrix
		}
		return ((float)nMarkInBoard/(float)(conf.width*conf.height));
	}
}
