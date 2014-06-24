/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.impl;

import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * This class is used to schedule recording in mason models.
 * 
 * @author Tamás Máhr
 *
 */
public class MasonStepEnded implements Steppable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	IMasonGeneratedModel model;
	
	/**
	 * 
	 */
	public MasonStepEnded(final IMasonGeneratedModel model) {
		this.model = model;
	}

	/** {@inheritDoc} 
	 */
	@Override
	public void step(SimState arg0) {
		model.stepEnded();
	}

}
