package ai.aitia.meme.paramsweep.batch.param;

public class SubmodelParameterInfo<T> extends ParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -6469952187619533059L;
	
	private final SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final T defaultValue, final boolean originalConstant, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,originalConstant);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final T defaultValue, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final ParameterInfo<T> p, final SubmodelInfo<?> parent) {
		super(p);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
}