package es.ava.aruco.sample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import es.ava.aruco.Marker;

public class NewMarkerActivity extends Activity{

	// layout fields
	private EditText mMarkerId;
	private ImageView mMarkerView;
	private Button mSaveButton;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.new_marker);
		
		// get the layout fields
		mMarkerView = (ImageView) findViewById(R.id.marker);
		mMarkerId = (EditText) findViewById(R.id.id);
		mSaveButton = (Button) findViewById(R.id.buttonSave);
		
		mSaveButton.setEnabled(false);
	}
	
	/** Buttons actions */
	public void createMarker(View view){
		try{
			Mat marker = Marker.createMarkerImage(Integer.valueOf(mMarkerId.getText().toString()),280);
			Bitmap bmp = Bitmap.createBitmap(marker.cols(), marker.rows(), Bitmap.Config.ARGB_8888);
			Imgproc.cvtColor(marker, marker, Imgproc.COLOR_GRAY2RGBA, 4);
			Utils.matToBitmap(marker, bmp);
			mMarkerView.setImageBitmap(bmp);
			mSaveButton.setEnabled(true);
		} catch(Exception e){
			// TODO position the toast
			showToast("invalid id for marker");
		}
	}
	
	public void saveMarker(View view){
		  File path = Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_PICTURES);
		  File file = new File(path, "marker.jpg");// TODO add time mark to name
		  path.mkdirs();// make sure the picture dir exists
		  FileOutputStream os = null;
		  try {
			  os = new FileOutputStream(file);
			  mMarkerView.buildDrawingCache();
			  mMarkerView.getDrawingCache().compress(Bitmap.CompressFormat.JPEG, 0, os);
			  showToast("The marker was saved to your pictures directory");
			  sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
		  } catch (FileNotFoundException fnfe) {
			  showToast("couldn't save the marker");
		  } finally {
			  if (os != null) {
				  try {
					  os.close();
				  } catch (IOException ioe) {
					  // do nothing
				  }
			  }
		  }
	}
	
	private void showToast(String message){
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}
}
