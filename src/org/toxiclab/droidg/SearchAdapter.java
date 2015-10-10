package org.toxiclab.droidg;

import java.util.ArrayList;

import phex.common.MediaType;
import phex.common.format.NumberFormatUtils;
import phex.download.RemoteFile;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchAdapter extends ArrayAdapter<SearchResultElement>{
    public static final int FILE_MODEL_INDEX = 0;
    public static final int EXTENSION_MODEL_INDEX = 1;
    public static final int SIZE_MODEL_INDEX = 2;
    public static final int SCORE_MODEL_INDEX = 3;
    public static final int HOST_RATING_MODEL_INDEX = 4;
    public static final int HOST_SPEED_MODEL_INDEX = 5;
    public static final int HOST_MODEL_INDEX = 6;
    public static final int HOST_VENDOR_MODEL_INDEX = 7;
    public static final int META_DATA_MODEL_INDEX = 8;
    public static final int SHA1_MODEL_INDEX = 9;
    
    /**
     * The unique column id is not allowed to ever change over Phex releases. It
     * is used when serializing column information. The column id is containd in
     * the identifier field of the TableColumn.
     */
    public static final int HOST_COLUMN_ID = 1001;
    public static final int FILE_COLUMN_ID = 1002;
    public static final int EXTENSION_COLUMN_ID = 1003;
    public static final int SIZE_COLUMN_ID = 1004;
    public static final int SCORE_COLUMN_ID = 1005;
    public static final int HOST_RATING_COLUMN_ID = 1006;
    public static final int HOST_SPEED_COLUMN_ID = 1007;
    public static final int HOST_VENDOR_COLUMN_ID = 1008;
    public static final int META_DATA_COLUMN_ID = 1009;
    public static final int SHA1_COLUMN_ID = 1010;
    //public static final int BITZI_RATING_COLUMN_ID = 1011;
    
    
	private Context m_context;
	private int m_id;
	private int sortedColumn = -1;
	private boolean isAscending = false;
	private SearchResultsDataModel displayedDataModel = null;
	//public Handler m_handler;
	private Boolean has_update = false;
	private ArrayList<SearchResultElement> underlying;
	private Object underlying_lock = new Object();
	private Boolean notification_post = true;
	private String error = null;
	
	public SearchAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		// TODO Auto-generated constructor stub
		m_context = context;
		m_id = textViewResourceId;
		//m_handler = new Handler();
	}

    public void setDisplayedSearch( SearchResultsDataModel dataModel )
    {
        // otherwise no need to update...
        if ( displayedDataModel != dataModel )
        {
            // unregister...
            if ( displayedDataModel != null )
            {
            	//stop searching and release the resources
            	displayedDataModel.getSearch().stopSearching();
                displayedDataModel.setVisualizationModel( null );
                displayedDataModel.release();
            }
            displayedDataModel = dataModel;
            
            
            SearchActivity.getHandler().post(new Runnable(){
            	public void run(){
            		emptyCache();
            	}
            });
            
            if ( displayedDataModel != null )
            {
                //sortByColumn( sortedColumn, isAscending );
                displayedDataModel.setVisualizationModel( this );
            }
        }
    }
    
	@Override
	public int getCount(){
		try{
			synchronized(underlying_lock){
				if(displayedDataModel == null) return 0;
				
				//update the list only when the GUI thread post a update message.
				if(!isNotificationPostAndSetFalse()) return underlying.size();
				
				try{
					int x = displayedDataModel.getSearchElementCount();
					underlying = new ArrayList<SearchResultElement>();
					for(int i=0; i<x; ++i)
						underlying.add(displayedDataModel.getSearchElementAt(i));
				}
				catch(Exception ex){
					ex.printStackTrace();
					System.err.println("Returning 0 from getCount. Becareful.");
					return 0;
				}	
			}
			if(underlying == null){
				System.err.println("Returning 0 from getCount. Becareful.");
				return 0;
			}
			else return underlying.size();
		}catch(Exception ex){
			error = ex.getMessage();
			return 0;
		}
	}

	@Override
	public SearchResultElement getItem(int i){
		//SearchResultElement se = displayedDataModel.getSearchElementAt(i);
		SearchResultElement se;
		synchronized(underlying_lock){
			try{
				se = underlying.get(i);
			}
			catch(Exception ex){
				se = null;
				System.err.println("Returning null. Becareful.");
			}
		}
		return se;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		View result = null;
		SearchResultElement se = null;
		boolean canReachEnd = false;
		try{
			if(convertView == null){
				//inflate the xml view
				LayoutInflater inflate = (LayoutInflater)
	            		m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				RelativeLayout layout = (RelativeLayout)inflate.inflate(R.layout.search_item, null);
				result = layout;
			}
			else{
				//use the view directly
				result = convertView;
			}
			
			se = getItem(position);
			
			if(se == null) return result;
			
			try{
				ImageView type = (ImageView)result.findViewById(R.id.ImageView01);
				TextView file = (TextView)result.findViewById(R.id.TextView01);
				TextView size = (TextView)result.findViewById(R.id.TextView02);
				TextView speed = (TextView)result.findViewById(R.id.TextView03);
				TextView nhost = (TextView)result.findViewById(R.id.TextView04);
				
				int typex = getType(getRemoteFile(se).getFileExt());
				if(typex == FILETYPE_AUDIO)
					type.setImageResource(R.drawable.music);
				else if(typex == FILETYPE_IMAGE)
					type.setImageResource(R.drawable.picture);
				else if(typex == FILETYPE_ZIP)
					type.setImageResource(R.drawable.pack);
				else if(typex == FILETYPE_VIDEO)
					type.setImageResource(R.drawable.video);
				else /*if(typex == FILETYPE_DOC)*/
					type.setImageResource(R.drawable.document);
				
				
				file.setText(getRemoteFile(se).getDisplayName());
				size.setText(getFileSize(getRemoteFile(se).getFileSizeObject()));
				speed.setText(se.getFormattedHostSpeed2());
				nhost.setText(String.valueOf(se.getValue2(6)));
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
	
			return result;
		}
		catch(Exception ex){
			ex.printStackTrace();
			result = null;
			System.err.println("Error has occured.");
			return null;
		}
		finally{
			if(result == null || se == null)
				System.err.println("Error has occured.");
		}
	}	
	
    private RemoteFile getRemoteFile( Object node )
    {
    	return ((SearchResultElement)node).getSingleRemoteFile();
    }
    
    private String getFileSize(long number){
        return NumberFormatUtils.formatSignificantByteSize2( number ) ;
    }
    
    public static int FILETYPE_AUDIO = 1;
    public static int FILETYPE_IMAGE = 2;
    public static int FILETYPE_ZIP = 3;
    public static int FILETYPE_DOC = 4;
    public static int FILETYPE_VIDEO = 5;
    
    public static int getType(String fn){
    	boolean right = false;
    	
    	String[] type = MediaType.AUDIO_FILE_EXT;
    	for(int i=0; i<type.length; ++i)
    		if(fn.endsWith(type[i]))
    			return FILETYPE_AUDIO;
    	type = MediaType.DOCUMENTS_FILE_EXT;
    	type = MediaType.IMAGE_FILE_EXT;
    	for(int i=0; i<type.length; ++i)
    		if(fn.endsWith(type[i]))
    			return FILETYPE_IMAGE;
    	type = MediaType.PROGRAM_FILE_EXT;
    	for(int i=0; i<type.length; ++i)
    		if(fn.endsWith(type[i]))
    			return FILETYPE_ZIP;
    	type = MediaType.VIDEO_FILE_EXT;
    	for(int i=0; i<type.length; ++i)
    		if(fn.endsWith(type[i]))
    			return FILETYPE_VIDEO;
    	return FILETYPE_DOC;
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
    
    public void setNotificationPost(){
    	synchronized(notification_post){
    		notification_post = true;
    	}
    }
    
    public boolean isNotificationPostAndSetFalse(){
    	synchronized(notification_post){
    		boolean result = notification_post;
    		notification_post = false;
    		return result;
    	}
    }
    
    @Override
    public boolean hasStableIds (){
    	return false;
    }
    
    public void emptyCache(){
    	synchronized(underlying_lock){
    		underlying = null;
    	}
    }
}
