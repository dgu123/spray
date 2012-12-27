package slingong.web.gae;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Handler class for generating deep copy of serializable object. <br /> 
 * <b>Need to call turnOffCopier method at the end of use of CopierImpl class before thread terminate, 
 * in order to avoid memory leak.</b> <br />
 * Because this works only on static ThreadLocal member fields, even different multiple instances of this 
 * class yield the exact same result with single instance. Didn't make this as singleton and consisted of 
 * all methods as static method, in order to have more scalability by programming to interface and have this 
 * more testable.
 * @author Arata Yamamoto
 */
public class CopierImpl implements Copier {
	
	public static class CopierEngine {
		// About stream objects being used for creating deep copy of object -----------------------
		protected FastByteArrayOutputStream fastByteArrayOutputStream = null; 
		protected ObjectOutputStream objectOutputStream = null; 
		protected FastByteArrayInputStream fastByteArrayInputStream = null;
		protected byte[] streamHeaderByteArray = null;

		/**
		 * Inserts bytes from streamHeaderByteArray (stream header what will be taken into 
		 * ObjectInputStream at construction of ObjectInputStream object) at the top of 
		 * objByteArray input. <br /> 
		 * Thread safety: Not thread safe. 
		 * @param objByteArray
		 * @return byte[] with elements of streamHeaderByteArray inserted at top of objByteArray input. 
		 */
		protected byte[] insertStreamHeader( final byte[] objByteArray) {
			byte[] writtenObj = new byte[ (streamHeaderByteArray.length + objByteArray.length)];
			System.arraycopy( 
					streamHeaderByteArray, 0, writtenObj, 0, streamHeaderByteArray.length);
			System.arraycopy( 
					objByteArray, 0, writtenObj, streamHeaderByteArray.length, objByteArray.length);
			return writtenObj;
		} // protected byte[] insertStreamHeader( final byte[] objByteArray)
		
		/**
		 * Generate deep copy of obj input by serializing and de-serializing. <br />
		 * Thread safety: Not thread safe including necessity of having thread safety on input obj assured.    
		 * @param obj
		 * @return deep copy of obj input.
		 * @throws 
		 */
		public <T extends Object> T generateCopy( final T obj) throws Throwable {
			if ( obj == null) return null;
			
			boolean isUsedObjectOutputStream = true;
			if ( streamHeaderByteArray == null) {
				isUsedObjectOutputStream = false;
				fastByteArrayOutputStream = new FastByteArrayOutputStream();
				objectOutputStream = new ObjectOutputStream( fastByteArrayOutputStream); 
					// throws IOException, SecurityException, NullPointerException
				streamHeaderByteArray = fastByteArrayOutputStream.getByteArray();
			}
			
			try {
				objectOutputStream.writeUnshared( obj);
					// throws NoSerializableException, InvalidClassException, IOException
				objectOutputStream.flush();
					// throws IOException
			}
			catch( Throwable throwable) {
				fastByteArrayOutputStream.reset();
				try {
					objectOutputStream.reset();
				}
				catch( Throwable throwableObj) {
					fastByteArrayOutputStream = null;
					objectOutputStream = null;
					streamHeaderByteArray = null;
				}
				throw throwable;
			}
			
			byte[] objByteArray = fastByteArrayOutputStream.getByteArray();
			
			if ( fastByteArrayInputStream == null) {
				fastByteArrayInputStream = new FastByteArrayInputStream();
				
				if ( isUsedObjectOutputStream)	{
					// Need to insert stream header to top of objByteArray when objectOutputStream has 
					// bean used before without fastByteArrayInputStream.
					objByteArray = insertStreamHeader( objByteArray);
				}
			}
			
			fastByteArrayInputStream.appendBytes( objByteArray);
			
			try {
				ObjectInputStream objectInputStream = fastByteArrayInputStream.getObjectInputStream();
					// throws StreamCorruptedException, IOException
				return (T)objectInputStream.readUnshared();
					// throws ClassNotFoundException, StreamCorruptedException, ObjectStreamException, OptionalDataException, IOException  
			}
			catch( Throwable throwable) {
				fastByteArrayInputStream = null;
				throw throwable;
			}
			finally {
				fastByteArrayOutputStream.reset();
				try {
					objectOutputStream.reset();
					// throws IOException
				}
				catch( Throwable throwable) {
					fastByteArrayOutputStream = null;
					objectOutputStream = null;
					streamHeaderByteArray = null;
					fastByteArrayInputStream = null;
					
					throw throwable;
				}
				if ( fastByteArrayInputStream != null) fastByteArrayInputStream.reset();
			}
		} // public Serializable generateCopy( final Serializable obj) throws Throwable
		
