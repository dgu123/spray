package slingong.web.gae;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

/**
 * Assuming the usage with accesses only from single thread.
 * ByteArrayInputStream implementation that does not synchronize methods.
 * @see http://javatechniques.com/public/java/docs/basics/faster-deep-copy.html
 */
public class FastByteArrayInputStream extends InputStream {
	protected byte[] buf = null;

	/**
     * Number of bytes that we can read from the buffer
     */
    protected int count = 0;
    /**
     * Number of bytes that have been read from the buffer
     */
    protected int pos = 0;

    // Constructors -------------------------------------------------------------------------------
    public FastByteArrayInputStream( byte[] buf, int count) {
    	if ( ( buf != null) && ( count > 0)) {
    		if ( buf.length >= count) {
                this.buf = buf;
                this.count = count;
    		}
    	}
    }    
    public FastByteArrayInputStream() {
    }
    // --------------------------------------------------------------------------------------------
    
    public int available() {
        return count - pos;
    }
    
    public synchronized void appendBytes( byte[] buf) {
    	if ( buf == null) return;
    	
    	if ( this.buf == null) {
			this.buf = buf;
			pos = 0;
			count = buf.length;
    	}
    	else {
    		if (pos >= count) {
    			this.buf = buf;
    			pos = 0;
    			count = buf.length;
    		}
    		else {
        		int newCount = count - pos + buf.length;
    			byte[] byteArray = new byte[ newCount];
    			
    			int numOfBytes = available();
        		System.arraycopy( this.buf, pos, byteArray, 0, numOfBytes);
        		System.arraycopy( buf, 0, byteArray, numOfBytes, buf.length);
    			
        		pos = 0;
        		count = newCount;
    		}
    	}
	}
    
    @Override
    public int read() throws IOException {
        return (pos < count) ? (buf[ pos++] & 0xff) : -1;
    }
    
    public int read( byte[] b, int off, int len) {
        if (pos >= count) return -1;

        if ( (pos + len) > count) len = (count - pos);

        System.arraycopy( buf, pos, b, off, len);
        pos += len;
        return len;
    }

    public long skip( long n) {
    	if ( n < 0) return 0;
    	
        if ((pos + n) > count) n = count - pos;
        if (n < 0) return 0;
        pos += n;
        return n;
    }    
    
	@Override
	public synchronized void reset() throws IOException {
		pos = 0;
		count = 0;
	}

	protected ObjectInputStream objectInputStream = null;
		public final ObjectInputStream getObjectInputStream() 
		throws StreamCorruptedException, IOException {
			if ( objectInputStream != null) return objectInputStream;
			
			if ( buf == null) return null;
			
			objectInputStream = new ObjectInputStream( this);
				// throws StreamCorruptedException, SecurityException, IOException, NullPointerException
			return objectInputStream;
		}
}
