package es.ava.aruco.sample;

import java.util.Vector;

import min3d.core.Object3dContainer;
import min3d.objectPrimitives.Box;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import android.graphics.Bitmap;
import aruco.min3d.Shared;
import aruco.min3d.Utils;
import es.ava.aruco.Board;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.R;
import es.ava.aruco.android.Aruco3dActivity;
import es.ava.aruco.exceptions.ExtParamException;

public class Min3dTestActivity extends Aruco3dActivity {
	private Object3dContainer objModel;
	private Object3dContainer cube;
	private Object3dContainer monster;

	@Override
	public void initScene() {
		super.onInitScene();
		scene.lights().add(new Light());
		
		IParser parser = Parser.createParser(Parser.Type.MAX_3DS,
				getResources(), "es.ava.aruco.sample:raw/monster_high", false);
		parser.parse();

		monster = parser.getParsedObject();
		monster.scale().x = monster.scale().y = monster.scale().z  = mMarkerSize/20;		
		
		parser = Parser.createParser(Parser.Type.OBJ,
				getResources(), "es.ava.aruco.sample:raw/camaro_obj", true);
		parser.parse();

		objModel = parser.getParsedObject();
		objModel.scale().x = objModel.scale().y = objModel.scale().z = mMarkerSize/2;
		objModel.rotation().x = -90;
		
		Bitmap b = Utils.makeBitmapFromResourceId(R.drawable.barong);
		Shared.textureManager().addTextureId(b, "barong", false);
		b.recycle();
		cube = new Box(mMarkerSize,mMarkerSize,mMarkerSize);
		cube.textures().addById("barong");
		cube.vertexColorsEnabled(false);
		cube.position().y = mMarkerSize/2f;
	}

	@Override
	public void updateScene() {
//		objModel.rotation().x++;
//		objModel.rotation().z++;
	}

	@Override
	public void initDetectionParam() {
		mLookForBoard = false;
		mCamParam = new CameraParameters();
		mMarkerSize = 0.034f;
		mShowFps = false;
	}

	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers, int idSelected) {
		for(Marker m : detectedMarkers){
			m.draw(frame, new Scalar(0,255,0), 3, true);
			if(m.getMarkerId() == idSelected)
				m.draw3dCube(frame, mCamParam, new Scalar(0,0,255));
		}
		if(detectedMarkers.size() > 2)
			try {
				scene.addChild(cube);
				scene.addChild(objModel);
				scene.addChild(monster);
				detectedMarkers.get(0).set3dObject(cube);
				detectedMarkers.get(1).set3dObject(objModel);
				detectedMarkers.lastElement().set3dObject(monster);
			} catch (ExtParamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else{
			scene.removeChild(cube);
			scene.removeChild(objModel);
			scene.removeChild(monster);
		}
	}

	@Override
	public void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability) {

	}
}
