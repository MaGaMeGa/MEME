/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.paramsweep.internal.platform;

import javax.swing.filechooser.FileFilter;

import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.utils.MasonModelFileFilter;


public class MasonGUIController extends CustomJavaGUIController {
	@Override public FileFilter getModelFileFilter() { return new MasonModelFileFilter(); }
	
	@Override public RunOption getRunOption() { return RunOption.NONE; };
	@Override public boolean isNewParametersEnabled() { return false; };

	@SuppressWarnings("incomplete-switch")
	public String getProgressInfo(BatchEvent event, boolean isLocal, double lastRun, double maxRun) {
		if (event instanceof IntelliSweepBatchEvent) {
			IntelliSweepBatchEvent _event = (IntelliSweepBatchEvent) event;
			String runStr = isLocal ? "Run: " : "Run (in all iterations): ";
			switch (event.getEventType()) {
			case RUN_ENDED :  long actRun = (long) event.getNumber();
							  return runStr + actRun + "/" + (long) maxRun + " (Iteration: " + _event.getIteration() + ")\n" +
									 (isLocal ? "Tick: " + 1. + "\n" + _event.getText() + "\n" : "");
			case STEP_ENDED : return runStr + (long) lastRun + "/" + (long) maxRun + " (Iteration: " + _event.getIteration() + ")\n" +
									 (isLocal ? "Tick: " + (event.getNumber()) + "\n" + _event.getText() + "\n" : "");
			}
		} else {
			switch (event.getEventType()) {
			case RUN_ENDED :  long actRun = (long) event.getNumber();
							  return "Run: " + actRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + 1. + "\n" : "");
			case STEP_ENDED : return "Run: " + (long) lastRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + (event.getNumber()) + "\n" : "");
			}
		}
		return "";
	}

}
