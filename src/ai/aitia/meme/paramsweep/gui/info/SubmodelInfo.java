package ai.aitia.meme.paramsweep.gui.info;

import java.util.ArrayList;
import java.util.List;

public class SubmodelInfo extends ParameterInfo implements ISubmodelGUIInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6758605100283402861L;
	
	private List<Class<?>> possibleTypes;
	private Class<?> actualType;
	private SubmodelInfo parent;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final String name, final String type, final Class<?> javaType, final List<Class<?>> possibleTypes, final SubmodelInfo parent) {
		this(name,null,type,javaType,possibleTypes,parent);
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo(final String name, final String description, final String type, final Class<?> javaType, final List<Class<?>> possibleTypes,
						final SubmodelInfo parent) {
		super(name,description,type,javaType);
		this.possibleTypes = possibleTypes;
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Class<?>> getPossibleTypes() { return possibleTypes; }
	public Class<?> getActualType() { return actualType; }
	public SubmodelInfo getParent() { return parent; }

	//----------------------------------------------------------------------------------------------------
	public void setPossibleTypes(final List<Class<?>> possibleTypes) { this.possibleTypes = possibleTypes; }
	public void setActualType(final Class<?> actualType) { this.actualType = actualType; }
	public void setParent(final SubmodelInfo parent) { this.parent = parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void setInitValue() {
		values.clear();
		super.setInitValue();
		if (values.isEmpty())
			values.add(null);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public SubmodelInfo clone() {
		final List<Class<?>> possibleTypesClone = new ArrayList<Class<?>>(this.possibleTypes);
		final SubmodelInfo clone = new SubmodelInfo(this.name,this.description,this.type,this.actualType,possibleTypesClone,this.parent);
		clone.actualType = this.actualType;
		
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public boolean isSubmodelParameter() { return parent != null; }
}