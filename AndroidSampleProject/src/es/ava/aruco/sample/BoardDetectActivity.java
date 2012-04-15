package es.ava.aruco.sample;

import java.util.Vector;

import min3d.animation.AnimationObject3d;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import es.ava.aruco.Board;
import es.ava.aruco.BoardConfiguration;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.android.Aruco3dActivity;
import es.ava.aruco.exceptions.ExtParamException;

public class BoardDetectActivity extends Aruco3dActivity {
	private AnimationObject3d ogre;
	
	@Override
	public void initScene() {
		super.onInitScene();
		scene.lights().add(new Light());

		IParser parser = Parser.createParser(Parser.Type.MD2,
				getResources(), "es.ava.aruco.sample:raw/ogro", false);
		parser.parse();

		ogre = parser.getParsedAnimationObject();
		ogre.scale().x = ogre.scale().y = ogre.scale().z = mMarkerSize/20;
		ogre.rotation().z = -90;
		ogre.rotation().x = -90;
		scene.addChild(ogre);
		ogre.setFps(50);
		ogre.play();
	}

	@Override
	public void updateScene() {

	}

	@Override
	public void initDetectionParam() {
		int[] ids = {200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224};
		int[][] markersId = new int[5][5];
		int index = 0;
		for(int i=0;i<5;i++)
			for(int j=0;j<5;j++){
				markersId[i][j] = ids[index];
				index++;
			}
		lookForBoard = true;
		mBC = new BoardConfiguration(5,5,markersId,100,20);
		mCamParam = new CameraParameters();
		mMarkerSize = 0.034f;
	}

	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers) {
		
	}

	@Override
	public void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability) {
		if(probability > 0.2){
			mBoardDetected.draw3dAxis(mFrame, mCamParam, new Scalar(255,0,0));
			try {
				scene.addChild(ogre);
				mBoardDetected.set3dObject(ogre);
			} catch (ExtParamException e) {
				e.printStackTrace();
			}
		}
		else
			scene.removeChild(ogre);
	}
}
