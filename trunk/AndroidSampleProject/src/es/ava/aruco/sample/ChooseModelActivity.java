package es.ava.aruco.sample;

import java.util.Vector;

import min3d.core.Object3dContainer;
import min3d.objectPrimitives.Sphere;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import android.graphics.Bitmap;
import android.view.Menu;
import android.view.MenuItem;
import aruco.min3d.Shared;
import aruco.min3d.Utils;
import es.ava.aruco.Board;
import es.ava.aruco.Marker;
import es.ava.aruco.android.Aruco3dActivity;
import es.ava.aruco.exceptions.ExtParamException;

public class ChooseModelActivity extends Aruco3dActivity {
	private Object3dContainer earth;
	private Object3dContainer car;

    private MenuItem            carOption;
    private MenuItem            earthOption;
    
    private int modelSelected = 0;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        carOption = menu.add("Car");
        earthOption = menu.add("Earth");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == carOption)
        	modelSelected = 0;
        else if (item == earthOption)
        	modelSelected = 1;
        return true;
    }
    
	@Override
	public void initScene() {
		super.onInitScene();
		scene.lights().add(new Light());
		
		IParser parser = Parser.createParser(Parser.Type.OBJ,
				getResources(), "es.ava.aruco.sample:raw/camaro_obj", true);
		parser.parse();

		car = parser.getParsedObject();
		car.initialScale(mMarkerSize/2);
		car.scale().x = car.scale().y = car.scale().z = mMarkerSize/2;
		car.isVisible(true);
		
		Bitmap b = Utils.makeBitmapFromResourceId(R.drawable.earth);
		Shared.textureManager().addTextureId(b, "earth", false);
		b.recycle();
				
		earth = new Sphere(0.8f, 15, 10);
		earth.initialScale(mMarkerSize);
		earth.scale().x = earth.scale().y = earth.scale().z = mMarkerSize;
		earth.textures().addById("earth");
//		objModel.rotation().x = -90;
		earth.isVisible(true);
		
		scene.addChild(car);
		scene.addChild(earth);
	}

	@Override
	public void updateScene() {
		if(earth.selected){
			// enable rotation on z axis with the x direction of the touch screen
			// and scale with the pinch gesture
			earth.rotation().z = mView.mAngleX();
			earth.rotation().x = mView.mAngleY();
			earth.scale().x=earth.scale().y=earth.scale().z = 
				mView.mScale()*earth.initialScale();;
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
			if(modelSelected == 1){
				if(car != null){
					car.isVisible(false);
				}
				try {
					// because it is possible that the process arrives here before the objects have
					// been initialized, due to they are done in separate thread TODO
					if(earth != null){
						earth.isVisible(true);
						detectedMarkers.get(0).set3dObject(earth);
					}
				} catch (ExtParamException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				if(earth != null){
					earth.isVisible(false);
				}
				try {
					// because it is possible that the process arrives here before the objects have
					// been initialized, due to they are done in separate thread TODO
					if(car != null){
						car.isVisible(true);
						detectedMarkers.get(0).set3dObject(car);
					}
				} catch (ExtParamException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		else{
			if(earth != null){
				earth.isVisible(false);
			}
			if(car != null){
				car.isVisible(false);
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
