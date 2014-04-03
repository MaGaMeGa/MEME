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

import java.io.File;

import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.utils.NetLogoModelFileFilter;

public class NetLogoGUIController implements IGUIController {

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public ModelDefinition getModelDefinitionType() { return ModelDefinition.JUST_MODEL_FILE; }
	public FileFilter getModelFileFilter() { return new NetLogoModelFileFilter(); }
	public boolean isClassPathEnabled() { return false;	}
	public boolean isModelFileEnabled() { return true; }
	public boolean isModelDirectoryEnabled() { return false; }
	public boolean isParameterFileEnabled() { return true; }
	public boolean isResourcesEnabled() { return true; }
	public RunOption getRunOption() { return RunOption.GLOBAL; }
	public boolean isNewParametersEnabled() { return true; }
	public boolean canNewParametersAlsoRecordables() { return true; }
	public boolean isRecordConditionEnabled() {	return false; }
	public boolean isRecordEndOfTheRunsEnabled() { return true; }
	public boolean isRecordEveryIterationEnabled() { return true; }
	public boolean isRecordNIterationEnabled() { return false; }
	public boolean isStopConditionEnabled() { return true; }
	public boolean isStopFixIntervalEnabled() { return true; }
	public WriteMode getDefaultWriteMode() { return WriteMode.RUN; }
	public boolean isWriteEndOfTheRunsEnabled() { return true; }
	public boolean isWriteEveryRecordingEnabled() { return false; }
	public boolean isWriteNIterationEnabled() {	return false; }
	public boolean hasAddAsOption() { return false; }
	public void enabledDisabledPopupMenuElements(RecordableElement element) {}
	public JPopupMenu getAddPopupMenu(ParameterSweepWizard wizard) { return null; }
	public boolean isScriptingSupport() { return true; } 
	public boolean isTimeDisplayed() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public String calculateOtherBasicModelInformation(ParameterSweepWizard wizard, String original) {
		File file = new File(original);
		return file.getParentFile().getAbsolutePath();
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("cast")
	public String getProgressInfo(BatchEvent event, boolean isLocal, double lastRun, double maxRun) {
		if (event instanceof IntelliSweepBatchEvent) {
			IntelliSweepBatchEvent _event = (IntelliSweepBatchEvent) event;
			String runStr = isLocal ? "Run: " : "Run (in all iterations): ";
			switch (event.getEventType()) {
			case RUN_ENDED :  long actRun = (long) (event.getNumber() == maxRun ? maxRun :  event.getNumber() + 1);
							  return runStr + (long) actRun + "/" + (long) maxRun + " (Iteration: " + _event.getIteration() + ")\n" +
									 (isLocal ? "Tick: " + 0. + "\n" + _event.getText() + "\n" : "");
			case STEP_ENDED : return runStr + (long) lastRun + "/" + (long) maxRun + " (Iteration: " + _event.getIteration() + ")\n" +
									 (isLocal ? "Tick: " + event.getNumber() + "\n" + _event.getText() + "\n" : "");
			}
		} else {
			switch (event.getEventType()) {
			case RUN_ENDED :  long actRun = (long) (event.getNumber() == maxRun ? maxRun :  event.getNumber() + 1);
							  return "Run: " + actRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + 0. + "\n" : "");
			case STEP_ENDED : return "Run: " + (long) lastRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + event.getNumber() + "\n" : "");
			}
		}
		return "";
	}
}
