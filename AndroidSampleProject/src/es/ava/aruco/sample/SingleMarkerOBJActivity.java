package es.ava.aruco.sample;

import java.util.Vector;

import min3d.core.Object3dContainer;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import es.ava.aruco.Board;
import es.ava.aruco.Marker;
import es.ava.aruco.android.Aruco3dActivity;
import es.ava.aruco.exceptions.ExtParamException;

public class SingleMarkerOBJActivity extends Aruco3dActivity {
	private Object3dContainer objModel;

	@Override
	public void initScene() {
		super.onInitScene();
		scene.lights().add(new Light());
		
		IParser parser = Parser.createParser(Parser.Type.OBJ,
				getResources(), "es.ava.aruco.sample:raw/camaro_obj", true);
		parser.parse();

		objModel = parser.getParsedObject();
		objModel.initialScale(mMarkerSize/2);
		objModel.scale().x = objModel.scale().y = objModel.scale().z = mMarkerSize/2;
		objModel.isVisible(false);
		
		scene.addChild(objModel);
	}

	@Override
	public void updateScene() {
		if(objModel.selected){
			// enable rotation on z axis with the x direction of the touch screen
			// and scale with the pinch gesture
			objModel.rotation().z = mView.mAngleX();
			objModel.scale().x=objModel.scale().y=objModel.scale().z = 
				mView.mScale()*objModel.initialScale();
		}
	}

	@Override
	public void initDetectionParam() {
		mLookForBoard = false;
		mMarkerSize = 0.034f;
	}

	@Override
	public void onDetection(Mat frame, Vector<Marker> detectedMarkers, int idSelected) {
		if(detectedMarkers.size() >= 1)
			try {
				// because it is possible that the process arrives here before the objects have
				// been initialized, due to they are done in separate thread TODO
				if(objModel != null){
					objModel.isVisible(true);
					detectedMarkers.get(0).set3dObject(objModel);
				}
			} catch (ExtParamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else{
			if(objModel != null){
				objModel.isVisible(false);
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
