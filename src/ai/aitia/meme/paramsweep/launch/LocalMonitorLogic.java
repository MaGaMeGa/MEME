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
package ai.aitia.meme.paramsweep.launch;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.gui.lop.IProgressUpdate;
import ai.aitia.meme.gui.lop.Progress;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.intellisweep.IIntelliSweepBatchListener;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.gui.MonitorGUI;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Mean;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.CancelImportException;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.utils.Utils;

public class LocalMonitorLogic implements IMonitorListener,
										  IIntelliSweepBatchListener,
										  IProgressUpdate {
	
	//====================================================================================================
	// members
	
	public static final long DAY = 24 * 3600 * 1000L;
	public static final java.text.DateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm:ss");
	static {
		timeFmt.setTimeZone(new java.util.SimpleTimeZone(0,"noDST"));
	}

	private IBatchController controller = null;
	private MonitorGUI gui = null;
	private Frame owner = null;
	
	private String[] outputFiles = null;
	private boolean interrupted = false;
	private boolean ended = false;
	private PrintStream sout = null;
	private double stepCounter = 0, allStep = 0; 
	private Mean<Double> stepNumber = new Mean<Double>(10);
	private boolean fromMEME = false;
	private IEBinary<String[],PlatformType> callback = null;
	private Progress prg = null;
	private volatile long startFR, endFR;
	private String settingsFile = null;
	private boolean isIntelliMethod = false;
	
	private double lastRun = 1;
	private boolean dynamicIntelliMethod = false;
	
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public LocalMonitorLogic(Frame owner, IBatchController controller, String simulationName, String details, String[] outputFiles, boolean fromMEME,
							 IPanelManager panelManager, IEBinary<String[],PlatformType> callback, String settingsFile, boolean isIntelliMethod,
							 boolean dynamicIntelliMethod) {
		this.owner = owner;
		this.controller = controller;
		this.controller.addBatchListener(this);
		this.fromMEME = fromMEME;
		this.gui = new MonitorGUI(owner,fromMEME,panelManager,true);
		this.gui.addMonitorListener(this);
		this.callback = callback;
		this.settingsFile = settingsFile;
		this.outputFiles = outputFiles;
		this.isIntelliMethod = isIntelliMethod;
		this.dynamicIntelliMethod = dynamicIntelliMethod;
		initializeMonitorGUI(simulationName,details);
	}
	
	//--------------------------------------------------------------------------------
	public void startMonitor() {
		sout = System.out;
		try {
			File tempFile = File.createTempFile("sout",null);
			tempFile.deleteOnExit();
			System.setOut(new PrintStream(tempFile));
		} catch (FileNotFoundException e) {
			// never happens
			throw new IllegalStateException(e);
		} catch (IOException e) {
			System.setOut(sout);
		} 
		startFR = System.currentTimeMillis();
		if (prg == null) {
			prg = new Progress();
			prg.setListener(this);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public MonitorGUI getGUI() { return gui; }
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	// these are valid event only in the case of remote simulation running
	public void downloadSimulationResultSignal(MonitorEvent event) {} 
	public void importSimulationResultSignal(MonitorEvent event) {} 
	public void killSimulationSignal(MonitorEvent event) {} 
	public void reconnectSignal(MonitorEvent event) {} 
	public void removeSimulationSignal(MonitorEvent event) {} 
	public void updateSimulationListSignal(MonitorEvent event) {} 

	//----------------------------------------------------------------------------------------------------
	public void stopCurrentRunSignal(MonitorEvent event) {
		try {
			controller.stopCurrentRun();
		} catch (BatchException e) {
			ParameterSweepWizard.logError("Error: stop current run");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			Utilities.userAlert(owner,"Error: operation cannot be executed","Reason: " + Util.getLocalizedMessage(e));
			gui.setEnabledInterruptButton(false);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void stopSimulationSignal(MonitorEvent event) {
		if (!ended) {
			try {
				interrupted = true;
				controller.stopBatch();
				System.setOut(sout);
			} catch (BatchException e) {
				ParameterSweepWizard.logError("Error: stop simulation");
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				Utilities.userAlert(owner,"Error: operation cannot be executed","Reason: " + Util.getLocalizedMessage(e));
				gui.setEnabledHardStopButton(false);
			}
		}
	}
	
	//====================================================================================================
	
	//----------------------------------------------------------------------------------------------------
	// only in the case of remote simulation running
	public void hardCancelled(BatchEvent event) {}
	public void softCancelled(BatchEvent event) {}
	
	//----------------------------------------------------------------------------------------------------
	public void batchEnded(BatchEvent event) {
		if (interrupted) {
			String msg = "Batch interrupted at run " + (long) lastRun;
			if (dynamicIntelliMethod) {
				IntelliSweepBatchEvent _event = (IntelliSweepBatchEvent) event;
				msg += " (Iteration: " + _event.getIteration() + ")";
			}
			msg += ".";
			gui.setProgress(msg);
			
		} else
			gui.setProgress("Batch done.");
		ended = true;
		System.setOut(sout);
		setLeftTimeToZero();
		gui.setEnabledInterruptButton(false);
		gui.setEnabledHardStopButton(false);
		if (fromMEME) {
				gui.getPanelManager().remove(gui);
				if (callback != null) {
					if (outputFiles == null || outputFiles.length == 0) 
						Utilities.userAlert(owner,"Automatic result importation is impossible because of missing output file(s) information.",
						"To import results to the database of MEME, please use the Import command.");
					else{
						if (isIntelliMethod){
							String[] temp = new String[outputFiles.length + 1];
							for (int i = 0; i < outputFiles.length;i++) 
								temp[i] = outputFiles[i];
							temp[outputFiles.length] = settingsFile;
							try {
								callback.run(temp,PlatformSettings.getPlatformType());
								Utilities.userAlert(owner,"Importing is done.");
							} catch (CancelImportException e) {
							} catch (Exception e) {
								ParameterSweepWizard.logError("Error at automatic importing");
								e.printStackTrace(ParameterSweepWizard.getLogStream());
								Utilities.userAlert(owner,"Error at automatic importing.",Util.getLocalizedMessage(e));
							}
						} else { 
							try {
								callback.run(outputFiles,PlatformSettings.getPlatformType());
								Utilities.userAlert(owner,"Importing is done.");
							} catch (CancelImportException e) {
							} catch (Exception e) {
								ParameterSweepWizard.logError("Error at automatic importing");
								e.printStackTrace(ParameterSweepWizard.getLogStream());
								Utilities.userAlert(owner,"Error at automatic importing.",Util.getLocalizedMessage(e));
							}
						}
					}
				} else 
					Utilities.userAlert(owner,"Automatic result importation is impossible because of missing import plugin.");
		} else 
			gui.setCloseButtonText("Close");
	}

	//----------------------------------------------------------------------------------------------------
	public void runChanged(BatchEvent event) {
		if (!interrupted) {
			stepNumber.add(stepCounter);
			stepCounter = 0;
			if (stepNumber.size() == 1) {
				endFR = System.currentTimeMillis();
				if (prg != null)
					prg.restart(controller.getNumberOfRuns() * stepNumber.mean());
			}
			gui.setProgress(PlatformSettings.getGUIControllerForPlatform().getProgressInfo(event,true,lastRun,controller.getNumberOfRuns()));
			lastRun = event.getNumber() + 1;
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void timeProgressed(BatchEvent event) {
		allStep++;
		int numberOfRuns = controller.getNumberOfRuns();
		gui.setProgress(PlatformSettings.getGUIControllerForPlatform().getProgressInfo(event,true,lastRun,numberOfRuns));
		stepCounter++;
		if (prg != null && !stepNumber.empty() && numberOfRuns > 0) 
			prg.update(allStep,numberOfRuns * stepNumber.mean());
		else 
			gui.setTime("Elapsed time: " + elapsedTime(System.currentTimeMillis() - startFR + 1));
	}
	
	//----------------------------------------------------------------------------------------------------
	public void iterationChanged(IntelliSweepBatchEvent event) {
		lastRun = 1;
		allStep = stepCounter = 0;
		stepNumber.clear();
	}

	//====================================================================================================

	//----------------------------------------------------------------------------------------------------
	public void onProgressUpdate(double percent, long elapsed, String left) {
		String newTime = null;
		long l = elapsed + endFR - startFR + 1;
		newTime = "Elapsed time: " + elapsedTime(l) + "\n";
		if (!left.equals("")) {
			newTime += "Estimated time left";
			if (dynamicIntelliMethod) 
				newTime += " in the current iteration";
			newTime += ": " + left;
		}
		gui.setTime(newTime);
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String elapsedTime(long duration) {
		String dayPrefix = "";
		if (duration >= DAY)
			dayPrefix = String.format("%d day(s), ",duration / DAY);
		return dayPrefix + timeFmt.format(new Date(duration));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeMonitorGUI(String simulationName, String details) {
		String name = Util.restoreNameAndTimeStamp(simulationName);
		gui.setSimulationName(name);
		String _details = "";
		if (details != null && details.length() > 0) { 
			_details = details;
			if (outputFiles != null)
				_details += "\n";
		}
		if (outputFiles != null)
			_details += "Result file(s):\n" + Utils.join(", ",(Object[])outputFiles);
		gui.setDetails(_details);
		gui.setProgress("Deploying model");
		gui.setTime("Elapsed time: 00:00:00");
		if (dynamicIntelliMethod)
			gui.setEnabledInterruptButton(false);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setLeftTimeToZero() {
		String original = gui.getTime();
		int idx = original.lastIndexOf("left: ");
		if (idx > -1) {
			original = original.substring(0,idx + 6) + "00:00:00";
			gui.setTime(original);
		}
	}
}
