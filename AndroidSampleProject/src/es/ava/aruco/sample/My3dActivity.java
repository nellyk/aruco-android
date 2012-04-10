package es.ava.aruco.sample;

import java.util.Vector;

import min3d.core.Object3dContainer;
import min3d.objectPrimitives.Box;
import min3d.vos.Light;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import es.ava.aruco.Board;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.android.Aruco3dActivity;
import es.ava.aruco.exceptions.ExtParamException;

public class My3dActivity extends Aruco3dActivity {
	
	private Object3dContainer testCube;
	
	@Override
	public void initDetectionParam() {
		lookForBoard = false;
		// initialize camera parameters and marker size in meters
		mCamParam = new CameraParameters();
		mMarkerSize = 0.034f;
	}

	@Override
	public void onInitScene(){
		super.onInitScene();
		
		scene.lights().add(new Light());
		testCube = new Box(mMarkerSize,mMarkerSize,mMarkerSize, null, true,true,false);
		// you can move the object from the marker position
		testCube.position().y = mMarkerSize/2f;
	}
	
	@Override
	public void onUpdateScene(){
		// do here translations, rotations and all 3d stuff.
//		testCube.rotation().y++;
	}
	
	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers) {
		if(detectedMarkers.size()>0){
			for(int i=0;i<detectedMarkers.size();i++){
				detectedMarkers.get(i).draw(frame, new Scalar(0,255,0), 3, true);
				detectedMarkers.get(i).draw3dCube(frame, mCamParam, new Scalar(255,0,255));
			}
			try {
				scene.addChild(testCube);
				detectedMarkers.get(0).set3dObject(testCube);
			} catch (ExtParamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
			scene.removeChild(testCube);
	}

	@Override
	public void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability) {
		// TODO Auto-generated method stub
		
	}
}