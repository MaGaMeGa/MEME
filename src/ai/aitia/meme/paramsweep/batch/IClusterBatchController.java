package ai.aitia.meme.paramsweep.batch;

public interface IClusterBatchController extends IBatchController {

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public void setSkipCount(final long skipCount);
	public void setOneRunOnly(final boolean oneRunOnly);
}