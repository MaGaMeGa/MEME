package ai.aitia.meme.paramsweep.gui.info;

public class MasonIntervalSubmodelParameterInfo extends MasonIntervalParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6870852296005661875L;
	
	protected SubmodelInfo parent;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final Number min, final Number max, 
											  final boolean isDoubleInterval, final SubmodelInfo parent) {
		this(name,null,type,javaType,min,max,isDoubleInterval,parent);
	}

	//----------------------------------------------------------------------------------------------------
	public MasonIntervalSubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType, final Number min,
											  final Number max, final boolean isDoubleInterval, final SubmodelInfo parent) {
		super(name,description,type,javaType,min,max,isDoubleInterval);
		this.parent = parent;
	}

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonIntervalSubmodelParameterInfo clone() {
		return new MasonIntervalSubmodelParameterInfo(this.name,this.description,this.type,this.javaType,this.intervalMin,this.intervalMax,this.isDoubleInterval,
													  this.parent); 
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (o instanceof MasonIntervalSubmodelParameterInfo) {
			final MasonIntervalSubmodelParameterInfo that = (MasonIntervalSubmodelParameterInfo) o;
			
			if (this.parent == null)
				return that.parent == null && this.name.equals(that.name);
			
			return this.name.equals(that.name) && this.parent.equals(that.parent);
		}
		
		return false;
	}
}