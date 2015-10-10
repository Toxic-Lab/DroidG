/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package phex.common;

public class FileHandlingException extends Exception
{
    public static final int RENAME_FAILED = 0;
    public static final int FILE_ALREADY_EXISTS = 1;
    public static final int CREATE_FILE_FAILED = 2;

    private int type;
    private String fileName;

    public FileHandlingException( int aType )
    {
        this( aType, null, null );
    }
    
    public FileHandlingException( int aType, Throwable cause )
    {
        this( aType, null, cause );
    }
    
    public FileHandlingException( int aType, String fileName, Throwable cause )
    {
        super( cause );
        type = aType;
        this.fileName = fileName;
    }

    public int getType()
    {
        return type;
    }
    
    public String getFileName()
    {
        return fileName;
    }
    
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer( super.toString() );
        buffer.append( ": " );
        switch ( type )
        {
            case RENAME_FAILED:
                buffer.append( "Rename file failed.");
                break;
            case FILE_ALREADY_EXISTS:
                buffer.append( "File already exists.");
                break;
            case CREATE_FILE_FAILED:
                buffer.append( "Creating file failed.");
                break;
        }
        return buffer.toString();
    }
}