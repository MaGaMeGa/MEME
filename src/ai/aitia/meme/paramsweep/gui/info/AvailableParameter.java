package ai.aitia.meme.paramsweep.gui.info;


//----------------------------------------------------------------------------------------------------
public class AvailableParameter {
	
	//====================================================================================================
	// members
	
	public final ParameterInfo info;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public AvailableParameter(final ParameterInfo info) {
		if (info == null) 
			throw new IllegalArgumentException("AvailableParameter(): null parameter.");
		
		this.info = info;
		
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		final String humanName = info.getName().replaceAll("([A-Z])", " $1");
		String result = humanName;
		if (info.getValue() != null) {
			if (info instanceof MasonChooserParameterInfo) {
				final MasonChooserParameterInfo mpInfo = (MasonChooserParameterInfo) info;
				result += ": " + mpInfo.getValidStrings()[((Integer)info.getValue())];
			} else
 				result += ": " + info.getValue().toString();
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof AvailableParameter) {
			final AvailableParameter that = (AvailableParameter) obj;
			return this.info.equals(that.info);
		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return info.hashCode();
	}
}