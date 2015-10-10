package org.toxiclab.droidg;


import java.io.File;

import phex.common.Phex;
import phex.common.ThreadTracking;
import phex.event.PhexEventServiceImpl;
import phex.host.NetworkHostsContainer;
import phex.prefs.core.DownloadPrefs;
import phex.prefs.core.NetworkPrefs;
import phex.prefs.core.PhexCorePrefs;
import phex.servent.OnlineStatus;
import phex.servent.Servent;
import phex.utils.Localizer;
import phex.utils.SystemProperties;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class DPhex extends Activity {
    /** Called when the activity is first created. */
	
	Button btn_server = null;
	Button btn_search = null;
	Button btn_download = null;
	Button btn_whatnew = null;
	Button btn_browsehost = null;
	
	private static AssetManager am = null;
	public static TestSystemPref sp = null;
	public static DPhex _self = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
        	Toast.makeText(getApplicationContext(), "Please insert SD card before running this application.",
			          Toast.LENGTH_LONG).show();
        	return;
        }
        
        setContentView(R.layout.main_layout);
        setTitle("DroidG");
        am = getAssets();
        _self = this;
        
//      AdManager.setTestDevices( new String[] {
//		AdManager.TEST_EMULATOR,
//		// Android emulator
//		"84953AE3C4A0B2D18E775377ED1CCA40", // My T-Mobile G1 Test Phone
//		} );
        
        try{
        	//initialize first
            PhexEventServiceImpl.inf = new EventThreadService();
			sp = new TestSystemPref();
			SystemProperties.initSettingOutside(sp);
			
	
	        PhexCorePrefs.init();
	        //let it be US
	        Localizer.initialize( "");
	        ThreadTracking.initialize();
	
			Phex.initialize();
			Servent.getInstance();
			Servent.getInstance().start();
        }
        catch(Exception ex){
        	ex.printStackTrace();
        }
        
        btn_server = (Button)findViewById(R.id.Button01);
        btn_search = (Button)findViewById(R.id.Button02);
        btn_download = (Button)findViewById(R.id.Button03);
        btn_whatnew = (Button)findViewById(R.id.Button04);
        btn_browsehost = (Button)findViewById(R.id.Button05);
        
        btn_server.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				Intent intent = new Intent();
//				intent.setClass(DPhex.this, ConnectionActivity.class);
//				startActivity(intent);
				
				//find all connecting host and display it in a dialog
				NetworkHostsContainer hostContainer = Servent.getInstance().getHostService().getNetworkHostsContainer();
				int length = hostContainer.getNetworkHostCount();
				StringBuffer sb = new StringBuffer();
				int connected_host = 0;
				for(int i=0; i<length; ++i){
					if(hostContainer.getNetworkHostAt(i).isConnected()){
						sb.append(hostContainer.getNetworkHostAt(i).getHostAddress().toString());
						sb.append("\n");
						++connected_host;
					}
				}
				
				AlertDialog alertDialog = new AlertDialog.Builder(DPhex.this).create();
				alertDialog.setTitle("Connection status: ");
				StringBuffer sb2 = new StringBuffer();
				sb2.append("Connected to ");
				sb2.append( connected_host);
				sb2.append( " server(s)\n\n");
				sb2.append(sb.toString());
				alertDialog.setMessage(sb2.toString());
				alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				});
				alertDialog.show();
			}
        	
        });
        
        btn_search.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(DPhex.this, SearchActivity.class);
				startActivity(intent);
			}
        	
        });
        
        btn_download.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(DPhex.this, DownloadActivity.class);
				startActivity(intent);
			}
        	
        });
        
        btn_whatnew.setOnClickListener(new OnClickListener(){
        	public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(DPhex.this, WhatsNewActivity.class);
				startActivity(intent);
        	}
        });
        
        btn_browsehost.setOnClickListener(new OnClickListener(){
        	public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(DPhex.this, BrowseHostActivity.class);
				startActivity(intent);
        	}
        });
    }
    
    public static AssetManager getAssetManager(){
    	return am;
    }
    
    public void onPause(){
    	super.onPause();
    }
    
    public void onStop(){
    	super.onPause();
    }
    
    public boolean onOptionItemSelected(MenuItem item){
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.common_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch(item.getItemId()){
    	case R.id.credit:
    		displayCredit();
    		return true;
    	case R.id.setting:
    		displaySetting();
    		return true;
    	case R.id.exit:
    		exit();
    		return true;
    	case R.id.reconnect:
    		SearchActivity.getHandler().post(new Runnable(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Servent.getInstance().setOnlineStatus(OnlineStatus.OFFLINE);
	        		Servent.getInstance().setOnlineStatus(OnlineStatus.ONLINE);
	        		Toast.makeText(DPhex._self, "Reconnecting... This will take a few seconds.", Toast.LENGTH_LONG).show();
				}
    		});
    		
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    public static void displayCredit(){
    	Toast.makeText(DPhex._self, "This program is written by Tom Lai.\nVersion 1.1\n\tCurrent only support searching and downloading, sharing is not enable in this version.\nContact info:kongutoxiclab@gmail.com", Toast.LENGTH_LONG).show();
    }
    
    public static void exit(){
    	//do all the cleaning stuff
    	try{
	    	Servent.getInstance().stop();
	    	PhexCorePrefs.save( true );
	    	System.exit(0);
    	}
    	catch(Exception ex){ex.printStackTrace();}
    }
    
    public void displaySetting(){
		Intent intent = new Intent();
		intent.setClass(DPhex.this, DroidGPreference.class);
		startActivity(intent);
    }
}
