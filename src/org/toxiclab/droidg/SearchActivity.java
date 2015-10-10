package org.toxiclab.droidg;

import java.util.Date;

import phex.common.Environment;
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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class SearchActivity extends Activity{

	private EditText txt_keyword = null;
	private Button btn_search = null;
	private ListView list_view = null;
	
	//only needed to init one.
	private static SearchAdapter adapter = null;
	private static String _search_kw = null;
	private static Search _search = null;
	private static Date search_start = null;
	private static SearchResultsDataModel _model = null;
	private static Handler m_handler = null;
	private static String title_format = "Searching for \"%s\" Progress: %d%%";
	private static String title_nothing = "Search";
	private static SearchActivity _self = null;
	//private static volatile boolean scrolling = false;
	
	private static ProgressDialog pd = null;

    private static class ListViewUpdate implements Runnable{
		@Override
		public void run() {
			//if(scrolling) return;
			// TODO Auto-generated method stub
			if(adapter == null){
				m_handler.postDelayed(this, 1000);
				return;
			}
			
			if(_search == null){
				m_handler.postDelayed(this, 1000);
				return;
			}
			
//			long ct = (new Date().getTime() - search_start.getTime())/1000;
//			String sct = DPhex.formatTime(ct);
			String t = String.format(title_format, _search_kw, _search.getProgress());
			_self.setTitle(t);
			
			if(adapter.isHaveUpdatesAndSetFalse()){
				m_handler.post(new Runnable(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						adapter.setNotificationPost();
						adapter.notifyDataSetChanged();
					}
				});
			}
			
			m_handler.postDelayed(this, 1000);
		}
    }
    
    private static ListViewUpdate listViewUpdate = new ListViewUpdate();
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        
        txt_keyword = (EditText)findViewById(R.id.EditText01);
        btn_search = (Button)findViewById(R.id.Button01);
        list_view = (ListView)findViewById(R.id.ListView01);
        _self = this;
        //if search is not null change back the title
        if(_search == null){
        	this.setTitle("Searching");
        }
        else{
        	setTitle("Searching for: " + _search_kw);
        	txt_keyword.setText(_search_kw);
        }
        //call to init the handler
        getHandler();
        
        //if search is not null change back the title
        adapter = getSearchAdapter();
        list_view.setAdapter(adapter);
        
        //start the reverse way
        getHandler().postDelayed(listViewUpdate, 1);
        //end of the reverse way
        
        btn_search.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(_model != null){
					if(_search != null){
						_search.stopSearching();
						_model.unregisterSearch(_search);
					}
					_model = null;
					_search = null;
					_search_kw = null;
					search_start = null;
					SearchActivity.this.setTitle("Searching");
				}
				
				SearchContainer searchContainer = Servent.getInstance().getQueryService().getSearchContainer();
				_search_kw = txt_keyword.getText().toString();
				search_start = new Date();
				_search = searchContainer.createSearch(_search_kw);
				_model = SearchResultsDataModel.registerNewSearch(_search, Servent.getInstance().getQueryService().getSearchFilterRules());
				adapter.setDisplayedSearch(_model);
				setTitle("Searching for: " + _search_kw);

				NetworkHostsContainer hostContainer = Servent.getInstance().getHostService().getNetworkHostsContainer();
				int length = hostContainer.getNetworkHostCount();
				int connected_host = 0;
				for(int i=0; i<length; ++i){
					if(hostContainer.getNetworkHostAt(i).isConnected()){
						++connected_host;
					}
				}
				
				Toast.makeText(SearchActivity.this, "Searching " + _search_kw + " from " + connected_host + " server. Please be patient.",  Toast.LENGTH_LONG).show();
			}
        });
        
        list_view.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
				
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
		getHandler().removeCallbacks(listViewUpdate);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.search_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch(item.getItemId()){
    	case R.id.sort:
    		sort();
    		return true;
    	case R.id.stop:
    		stop();
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
    
    private void stop(){
    	if(_search == null) return;
    	_search.stopSearching();
    	SearchActivity.this.setTitle(
    		SearchActivity.this.getTitle() + " (Stopped)");
    }
    
    private void clear(){
		if(_model != null){
			if(_search != null)
				_model.unregisterSearch(_search);
			_model = null;
			_search = null;
			adapter.setDisplayedSearch(null);
			
			getHandler().post(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					adapter.notifyDataSetChanged();
				}
			
			});
			SearchActivity.this.setTitle("Searching");
		}
    }
    
    public static Handler getHandler(){
    	if(m_handler == null)
    		m_handler = new Handler();
    	return m_handler;
    }
    
    private SearchAdapter getSearchAdapter(){
    	if(adapter == null)
    		adapter = new SearchAdapter(this, R.layout.search_item);
    	return adapter;
    }
    
    private void toDownloadScreen(){
		Intent intent = new Intent();
		intent.setClass(this, DownloadActivity.class);
		startActivity(intent);
    }
}
