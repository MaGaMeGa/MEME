package ai.aitia.meme.paramsweep.gui.info;

public class SubmodelParameterInfo extends ParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6991443589897247037L;
	
	protected SubmodelInfo parent;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final SubmodelInfo parent) {
		this(name,null,type,javaType,parent);
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType, final SubmodelInfo parent) {
		super(name,description,type,javaType);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	public void setParent(final SubmodelInfo parent) { this.parent = parent; }

	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelParameterInfo clone() {
		final SubmodelParameterInfo clone = new SubmodelParameterInfo(this.name,this.description,this.type,this.javaType,this.parent);
		
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
}