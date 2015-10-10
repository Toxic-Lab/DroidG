package org.toxiclab.droidg;

import java.util.Date;

import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.host.NetworkHostsContainer;
import phex.query.Search;
import phex.query.SearchContainer;
import phex.servent.Servent;
import phex.utils.StringUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class WhatsNewActivity extends Activity{
	private ListView list_view = null;
	private Button btn_search = null;
	
	//static part of the search
	private static WhatsNewAdapter adapter = null;
	private static Search search = null;
	private static WhatsNewActivity _self = null;
	private static SearchResultsDataModel _model = null;
	private static String title_format = "Searching for new stuffs Progress: %d%%";
	private static String title_normal = "What's new";
	private static Date search_start = null;
	private static MenuItem item = null;
	
    private static class ListViewUpdate implements Runnable{
		@Override
		public void run() {
			//if(scrolling) return;
			// TODO Auto-generated method stub
			getWhatsNewAdapater();
			if(adapter == null) {
				getHandler().postDelayed(this, 1000);
				return;
			}
			
			if(search == null){
				getHandler().postDelayed(this, 1000);
				return;
			}
//			long ct = (new Date().getTime() - search_start.getTime())/1000;
//			String sct = DPhex.formatTime(ct);
			String t = String.format(title_format, search.getProgress());
			_self.setTitle(t);
			
			if(adapter.isHaveUpdatesAndSetFalse()){
				getHandler().post(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						adapter.setNotificationPost();
						adapter.notifyDataSetChanged();
					}
				});
			}
			getHandler().postDelayed(this, 1000);
		}
    }
    
    private static ListViewUpdate listViewUpdate = new ListViewUpdate();
   
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resultlist);
        _self = this;
        list_view = (ListView)findViewById(R.id.ListView01);
        btn_search = (Button)findViewById(R.id.Button01);
        setTitle(title_normal);
        
        adapter = getWhatsNewAdapater();
        list_view.setAdapter(adapter);
        
        list_view.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				AlertDialog.Builder builder = new AlertDialog.Builder(WhatsNewActivity.this);
				
				builder.setTitle("Download...");
				final SearchResultElement se = adapter.getItem(arg2);
				builder.setMessage("Continue downloading " + se.getSingleRemoteFile().getFilename() + "?");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						final RemoteFile[] rfiles = se.getRemoteFiles();
						Runnable runner = new Runnable(){
							public void run(){
								try{
									SwarmingManager downloadService = Servent.getInstance().getDownloadService();
									for(int i=0; i<rfiles.length; ++i){
			                            rfiles[i].setInDownloadQueue( true );
			                            
			                            SWDownloadFile downloadFile = downloadService.getDownloadFile(
			                                rfiles[i].getFileSize(), rfiles[i].getURN() );
			            
			                            if ( downloadFile != null )
			                            {
			                                downloadFile.addDownloadCandidate( rfiles[i] );
			                            }
			                            else
			                            {
			                                RemoteFile dfile = new RemoteFile( rfiles[i] );
			                                String searchTerm = StringUtils.createNaturalSearchTerm( dfile.getFilename() );
			                                downloadService.addFileToDownload( dfile,
			                                    dfile.getFilename(), searchTerm );
			                            }
									}
									Toast.makeText(getApplicationContext(), se.getSingleRemoteFile().getFilename() + " is added to downloading list.",
									          Toast.LENGTH_SHORT).show();
								}
			                    catch ( Exception th )
			                    {
			                    	th.printStackTrace();
			                    }
							}
						};
						getHandler().post(runner);
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						return;
					}
				});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
        	
        });
        
        if(search == null || search.isSearchFinished())
        	btn_search.setText("Start a new search");
        else
        	btn_search.setText("Stop searching");
       
        btn_search.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				String text = btn_search.getText().toString();
				if(text.compareTo("Start a new search") == 0){
					btn_search.setText("Stop searching");
					if(item != null)
						item.setTitle("Stop searching");
					startWhatsNewSearch();
				}
				else{
					stop();
					if(item != null)
						item.setTitle("Start searching");
					btn_search.setText("Start a new search" );
				}
			}
        });
	}
	
	public void onPause(){
		super.onPause();
		getHandler().removeCallbacks(listViewUpdate);
	}
	
	public void onResume(){
		super.onResume();
		getHandler().postDelayed(listViewUpdate, 1);
	}
	
	public void onStop(){
		super.onStop();
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch(item.getItemId()){
    	case R.id.sort:
    		sort();
    		return true;
    	case R.id.stop:
            if(search == null || search.isSearchFinished()){
				btn_search.setText("Stop searching");
				item.setTitle("Stop searching");
				startWhatsNewSearch();
            }
            else{
            	stop();
            	item.setTitle("Start searching");
				btn_search.setText("Start a new search" );
    		}
    		return true;
    	case R.id.clear:
    		clear();
    		return true;
    	case R.id.credit:
    		DPhex.displayCredit();
    		return true;
    	case R.id.exit:
    		toDownloadScreen();
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.search_menu, menu);
    	
    	item = menu.findItem(R.id.stop);
        if(search == null || search.isSearchFinished())
        	item.setTitle("Start searching");
        else
        	item.setTitle("Stop searching");
    	
    	return true;
    }
    
    private static Handler getHandler(){
    	return SearchActivity.getHandler();
    }
    
    public void startWhatsNewSearch(){
		if(_model != null){
			if(search != null)
				_model.unregisterSearch(search);
			_model = null;
			search = null;
			search_start = null;
		}
		
		SearchContainer searchContainer = Servent.getInstance().getQueryService().getSearchContainer();
		search = searchContainer.createWhatsNewSearch( );
		_model = SearchResultsDataModel.registerNewSearch(search, Servent.getInstance().getQueryService().getSearchFilterRules());
		search_start = new Date();
		getWhatsNewAdapater().setDisplayedSearch(_model);
		
		NetworkHostsContainer hostContainer = Servent.getInstance().getHostService().getNetworkHostsContainer();
		int length = hostContainer.getNetworkHostCount();
		int connected_host = 0;
		for(int i=0; i<length; ++i){
			if(hostContainer.getNetworkHostAt(i).isConnected()){
				++connected_host;
			}
		}
		
		Toast.makeText(WhatsNewActivity.this, "Searching a What's new search from " + connected_host + " server. Please be patient.",  Toast.LENGTH_LONG).show();	
    }
    private static WhatsNewAdapter getWhatsNewAdapater(){
    	if(adapter == null)
    		adapter = new WhatsNewAdapter(_self, R.layout.search_item);
    	return adapter;
    }
    
    private void stop(){
    	if(search == null) return;
    	search.stopSearching();
    }
    
    private void clear(){
		if(_model != null){
			if(search != null)
				_model.unregisterSearch(search);
			_model = null;
			search = null;
			adapter.setDisplayedSearch(null);
			item.setTitle("Start searching");
			btn_search.setText("Start a new search" );
			getHandler().post(new Runnable(){
			
				@Override
				public void run() {
					// TODO Auto-generated method stub
					adapter.notifyDataSetChanged();
				}
			
			});
			WhatsNewActivity.this.setTitle(title_normal);;
		}
    }
    
    private void sort(){
    	//display a dialog for asking user to search for which dimension
    	//sort for type
    	//sort for speed
    	//sort for nhost
    	final String[] item = new String[] {"Type", "Speed", "Number of Sources"};
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Sort...");
    	builder.setSingleChoiceItems(item, -1, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(_model == null) return;
				// TODO Auto-generated method stub
				switch(which){
				case 0:
					_model.setSortBy(SearchResultElementComparator.SORT_BY_EXTENSION, false);
					return;
				case 1:
					_model.setSortBy(SearchResultElementComparator.SORT_BY_SPEED, false);
					return;
				case 2:
					_model.setSortBy(SearchResultElementComparator.SORT_BY_HOST, false);
					return;
				}
			}
		});
    	AlertDialog alert = builder.create();
    	alert.show();    	
    }
    
    private void toDownloadScreen(){
		Intent intent = new Intent();
		intent.setClass(this, DownloadActivity.class);
		startActivity(intent);
    }
    
}
