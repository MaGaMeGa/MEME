/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.impl;

import java.util.Map;

/**
 * @author Tamás Máhr
 *
 */
public interface MasonRecorderListener {
	void recordingPerformed(Map<String, Object> parameters, Map<String, Object> values);
}
