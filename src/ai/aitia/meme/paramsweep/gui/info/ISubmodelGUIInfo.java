package ai.aitia.meme.paramsweep.gui.info;

public interface ISubmodelGUIInfo {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public SubmodelInfo getParent();
	public String getName();
	public Class<?> getParentValue();
	public void setParentValue(final Class<?> parentValue);
}
