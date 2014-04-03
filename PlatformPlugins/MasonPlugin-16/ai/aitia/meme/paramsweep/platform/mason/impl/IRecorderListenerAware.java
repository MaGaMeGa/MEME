/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.impl;

/**
 * Classes implementing this interface should handle {@link MasonRecorderListener} instances. They have to make sure that the listeners are notified
 * when recording happens.
 * 
 * @author Tamás Máhr
 * 
 */
public interface IRecorderListenerAware {

	/**
	 * Registers the provided listener at the {@link MasonRecorder} objects that exists in the generated model.
	 * 
	 * @param listener the listener to be registered at the recorders.
	 */
	public void addRecorderListener(final MasonRecorderListener listener);
	
	/**
	 * Unregisters the provided listener at the {@link MasonRecorder} objects that exists in the generated model.
	 * 
	 * @param listener the listere to be unregistered.
	 */
	public void removeRecorderListener(final MasonRecorderListener listener);
}
