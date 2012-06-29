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
		monster.initialScale(mMarkerSize/20);
		monster.scale().x = monster.scale().y = monster.scale().z  = mMarkerSize/20;
//		monster.rotation().x = 90;
		monster.isVisible(false);
		
		parser = Parser.createParser(Parser.Type.OBJ,
				getResources(), "es.ava.aruco.sample:raw/camaro_obj", true);
		parser.parse();

		objModel = parser.getParsedObject();
		objModel.initialScale(mMarkerSize/2);
		objModel.scale().x = objModel.scale().y = objModel.scale().z = mMarkerSize/2;
//		objModel.rotation().x = -90;
		objModel.isVisible(false);
		
		Bitmap b = Utils.makeBitmapFromResourceId(R.drawable.barong);
		Shared.textureManager().addTextureId(b, "barong", false);
		b.recycle();
		cube = new Box(mMarkerSize,mMarkerSize,mMarkerSize);
		cube.textures().addById("barong");
		cube.vertexColorsEnabled(false);
//		cube.position().y = mMarkerSize/2f;
		cube.isVisible(false);
		
		scene.addChild(cube);
		scene.addChild(objModel);
		scene.addChild(monster);
	}

	@Override
	public void updateScene() {
//		objModel.rotation().x++;
//		cube.rotation().z++;
		if(objModel.selected){
			objModel.rotation().z = mView.mAngleX();
//			objModel.rotation().x = mView.mAngleY();
			objModel.scale().x=objModel.scale().y=objModel.scale().z = mView.mScale()*mMarkerSize/2;
		}
		if(cube.selected){
			cube.rotation().z = mView.mAngleX();
			cube.rotation().x = mView.mAngleY();
			cube.scale().x=cube.scale().y=cube.scale().z = mView.mScale();
		}
		if(monster.selected){
			monster.rotation().z = mView.mAngleX();
//			monster.rotation().x = mView.mAngleY();
			monster.scale().x=monster.scale().y=monster.scale().z = mView.mScale()*mMarkerSize/20;
		}
	}

	@Override
	public void initDetectionParam() {
		mLookForBoard = false;
		mMarkerSize = 0.034f;
		mShowFps = true;
	}

	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers, int idSelected) {
		if(detectedMarkers.size() >= 1)
			try {
				// because it is possible that the process arrives here before the objects have
				// been initialized, due to they are done in separate thread
				if(cube != null){
//					cube.isVisible(true);
					objModel.isVisible(true);
//					monster.isVisible(true);
				}
				
//				detectedMarkers.get(0).set3dObject(cube);
				detectedMarkers.get(0).set3dObject(objModel);
//				detectedMarkers.lastElement().set3dObject(monster);
				
//				detectedMarkers.lastElement().draw3dAxis(frame, mCamParam, new Scalar(255,0,0));
			} catch (ExtParamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else{
			if(cube != null){
				cube.isVisible(false);
				objModel.isVisible(false);
				monster.isVisible(false);
			}
		}
		for(Marker m : detectedMarkers){
			if(m.getMarkerId() == idSelected){
				m.draw(frame, new Scalar(255,0,0), 3, true);
				if(m.object()!=null)
					m.object().selected = true;
			}
			else{
				m.draw(frame, new Scalar(0,255,0), 3, true);
				if(m.object() != null)
					m.object().selected = false;
			}
		}
	}

	@Override
	public void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability) {

	}
}
