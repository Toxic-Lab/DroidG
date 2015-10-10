package org.toxiclab.droidg;

import java.io.File;

import phex.common.Environment;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadInfo;
import phex.download.swarming.SwarmingManager;
import phex.servent.Servent;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DownloadActivity extends Activity{
	
	private DownloadAdapter adapter;
	private Handler m_handler;
	private ListView list_view;
	private Gui_update gui_update;
	
	private SWDownloadFile context_current = null;
	private SwarmingManager downloadService = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);
        setTitle("Download");
        downloadService = Servent.getInstance().getDownloadService();

        adapter = new DownloadAdapter(this, R.layout.download_item, downloadService);
        list_view = (ListView)findViewById(R.id.ListView01);
        list_view.setAdapter(adapter);
        registerForContextMenu(list_view);
        m_handler = new Handler();
        gui_update = new Gui_update();
        
        m_handler.postDelayed(gui_update, 1);
	}
	
	public void onPause(){
		m_handler.removeCallbacks(gui_update);
		super.onPause();
	}
	
	public void onResume(){
		m_handler.postDelayed(gui_update, 1);
		super.onResume();
	}
	
	public void onStop(){
		m_handler.removeCallbacks(gui_update);
		super.onStop();
	}
	
	class Gui_update implements Runnable{

		@Override
		public void run() {
			//if(scrolling) return;
			// TODO Auto-generated method stub
			if(adapter == null){
				m_handler.postDelayed(this, 1000);
				return;
			}
			adapter.notifyDataSetChanged();
			m_handler.postDelayed(this, 1000);
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	  if (v.getId()==R.id.ListView01) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    
	    menu.setHeaderTitle("Actions");
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.download_context, menu);
	    AdapterView.AdapterContextMenuInfo info1 = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    SWDownloadFile file = adapter.getItem(info1.position);
	    context_current = file;
	    
	    MenuItem open = menu.findItem(R.id.open);
	   if(file.isPreviewPossible())
		   open.setEnabled(true);
	   else
		   open.setEnabled(false);
	   
	    //MenuItem details = menu.findItem(R.id.details);
	    MenuItem start = menu.findItem(R.id.start);
	    if(file.isDownloadStopped())
	    	if(!file.isFileCompletedOrMoved())
	    		start.setEnabled(true);
	    	else
	    		start.setEnabled(false);
	    else
	    	start.setEnabled(false);
	    MenuItem stop = menu.findItem(R.id.stop);
	    if(file.isDownloadStopped())
	    	stop.setEnabled(false);
	    else
	    	if(!file.isFileCompletedOrMoved())
	    		stop.setEnabled(true);
	    	else
	    		stop.setEnabled(false);
	  }
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	  switch (item.getItemId()) {
	  case R.id.start:
	    if(context_current == null) return true;
		context_current.startDownload();
	    return true;
	  case R.id.stop:
		    if(context_current == null) return true;
			context_current.stopDownload();
	    return true;
	  case R.id.details:
		  //do a alert dialog
		  if(context_current == null) return true;
		  showPeerStatus();		  
		  return true;
	  case R.id.remove:
		  removeDownload();
		  return true;
	  case R.id.open:
		  if(context_current == null) return true;
		  previewFile();
		  return true;
	  default:
	    return super.onContextItemSelected(item);
	  }
	}
	
	private void removeDownload(){
        SWDownloadFile files = context_current;

        SWDownloadFile warningFiles = null;
        SWDownloadFile removeFiles = null;
        if ( files == null )
        {
            return;
        }
        if ( files.isFileCompletedMoved() )
        {
            removeFiles= files ;
        }
        else if ( files.isFileCompleted() )
        {
            // ignore the intermediate state of a file between beeing
            // completed and beeing completed moved
        }
        // if download not started.
        else if ( files.getTransferredDataSize() == 0 )
        {
            removeFiles = files ;
        }
        else
        {// if not completed... schedule for warning
            warningFiles = files ;
        }

        Integer warningSize = warningFiles == null ? 0 : 1;
        if(warningSize == 1)
        {
        	removeFiles = warningFiles;
        }

        // do the remove...
        if ( removeFiles != null )
        {
            final SWDownloadFile[] filesToRemove = new SWDownloadFile[]{removeFiles};
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        downloadService.removeDownloadFiles( filesToRemove );
                    }
                    catch ( Throwable th )
                    {
                        //NLogger.error( RemoveDownloadAction.class, th, th);
                    	System.err.println(th.toString());
                    }
                }
            };
            //Environment.getInstance().executeOnThreadPool(runner, "RemoveDownloadFiles" );
            m_handler.post(runner);
        }
	}
	
	private void previewFile(){
        try
        {
            final SWDownloadFile file = context_current;
            if ( file == null ) return;
            Runnable runner = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        File previewFile = file.getPreviewFile();
                        openFile(previewFile );
                    }
                    catch ( Throwable th )
                    {
                        //NLogger.error( GeneratePreviewAction.class, th, th);
                    }
                }
            };
            Environment.getInstance().executeOnThreadPool(runner, "GenerateDownloadPreview" );
        }
        catch ( Throwable th )
        {
            //NLogger.error( GeneratePreviewAction.class, th, th);
        }
	}
	
	private void openFile(File f){
		Intent intent = new Intent( Intent.ACTION_VIEW );
		File f1 = f;
		 
		// 檔名小寫, 容易判斷副檔名
		String vlowerFileName = f.getName().toLowerCase();
		 
		// 影片
		if(  vlowerFileName.endsWith("mpg")
		  || vlowerFileName.endsWith("mp4")
		  )
		    intent.setDataAndType( Uri.fromFile(f1), "video/*" );
		// 音樂
		else if( vlowerFileName.endsWith("mp3") )
		    intent.setDataAndType( Uri.fromFile(f1), "audio/*" );
		// 影像
		else if( vlowerFileName.endsWith("bmp")
		      || vlowerFileName.endsWith("gif")
		      || vlowerFileName.endsWith("jpg")
		      || vlowerFileName.endsWith("png")
		  )
		    intent.setDataAndType( Uri.fromFile(f1), "image/*" );
		// 文字檔
		else if( vlowerFileName.endsWith("txt")
		      || vlowerFileName.endsWith("html")
		      )
		    intent.setDataAndType( Uri.fromFile(f1), "text/*" );
		// Android APK
		else if( vlowerFileName.endsWith("apk")
		      )
		    intent.setDataAndType( Uri.fromFile(f1), "application/vnd.android.package-archive" );
		// 其他
		else
		    intent.setDataAndType( Uri.fromFile(f1), "application/*" );
		 
		// 切換到開啟的檔案
		startActivity(intent);
	}
	
	private void showPeerStatus(){
		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
		  builder.setTitle("Peer status");
		  int cnt = context_current.getCandidatesCount();
		  StringBuffer sb = new StringBuffer();
		  for(int i=0; i<cnt; ++i){
			  SWDownloadCandidate candidate = context_current.getCandidate( i );
			  sb.append(candidate.getHostAddress());
			  sb.append("\n");
			  sb.append("\t");
			  sb.append(SWDownloadInfo.getDownloadCandidateStatusString2(candidate ));
			  sb.append("\n");
		  }
		  builder.setMessage(sb.toString());
		  AlertDialog dialog = builder.create();
		  dialog.setButton("OK", new OnClickListener(){

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				return;
			}
			  
		  });
		  dialog.show();
	}
	
	    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.common_menu, menu);
    	MenuItem item = menu.findItem(R.id.exit);
    	item.setTitle("Back");
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	switch(item.getItemId()){
    	case R.id.credit:
    		DPhex.displayCredit();
    		return true;
    	case R.id.setting:
    		displaySetting();
    		return true;
    	case R.id.exit:
    		finish();
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    public void displaySetting(){
		Intent intent = new Intent();
		intent.setClass(this, DroidGPreference.class);
		startActivity(intent);
    }
    
}
