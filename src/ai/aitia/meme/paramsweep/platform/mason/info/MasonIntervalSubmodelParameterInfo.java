package ai.aitia.meme.paramsweep.platform.mason.info;

import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public class MasonIntervalSubmodelParameterInfo<T> extends MasonIntervalParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -5765896717297439205L;
	
	protected SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String description, final T defaultValue, final Number min, final Number max,
											  final boolean isDouble, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,min,max,isDouble);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonIntervalSubmodelParameterInfo<T> clone() {
		return new MasonIntervalSubmodelParameterInfo<T>(this.getName(),this.getDescription(),this.getDefaultValue(),this.min,this.max,this.isDouble,this.parent);
	}
}