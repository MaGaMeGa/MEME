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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javassist.CtClass;

import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.utils.ModelFileFilter;

public class RepastGUIController implements IGUIController {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public FileFilter getModelFileFilter() { return new ModelFileFilter(); }
	public ModelDefinition getModelDefinitionType() { return ModelDefinition.JUST_MODEL_FILE; }
	public boolean isClassPathEnabled() { return true; }
	public boolean isModelDirectoryEnabled() { return false; }
	public boolean isModelFileEnabled() { return true; }
	public boolean isParameterFileEnabled() { return true; }
	public boolean isResourcesEnabled() { return true; }
	public RunOption getRunOption() { return RunOption.LOCAL; }
	public boolean isNewParametersEnabled() { return true; }
	public boolean canNewParametersAlsoRecordables() { return false; }
	public JPopupMenu getAddPopupMenu(ParameterSweepWizard wizard) { return null; }
	public boolean hasAddAsOption() { return true; }
	public boolean isRecordConditionEnabled() { return true; }
	public boolean isRecordEndOfTheRunsEnabled() { return true; }
	public boolean isRecordEveryIterationEnabled() { return true; }
	public boolean isRecordNIterationEnabled() { return true; }
	public boolean isScriptingSupport() { return true; }
	public boolean isWriteEndOfTheRunsEnabled() { return true; }
	public boolean isWriteEveryRecordingEnabled() { return true; }
	public boolean isWriteNIterationEnabled() { return true; }
	public WriteMode getDefaultWriteMode() { return WriteMode.RUN; }
	public boolean isStopConditionEnabled() { return true;	}
	public boolean isStopFixIntervalEnabled() {	return true; }
	public void enabledDisabledPopupMenuElements(RecordableElement element) {}
	public boolean isTimeDisplayed() { return true;	}
	
	//----------------------------------------------------------------------------------------------------
	public String calculateOtherBasicModelInformation(ParameterSweepWizard wizard, String original) {
		File f = new File(original);
		CtClass	clazz = null;
		try {
			InputStream ins = new FileInputStream(f);
			clazz = wizard.getClassPool().makeClass(ins);
			ins.close();
			clazz.stopPruning(true);
		} catch (IOException e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} finally {
			if (clazz != null)
				clazz.defrost();
		}
		String packageName = clazz.getName();
		int index = packageName.lastIndexOf('.');
		if (index != -1) 
			packageName = packageName.substring(0,index);
		packageName = packageName.replace('.',File.separatorChar);
		index = original.lastIndexOf(packageName);
		String returnPath = (index == -1) ? original : original.substring(0,index - 1);
		return returnPath;
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
									 (isLocal ? "Tick: " + 1. + "\n" + _event.getText() + "\n" : "");
			case STEP_ENDED : return runStr + (long) lastRun + "/" + (long) maxRun + " (Iteration: " + _event.getIteration() + ")\n" +
									 (isLocal ? "Tick: " + (event.getNumber() + 1) + "\n" + _event.getText() + "\n" : "");
			}
		} else {
			switch (event.getEventType()) {
			case RUN_ENDED :  long actRun = (long) (event.getNumber() == maxRun ? maxRun :  event.getNumber() + 1);
							  return "Run: " + actRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + 1. + "\n" : "");
			case STEP_ENDED : return "Run: " + (long) lastRun + "/" + (long) maxRun + "\n" + (isLocal ? "Tick: " + (event.getNumber() + 1) + "\n" : "");
			}
		}
		return "";
	}
}
