package slingong.web.gae;

import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.NonTransactionModeEvent;

public interface TransactionalMemcacheServiceTransactionHelper {
	public void clearTransactionResiduals();
	public boolean hasClearAllInvokedInTransaction();
	public boolean hasMemcacheClearedForClearAllInvocation();
	public boolean isTransactionModeThread();
	public void setMemcacheClearedForClearAllInvocation();
	public void clearAllInTransaction();
	public NonTransactionModeEvent getNonTransactionModeEvent();
	public void switchThreadToNonTransactionMode( boolean saveTransactionChanges);
	public void switchThreadToTransactionMode();
}
