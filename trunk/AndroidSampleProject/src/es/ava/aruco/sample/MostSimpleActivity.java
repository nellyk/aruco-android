package es.ava.aruco.sample;

import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import es.ava.aruco.Board;
import es.ava.aruco.Marker;
import es.ava.aruco.android.Aruco3dActivity;

/**
 * This example shows how to build a simple activity. No 3d scene is used, just
 * opencv functions through Aruco library. It draws a square around any marker detected.
 * By touching any marker we select it and a 3d cube (using only opencv) will be drawn.
 * @author Rafael Ortega
 *
 */
public class MostSimpleActivity extends Aruco3dActivity{

	@Override
	public void initDetectionParam() {
		mMarkerSize = 0.034f;	
	}

	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers, int idSelected) {
		for(Marker m : detectedMarkers){
			if(m.getMarkerId() == idSelected){
				m.draw3dCube(frame, super.mView.mCamParam, new Scalar(255,0,0));
			}
			else{
				m.draw(frame, new Scalar(255,0,0), 3, true);
			}
		}
	}

	@Override
	public void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability) {
		
	}

}
