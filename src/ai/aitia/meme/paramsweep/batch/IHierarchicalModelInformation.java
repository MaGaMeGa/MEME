package ai.aitia.meme.paramsweep.batch;

import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public interface IHierarchicalModelInformation extends IModelInformation {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public List<ParameterInfo<?>> getSubmodelParameters(final SubmodelInfo<?> submodel) throws ModelInformationException;
}