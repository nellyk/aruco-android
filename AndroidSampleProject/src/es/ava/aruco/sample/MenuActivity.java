package es.ava.aruco.sample;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MenuActivity extends ListActivity{

	private class SampleActivity
	{
		public Class<?> cls;
		public String label;

		public SampleActivity(String $label, Class<?> $class)
		{
			label = $label;
			cls = $class;
		}
	}
	
	private SampleActivity[] _items = {
			new SampleActivity("Animated model on board", BoardDetectActivity.class),
			new SampleActivity("Markers detection", Min3dTestActivity.class),
			new SampleActivity("OBJ in a marker", SingleMarkerOBJActivity.class),
			new SampleActivity("Most simple example", MostSimpleActivity.class),
			new SampleActivity("Create marker", NewMarkerActivity.class),
			new SampleActivity("Selectable model", ChooseModelActivity.class),
//			new SampleActivity("Calibrate camera", CameraCalibrationActivity.class)
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	String[] strings = new String[_items.length];
    	for (int i = 0; i < _items.length; i++) {
    		strings[i] = _items[i].label;
    	}
    	
	    super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_layout);
	    setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, strings));
	    
	    TextView tv = (TextView) this.findViewById(R.id.menuTitle);
	    Linkify.addLinks(tv, 0x07);
	    
	    registerForContextMenu(getListView());
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id)
    {
    	this.startActivity( new Intent(this, _items[position].cls ) );
    }
}
