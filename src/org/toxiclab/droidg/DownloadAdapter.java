package org.toxiclab.droidg;

import java.util.ArrayList;

import phex.common.TransferDataProvider;
import phex.common.format.NumberFormatUtils;
import phex.common.format.TimeFormatUtils;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadInfo;
import phex.download.swarming.SwarmingManager;
import phex.utils.Localizer;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DownloadAdapter extends ArrayAdapter<SWDownloadFile>{
	private Context m_context;
	private int m_id;
	private SwarmingManager downloadService;
	private Boolean has_update = false;
	private ArrayList<SWDownloadFile> underlying;
	private Object underlying_lock = new Object();
	
    public DownloadAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		// TODO Auto-generated constructor stub
		m_context = context;
		m_id = textViewResourceId;
	}
    
    public DownloadAdapter(Context context, int textViewResourceId, SwarmingManager downloadService){
    	this(context, textViewResourceId);
    	this.downloadService = downloadService;
    }

	@Override
	public int getCount(){
		synchronized(underlying_lock){
			try{
				int x = downloadService.getDownloadFileCount();
				underlying = new ArrayList<SWDownloadFile>();
				for(int i=0; i<x; ++i)
					underlying.add(downloadService.getDownloadFile( i ));
			}
			catch(Exception ex){
				ex.printStackTrace();
			}	
			
		}
		if(underlying == null)return 0;
		else return underlying.size();
	}

	@Override
	public SWDownloadFile getItem(int i){
		SWDownloadFile se;
		synchronized(underlying_lock){
			try{
				se = underlying.get(i);
			}
			catch(Exception ex){
				se = null;
			}
		}
		return se;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		View result;
		if(convertView == null){
			//inflate the xml view
			LayoutInflater inflate = (LayoutInflater)
            		m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			RelativeLayout layout = (RelativeLayout)inflate.inflate(R.layout.download_item, null);
			result = layout;
		}
		else{
			//use the view directly
			result = convertView;
		}
		
		SWDownloadFile se = getItem(position);
		if(se == null) return result;
		
		try{
			ImageView type = (ImageView)result.findViewById(R.id.ImageView01);
			TextView file = (TextView)result.findViewById(R.id.TextView01);
			TextView size = (TextView)result.findViewById(R.id.TextView02);
			TextView speed = (TextView)result.findViewById(R.id.TextView03);
			TextView status = (TextView)result.findViewById(R.id.TextView04);
			
			int typex = SearchAdapter.getType(se.getFileName());
			if(typex == SearchAdapter.FILETYPE_AUDIO)
				type.setImageResource(R.drawable.music);
			else if(typex == SearchAdapter.FILETYPE_IMAGE)
				type.setImageResource(R.drawable.picture);
			else if(typex == SearchAdapter.FILETYPE_ZIP)
				type.setImageResource(R.drawable.pack);
//			else if(typex == SearchAdapter.FILETYPE_DOC)
//				type.setImageResource(R.drawable.document);
			else if(typex == SearchAdapter.FILETYPE_VIDEO)
				type.setImageResource(R.drawable.video);
			else
				type.setImageResource(R.drawable.document);
			
			file.setText(se.getFileName().toString());
			size.setText(getTransferSize(se));
			speed.setText(NumberFormatUtils.formatSignificantByteSize2(se.getTransferSpeed() ) + "/s");
			status.setText(SWDownloadInfo.getDownloadFileStatusString2(se.getStatus()));
		}
		catch(Exception ex){
			ex.printStackTrace();
		}

		return result;
	}
	public String getTransferSize(TransferDataProvider provider){

        long transferredSize = provider.getTransferredDataSize();
        long transferSize = provider.getTransferDataSize();
        StringBuffer buffer = new StringBuffer();
        buffer.append( NumberFormatUtils.formatSignificantByteSize2( transferredSize ) );
        if ( transferSize > -1 )
        {
            buffer.append( " / " );
            buffer.append( NumberFormatUtils.formatSignificantByteSize2( transferSize ) );
        }
        return buffer.toString();
   
    }
    
    public String getETA(TransferDataProvider provider){

        long transferredSize = provider.getTransferredDataSize();
        long totalTransferSize = provider.getTransferDataSize();
        long transferRate = provider.getLongTermTransferRate();
        
        if ( totalTransferSize == -1 || 
             provider.getDataTransferStatus() != TransferDataProvider.TRANSFER_RUNNING )
        {
            return  "" ;
        }
        else
        {
            long timeRemaining;
            if ( transferRate == 0 )
            {
                timeRemaining = TransferDataProvider.INFINITY_ETA_INT;
            }
            else
            {
                timeRemaining = (long)((totalTransferSize - transferredSize) / transferRate);
                timeRemaining = Math.max( 0, timeRemaining );
            }
    
            // estimated time of arival
            if ( timeRemaining < TransferDataProvider.INFINITY_ETA_INT )
            {
                return ( TimeFormatUtils.formatSignificantElapsedTime2( timeRemaining ) );
            }
            else
            {
                return ( Localizer.getDecimalFormatSymbols().getInfinity() );
            }
        }
    }
    
    @Override
    public boolean hasStableIds (){
    	return false;
    }
    
    public void setHaveUpdates(boolean values){
    	synchronized(has_update){
    		has_update = true;
    	}
    }
    
    public boolean isHaveUpdatesAndSetFalse(){
    	synchronized(has_update){
    		boolean result = has_update;
    		has_update = false;
    		return result;
    	}
    }
    
    
}
