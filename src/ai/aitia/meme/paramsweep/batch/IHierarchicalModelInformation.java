package ai.aitia.meme.paramsweep.batch;

import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public interface IHierarchicalModelInformation extends IModelInformation {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public List<ISubmodelParameterInfo> getSubmodelParameters(final SubmodelInfo<?> submodel);
}