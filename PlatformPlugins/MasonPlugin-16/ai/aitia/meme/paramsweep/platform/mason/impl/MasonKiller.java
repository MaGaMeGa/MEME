/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.impl;

import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * @author Tamás Máhr
 *
 */
public class MasonKiller implements Steppable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** {@inheritDoc} 
	 */
	@Override
	public void step(SimState state) {
		state.kill();
	}

}
