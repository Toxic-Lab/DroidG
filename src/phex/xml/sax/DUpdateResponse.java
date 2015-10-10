package phex.xml.sax;

import java.util.ArrayList;
import java.util.List;

public class DUpdateResponse
{
    private List<VersionType> versionList;

    private List<InfoType> infoList;

    public List<VersionType> getVersionList()
    {
        if ( versionList == null )
        {
            versionList = new ArrayList<VersionType>();
        }
        return versionList;
    }

    public List<InfoType> getInfoList()
    {
        if ( infoList == null )
        {
            infoList = new ArrayList<InfoType>();
        }
        return infoList;
    }

    public static class InfoType
    {
        private String text;
        private String header;
        private String id;

        public String getText()
        {
            return text;
        }

        public void setText( String value )
        {
            text = value;
        }

        public String getHeader()
        {
            return header;
        }

        public void setHeader( String value )
        {
            header = value;
        }

        public String getId()
        {
            return id;
        }

        public void setId( String value )
        {
            id = value;
        }
    }

    public static class VersionType
    {
        private boolean hasBeta;
        private boolean beta;
        private String text;
        private String id;

        public boolean isBeta()
        {
            return beta;
        }

        public void setBeta( boolean value )
        {
            beta = value;
            hasBeta = true;
        }

        public String getText()
        {
            return text;
        }

        public void setText( String value )
        {
            text = value;
        }

        public String getId()
        {
            return id;
        }

        public void setId( String value )
        {
            id = value;
        }
    }
}
