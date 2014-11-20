package ai.aitia.meme.paramsweep.gui.info;

//----------------------------------------------------------------------------------------------------
public class ParameterInATree extends AvailableParameter {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ParameterInATree(final ParameterInfo info) {
		super(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String result = info.toString();
		final int idx = result.indexOf(":");
		if (idx > 0) {
			final String humanName = result.substring(0,idx).replaceAll("([A-Z])", " $1");
			result = humanName + result.substring(idx);
		}
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ParameterInATree) {
			super.equals(obj);
		}
		
		return false;
	}
}