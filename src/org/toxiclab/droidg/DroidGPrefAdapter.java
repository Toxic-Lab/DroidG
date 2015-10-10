package org.toxiclab.droidg;

import phex.prefs.core.NetworkPrefs;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DroidGPrefAdapter extends ArrayAdapter<String>{
	String[] key = new String[]{"File Directory", "Configuration Directory", "Port"};
	private Context m_context;
	public DroidGPrefAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		m_context = context;
		// TODO Auto-generated constructor stub
	}    
	
	@Override
	public int getCount(){
		return key.length;
	}
	
	@Override
	public String getItem(int i){
		return key[i];
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		View result = null;

		if(convertView == null){
			//inflate the xml view
			LayoutInflater inflate = (LayoutInflater)
            		m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			RelativeLayout layout = (RelativeLayout)inflate.inflate(R.layout.preferenceitem, null);
			result = layout;
		}
		else{
			//use the view directly
			result = convertView;
		}
		
		String se = getItem(position);
		TextView t1 = (TextView)result.findViewById(R.id.TextView01);
		t1.setText(se);
		TextView t2 = (TextView)result.findViewById(R.id.TextView02);
		if(position == 1){
			t2.setText(DPhex.sp.getPhexConfigRoot().getAbsolutePath());
		}
		else if(position == 2){
			String a = String.valueOf(NetworkPrefs.ListeningPort.get());
			t2.setText(a);
		}
		else if(position == 0){
			t2.setText(DPhex.sp.getPhexDownloadsRoot().getAbsolutePath());
		}
		

		return result;
	}
}

