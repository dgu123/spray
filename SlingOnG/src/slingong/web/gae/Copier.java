package slingong.web.gae;

import java.io.Serializable;

public interface Copier {

	public boolean isCopierOn();

	/**
	 * <b>Need this to be executed at the end of use once per thread on any Copier instance before 
	 * thread terminate, in order to avoid memory leak.</b> 
	 */
	public void turnOffCopier();

	/**
	 * Generate deep copy of obj input by serializing and de-serializing. <br />
	 * Thread safety: thread safe. Lock on obj input.
	 * @param obj
	 * @return deep copy of obj input.
	 * @throws 
	 */
	public <T extends Object> T generateCopy( final T obj) throws Throwable;

	/**
	 * Serialize obj input to byte array. 
	 * Every returned byte array contains stream header at top for de-serialization. <br />
	 * Thread safety: thread safe. 
	 * @param obj
	 * @return byte array by serializing obj input.
	 * @throws Throwable
	 */
	public byte[] serialize( final Serializable obj) throws Throwable;

	/**
	 * De-serialize byte array to object. <br />
	 * Thread safety: thread safe. 
	 * @param writtenObj must contain stream header at top of it.
	 * @return object by de-serializing byte array input. 
	 * @throws Throwable
	 */
	public Object deserialize( final byte[] writtenObj) throws Throwable;

}