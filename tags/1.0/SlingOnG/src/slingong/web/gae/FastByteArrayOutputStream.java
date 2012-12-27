package slingong.web.gae;

import java.io.OutputStream;
import java.util.Arrays;

/**
 * Assuming the usage with accesses only from single thread.
 * ByteArrayOutputStream implementation that doesn't synchronize methods
 * and doesn't copy the data on toByteArray(). 
 * @see http://javatechniques.com/public/java/docs/basics/faster-deep-copy.html
 */
public class FastByteArrayOutputStream extends OutputStream {
	protected byte[] buf = null;
    protected int size = 0;
    
    // Constructors -------------------------------------------------------------------------------
    public FastByteArrayOutputStream() {
        this( 5 * 1024);
    }
    
    public FastByteArrayOutputStream( int initSize) {
        this.size = 0;
        this.buf = new byte[ initSize];
    }
    // --------------------------------------------------------------------------------------------
    
    /**
     * Ensures that we have a large enough buffer for the given size.
     */
    private void verifyBufferSize( int sz) {
        if ( sz > buf.length) {
            byte[] old = buf;
            buf = new byte[ Math.max( sz, 2 * buf.length )];
            System.arraycopy( old, 0, buf, 0, old.length);
            old = null;
        }
    }
    
    public int getSize() {
        return size;
    }    
    
    public byte[] getByteArray() {
        return Arrays.copyOf( buf, size);
    }
    
    public void write( byte b[]) {
        verifyBufferSize( size + b.length);
        System.arraycopy( b, 0, buf, size, b.length);
        size += b.length;
    }

    public void write( byte b[], int off, int len) {
        verifyBufferSize( size + len);
        System.arraycopy( b, off, buf, size, len);
        size += len;
    }

    @Override
    public void write( int b) {
        verifyBufferSize( size + 1);
        buf[size++] = (byte) b;
    }
    
    public void reset() {
        size = 0;
    }
    
    /**
     * Returns a ByteArrayInputStream for reading back the written data
     */
    public FastByteArrayInputStream getInputStream() {
        return new FastByteArrayInputStream( buf, size);
    }
}