		/**
		 * Serialize obj input to byte array. 
		 * Every returned byte array contains stream header at top for de-serialization. <br />
		 * Thread safety: Not thread safe including thread safety regarding input obj also needs to 
		 * be assured. 
		 * @param obj
		 * @return byte array by serializing obj input.
		 * @throws Throwable
		 */
		public byte[] serialize( final Serializable obj) throws Throwable {
			boolean isUsedObjectOutputStream = true;
			if ( streamHeaderByteArray == null) {
				isUsedObjectOutputStream = false;
				fastByteArrayOutputStream = new FastByteArrayOutputStream();
				objectOutputStream = new ObjectOutputStream( fastByteArrayOutputStream);
					// throws IOException, SecurityException, NullPointerException
				streamHeaderByteArray = fastByteArrayOutputStream.getByteArray();
			}
			
			try {
				objectOutputStream.writeUnshared( obj);
				// throws NoSerializableException, InvalidClassException, IOException
				objectOutputStream.flush();
					// throws IOException
				
				byte[] objByteArray = fastByteArrayOutputStream.getByteArray();
				if ( isUsedObjectOutputStream) {
					objByteArray = insertStreamHeader( objByteArray);
				}
				
				return objByteArray;
			}
			catch( Throwable throwable) {
				throw throwable;
			}
			finally {
				fastByteArrayOutputStream.reset();
				try {
					objectOutputStream.reset();
				}
				catch( Throwable throwable) {
					fastByteArrayOutputStream = null;
					objectOutputStream = null;
					streamHeaderByteArray = null;
					
					throw throwable;
				}
			}
		} // public byte[] serialize( final Serializable obj) throws Throwable
		
		/**
		 * De-serialize byte array to object. 
		 * This is for de-serializing from cached byte array to actual object as value of data entry. <br />
		 * Thread safety: Not thread safe. 
		 * @param writtenObj must contain stream header at top of it.
		 * @return object by de-serializing byte array input. 
		 * @throws Throwable
		 */
		public Object deserialize( final byte[] writtenObj) throws Throwable {
			FastByteArrayInputStream fastByteArrayInputStream 
			= new FastByteArrayInputStream( writtenObj, writtenObj.length);
			ObjectInputStream objectInputStream = fastByteArrayInputStream.getObjectInputStream();
				// throws StreamCorruptedException, IOException
			return objectInputStream.readUnshared();
				// throws ClassNotFoundException, StreamCorruptedException, ObjectStreamException, OptionalDataException, IOException  
		} // public Object deserialize( final byte[] writtenObj)
		// ----------------------------------------------------------------------------------------		
	} // protected static class CopierEngine

	protected static ThreadLocal<Boolean> copierEngineSwitchThreadLocal 
	= new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return new Boolean( false);
		}
	};
		@Override
		public boolean isCopierOn() {
			boolean copierEngineSwitch = CopierImpl.copierEngineSwitchThreadLocal.get();
			if ( !copierEngineSwitch) {
				CopierImpl.copierEngineSwitchThreadLocal.remove();
			}
			return copierEngineSwitch;
		}
	
	protected static ThreadLocal<CopierEngine> copierEngineThreadLocal 
	= new ThreadLocal<CopierEngine>() {
		@Override
		protected CopierEngine initialValue() {
			return new CopierEngine();
		}
	};
	
	protected static CopierEngine getCopierEngine() {
		copierEngineSwitchThreadLocal.set( true);

		return CopierImpl.copierEngineThreadLocal.get();
	}
	
	/**
	 * <b>Need this to be executed at the end of use once per thread on any CopierImpl instance before 
	 * thread terminate, in order to avoid memory leak.</b> 
	 */
	@Override
	public void turnOffCopier() {
		CopierImpl.copierEngineSwitchThreadLocal.remove();
		CopierImpl.copierEngineThreadLocal.remove();
	}
	
	/**
	 * Generate deep copy of obj input by serializing and de-serializing. <br />
	 * Thread safety: conditional thread safe; thread safety of obj input needs to be assured. 
	 * @param obj
	 * @return deep copy of obj input.
	 * @throws 
	 */
	@Override
	public <T extends Object> T generateCopy( final T obj) throws Throwable {
		CopierEngine copierEngine = getCopierEngine();
		try {
			return copierEngine.generateCopy( obj);
		}
		catch( Throwable throwable) {
			turnOffCopier();
			throw throwable;
		}
	}
	
	/**
	 * Serialize obj input to byte array. 
	 * Every returned byte array contains stream header at top for de-serialization. <br />
	 * Thread safety: thread safe. 
	 * @param obj
	 * @return byte array by serializing obj input.
	 * @throws Throwable
	 */
	@Override
	public byte[] serialize( final Serializable obj) throws Throwable {
		CopierEngine copierEngine = getCopierEngine();
		try {
			return copierEngine.serialize( obj);
		}
		catch( Throwable throwable) {
			turnOffCopier();
			throw throwable;
		}
	}
	
	/**
	 * De-serialize byte array to object. <br />
	 * Thread safety: thread safe. 
	 * @param writtenObj must contain stream header at top of it.
	 * @return object by de-serializing byte array input. 
	 * @throws Throwable
	 */
	@Override
	public Object deserialize( final byte[] writtenObj) throws Throwable {
		CopierEngine copierEngine = getCopierEngine();
		try {
			return copierEngine.deserialize( writtenObj);
		}
		catch( Throwable throwable) {
			turnOffCopier();
			throw throwable;
		}
	}
}
