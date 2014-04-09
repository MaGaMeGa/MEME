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
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object o) {
		if (o instanceof MasonChooserSubmodelParameterInfo) {
			final MasonChooserSubmodelParameterInfo that = (MasonChooserSubmodelParameterInfo) o;
			
			if (this.parent == null)
				return that.parent == null && this.name.equals(that.name);
			
			return this.name.equals(that.name) && this.parent.equals(that.parent);
		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + name.hashCode();
		if (parent != null)
			result = 31 * result + parent.hashCode();
		
		return result;
	}
}