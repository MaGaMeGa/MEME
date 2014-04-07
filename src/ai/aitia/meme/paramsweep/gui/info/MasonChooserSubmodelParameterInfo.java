package ai.aitia.meme.paramsweep.gui.info;

import java.util.ArrayList;
import java.util.List;

public class MasonChooserSubmodelParameterInfo extends MasonChooserParameterInfo implements ISubmodelGUIInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6494379199798639435L;
	
	protected SubmodelInfo parent;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String type, final Class<?> javaType, final List<Integer> validValues,
											 final List<String> validStringValues, final SubmodelInfo parent) {
		this(name,null,type,javaType,validValues,validStringValues,parent);
	}

	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String description, final String type, final Class<?> javaType,
											 final List<Integer> validValues, final List<String> validStringValues, final SubmodelInfo parent) {
		super(name,description,type,javaType,validValues,validStringValues);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonChooserSubmodelParameterInfo clone() {
		final List<Integer> validValuesClone = new ArrayList<Integer>(this.validValues);
		final List<String> validNamedValuesClone = new ArrayList<String>(this.validStringValues);
		final MasonChooserSubmodelParameterInfo clone = new MasonChooserSubmodelParameterInfo(this.name,this.description,this.javaType,validValuesClone,
																							  validNamedValuesClone,this.parent);
		
		return clone; 
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return true; }
}