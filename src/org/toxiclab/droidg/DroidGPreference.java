package org.toxiclab.droidg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class DroidGPreference extends Activity {
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferencelayout);
		ListView lv = (ListView)findViewById(R.id.ListView01);
		lv.setAdapter(new DroidGPrefAdapter(this, R.layout.preferenceitem));
		setTitle("DroidG Setting");
	}
}
