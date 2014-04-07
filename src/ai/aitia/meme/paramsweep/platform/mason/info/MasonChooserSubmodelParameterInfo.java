package ai.aitia.meme.paramsweep.platform.mason.info;

import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;

public class MasonChooserSubmodelParameterInfo<T> extends MasonChooserParameterInfo<T> implements ISubmodelParameterInfo {
	
	//====================================================================================================
	// members

	private static final long serialVersionUID = -2727591278171724847L;
	
	protected SubmodelInfo<?> parent;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonChooserSubmodelParameterInfo(final String name, final String description, final T defaultValue, final List<T> possibleValues,
											 final List<String> possibleNamedValues, final SubmodelInfo<?> parent) {
		super(name,description,defaultValue,possibleValues,possibleNamedValues);
		this.parent = parent;
	}
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo<?> getParentInfo() { return parent; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public MasonChooserSubmodelParameterInfo<T> clone() {
		final List<T> possibleValuesClone = new ArrayList<T>(this.possibleValues);
		final List<String> possibleNamedValuesClone = new ArrayList<String>(this.possibleNamedValues);
		final MasonChooserSubmodelParameterInfo<T> clone = new MasonChooserSubmodelParameterInfo<T>(this.getName(),this.getDescription(),this.getDefaultValue(),
																									possibleValuesClone,possibleNamedValuesClone,this.parent);
		
		return clone; 
	}
}