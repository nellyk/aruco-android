package es.ava.aruco.android;

import java.util.Vector;

import min3d.core.RendererActivity;
import min3d.vos.Light;

import org.opencv.core.Mat;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import es.ava.aruco.Board;
import es.ava.aruco.BoardConfiguration;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;

public abstract class Aruco3dActivity extends RendererActivity {
	
	public boolean lookForBoard;
	public CameraParameters mCamParam;
	public float mMarkerSize;
	public BoardConfiguration mBC;
	
	View view;// TODO set to private¿?
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		initDetectionParam();
		super.onCreate(savedInstanceState);
	}
	
    @Override 
	protected void glSurfaceViewConfig()
    {
		// !important
        _glSurfaceView.setEGLConfigChooser(8,8,8,8, 16, 0);
        _glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
    
    @Override
	public void onInitScene() {
		// !important
		scene.backgroundColor().setAll(0x00000000);
		scene.lights().add(new Light());
	}
	
	public void onCreateSetContentView(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		addContentView(_glSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, 
				LayoutParams.FILL_PARENT));
		view = new View(this, this, mCamParam, mMarkerSize);
		addContentView(view, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
	}
	
	public abstract void initDetectionParam();
	public abstract void onDetection(Mat frame, Vector<Marker> detectedMarkers);
	public abstract void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability);
}