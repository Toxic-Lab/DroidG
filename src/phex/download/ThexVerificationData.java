package phex.download;

import java.util.List;

import phex.thex.TTHashCalcUtils;

public class ThexVerificationData
{
    /**
     * The thex data.
     */
    private ThexData thexData;
    
    /**
     * Indicates if the thex data is currently requested for the associated 
     * download file. 
     */
    private boolean isThexRequested;
    
    private String rootHash;
    
    public ThexVerificationData()
    {
        thexData = null;
        isThexRequested = false;
    }

    public String getRootHash()
    {
        return rootHash;
    }

    public void setRootHash(String rootHash)
    {
        this.rootHash = rootHash;
    }

    public ThexData getThexData()
    {
        return thexData;
    }
    
    public void setThexData( List<byte[]> lowestLevelNodes, int treeDepth, long fileSize )
    {
        thexData = new ThexData( lowestLevelNodes, treeDepth, fileSize );
    }

    public boolean isThexRequested()
    {
        return isThexRequested;
    }

    public void setThexRequested(boolean isThexRequested)
    {
        this.isThexRequested = isThexRequested;
    }
    
    public class ThexData
    {
        private final long fileSize;
        private final List<byte[]> lowestLevelNodes;
        private final int treeDepth;
        private final int nodeSize;
        
        public ThexData( List<byte[]> lowestLevelNodes, int treeDepth, long fileSize )
        {
            this.lowestLevelNodes = lowestLevelNodes; 
            this.treeDepth = treeDepth;
            this.fileSize = fileSize;
            this.nodeSize = TTHashCalcUtils.getTreeNodeSize( fileSize, treeDepth );
        }
        
        public byte[] getNodeHash( int idx )
        {
            return lowestLevelNodes.get( idx );
        }
        
        public int getNodeSize()
        {
            return nodeSize;
        }
        
        /**
         * Returns true if the thex tree has a good quality.
         * @return
         */
        public boolean isGoodQuality()
        {
            return treeDepth >= TTHashCalcUtils.getTreeLevels( fileSize ) - 1;
        }
    }
}
