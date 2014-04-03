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

import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;

public interface IGUIController {
	
	public static enum ModelDefinition {
		JUST_MODEL_FILE,
		JUST_MODEL_DIRECTORY,
		BOTH_MODEL_FILE_AND_DIRECTORY
	}
	
	public static enum RunOption {
		NONE,
		GLOBAL,
		LOCAL
	}
	
	public boolean isModelDirectoryEnabled();
	public boolean isModelFileEnabled();
	public boolean isClassPathEnabled();
	public boolean isParameterFileEnabled();
	public boolean isResourcesEnabled();
	
	public FileFilter getModelFileFilter();
	public ModelDefinition getModelDefinitionType();
	// if getModelDefinitionType() == JUST_MODEL_FILE returns the model directory
	// if getModelDefinitionType() == JUST_MODEL_DIRECTORY returns the model file
	// otherwise returns null
	public String calculateOtherBasicModelInformation(ParameterSweepWizard wizard, String original);
	
	public RunOption getRunOption();
	public boolean isNewParametersEnabled();
	public boolean canNewParametersAlsoRecordables();
	
	public boolean isStopFixIntervalEnabled();
	public boolean isStopConditionEnabled();
	public boolean isRecordEveryIterationEnabled();
	public boolean isRecordNIterationEnabled();
	public boolean isRecordEndOfTheRunsEnabled();
	public boolean isRecordConditionEnabled();
	public boolean isWriteEveryRecordingEnabled();
	public boolean isWriteEndOfTheRunsEnabled();
	public boolean isWriteNIterationEnabled();
	public WriteMode getDefaultWriteMode();
	public boolean isScriptingSupport();
	public boolean hasAddAsOption();
	public JPopupMenu getAddPopupMenu(ParameterSweepWizard wizard);
	public void enabledDisabledPopupMenuElements(RecordableElement element);
	
	public boolean isTimeDisplayed();
	public String getProgressInfo(BatchEvent event, boolean isLocal, double lastRun, double maxRun);
}
