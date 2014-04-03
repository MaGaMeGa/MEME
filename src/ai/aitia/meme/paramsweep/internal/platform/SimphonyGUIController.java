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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.RepastSMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.RepastSRecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.platform.simphony.impl.info.AggrType;
import ai.aitia.meme.paramsweep.utils.ScoreFileFilter;

public class SimphonyGUIController implements IGUIController {
	
	//====================================================================================================
	// members
	
	private AggrAction geometricMeanAggrAction = null;
	private AggrAction kurtosisAggrAction = null;
	private AggrAction maxAggrAction = null; 
	private AggrAction meanAggrAction = null;
	private AggrAction minAggrAction = null;
	private AggrAction skewnessAggrAction = null;
	private AggrAction standardDeviationAggrAction = null;
	private AggrAction sumAggrAction = null;
	private AggrAction sumsqAggrAction = null;
	private AggrAction varianceAggrAction = null;
	
	//=====================================================================================
	//implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public boolean isModelDirectoryEnabled() { return true; }
	public boolean isModelFileEnabled() { return true; }
	public boolean isClassPathEnabled() { return true; }
	public boolean isParameterFileEnabled() { return false; } // TODO: ???
	public boolean isResourcesEnabled() { return false; } // TODO: ???
	
	//----------------------------------------------------------------------------------------------------
	public FileFilter getModelFileFilter() { return new ScoreFileFilter(); }
	public ModelDefinition getModelDefinitionType() { return ModelDefinition.BOTH_MODEL_FILE_AND_DIRECTORY; }
	public String calculateOtherBasicModelInformation(ParameterSweepWizard wizard, String original){ return null; }
	
	//----------------------------------------------------------------------------------------------------
	public RunOption getRunOption(){ return RunOption.GLOBAL; }
	public boolean isNewParametersEnabled(){ return false; }
	public boolean canNewParametersAlsoRecordables() { return false; }
	
	//----------------------------------------------------------------------------------------------------
	public boolean isStopFixIntervalEnabled(){ return true; }
	public boolean isStopConditionEnabled(){ return false; }
	public boolean isRecordEveryIterationEnabled() { return true; } 
	public boolean isRecordNIterationEnabled() { return true; } 
	public boolean isRecordEndOfTheRunsEnabled() { return true; } 
	public boolean isRecordConditionEnabled() { return false; } 
	public boolean isWriteEveryRecordingEnabled(){ return false; }  
	public boolean isWriteEndOfTheRunsEnabled(){ return true; } 
	public boolean isWriteNIterationEnabled(){ return false; }
	public WriteMode getDefaultWriteMode(){	return WriteMode.RUN; }
	public boolean isScriptingSupport(){ return false; } // TODO: gyan�tom, hogy ez ideiglenes 
	public boolean hasAddAsOption() { return false; }
	public boolean isTimeDisplayed(){ return true; }
	
	//----------------------------------------------------------------------------------------------------
	public JPopupMenu getAddPopupMenu(ParameterSweepWizard wizard) {
		if (wizard.getRecordersPage().getSelectedRecordableTab() > 1) {
			wizard.getRecordersPage().getRecordableVariablesList().clearSelection();
			wizard.getRecordersPage().getRecordableMethodsList().clearSelection();
			return null;
		} else {
			if (geometricMeanAggrAction == null) {
				geometricMeanAggrAction = new AggrAction(wizard,AggrType.GEOMETRIC_MEAN);
				kurtosisAggrAction = new AggrAction(wizard,AggrType.KURTOSIS);
				maxAggrAction = new AggrAction(wizard,AggrType.MAX);
				meanAggrAction = new AggrAction(wizard,AggrType.MEAN);
				minAggrAction = new AggrAction(wizard,AggrType.MIN);
				skewnessAggrAction = new AggrAction(wizard,AggrType.SKEWNESS);
				standardDeviationAggrAction = new AggrAction(wizard,AggrType.STANDARD_DEVIATION);
				sumAggrAction = new AggrAction(wizard,AggrType.SUM);
				sumsqAggrAction = new AggrAction(wizard,AggrType.SUMSQ);
				varianceAggrAction = new AggrAction(wizard,AggrType.VARIANCE);
			}
			
			JPopupMenu popup = new JPopupMenu();
			popup.add(new AggrAction(wizard,AggrType.NONE));
			popup.addSeparator();
			popup.add(new AggrAction(wizard,AggrType.COUNT));
			popup.add(geometricMeanAggrAction);
			popup.add(kurtosisAggrAction);
			popup.add(maxAggrAction);
			popup.add(meanAggrAction);
			popup.add(minAggrAction);
			popup.add(skewnessAggrAction);
			popup.add(standardDeviationAggrAction);
			popup.add(sumAggrAction);
			popup.add(sumsqAggrAction);
			popup.add(varianceAggrAction);
			return popup;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void enabledDisabledPopupMenuElements(RecordableElement element) {
		RepastSMemberInfo mi = (RepastSMemberInfo) element.getInfo();
		boolean enabled = mi.getJavaType().equals(Integer.class) || mi.getJavaType().equals(Integer.TYPE) || mi.getJavaType().equals(Long.class) ||
			  			  mi.getJavaType().equals(Long.TYPE) ||	mi.getJavaType().equals(Double.class) || mi.getJavaType().equals(Double.TYPE) ||
			  			  mi.getJavaType().equals(Float.class) || mi.getJavaType().equals(Float.TYPE) || mi.getJavaType().equals(Short.class) ||
			  			  mi.getJavaType().equals(Short.TYPE) || mi.getJavaType().equals(Byte.class) || mi.getJavaType().equals(Byte.TYPE);
		geometricMeanAggrAction.setEnabled(enabled);
		kurtosisAggrAction.setEnabled(enabled);
		maxAggrAction.setEnabled(enabled);
		meanAggrAction.setEnabled(enabled);
		minAggrAction.setEnabled(enabled);
		skewnessAggrAction.setEnabled(enabled);
		standardDeviationAggrAction.setEnabled(enabled);
		sumAggrAction.setEnabled(enabled);
		sumsqAggrAction.setEnabled(enabled);
		varianceAggrAction.setEnabled(enabled);
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
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	/** Add aggregate operation when using Simphony. */
	@SuppressWarnings("serial")
	public static class AggrAction extends AbstractAction {
		
		//====================================================================================================
		// members
		
		private ParameterSweepWizard wizard = null;
		private AggrType type = null;
		
		//====================================================================================================
		// methods
		  
		//----------------------------------------------------------------------------------------------------
		public AggrAction(ParameterSweepWizard wizard, AggrType type) {
		   super(RepastSRecordableElement.toString(type));
		   this.wizard = wizard;
		   this.type = type;
  	    }
		
		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			JTree recorderTree = wizard.getRecordersPage().getRecorderTree();
			DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorderTree.getSelectionPath().getLastPathComponent();
			
			while (!(node.getUserObject() instanceof String)) {
				if (node.equals(treeModel.getRoot())) return;
				node = (DefaultMutableTreeNode) node.getParent();
			}
			
			JList recordableVariables = wizard.getRecordersPage().getRecordableVariablesList();
			DefaultListModel listModel = (DefaultListModel) recordableVariables.getModel();
			int[] indices = recordableVariables.getSelectedIndices();
				
			if (indices.length > 0 && wizard.getRecordersPage().getSelectedRecordableTab() == 0) {
				RepastSRecordableElement orig_rsre = (RepastSRecordableElement) listModel.get(indices[0]);
				RepastSRecordableElement rsre = new RepastSRecordableElement(orig_rsre.getInfo(),type);
				if (type != AggrType.NONE) {
					String name = rsre.getInfo().getFieldName() != null ? rsre.getInfo().getFieldName() : rsre.getInfo().getName();
					rsre.setAlias(RepastSRecordableElement.toString(type) + " of " + name);
				}
				treeModel.insertNodeInto(new DefaultMutableTreeNode(rsre),node,node.getChildCount());
  		  	}
			
			JList recordableMethods = wizard.getRecordersPage().getRecordableMethodsList();
			listModel = (DefaultListModel) recordableMethods.getModel();
			indices = recordableMethods.getSelectedIndices();
			  
			if (indices.length > 0 && wizard.getRecordersPage().getSelectedRecordableTab() == 1) {
				RepastSRecordableElement orig_rsre = (RepastSRecordableElement) listModel.get(indices[0]);
				RepastSRecordableElement rsre = new RepastSRecordableElement(orig_rsre.getInfo(),type);
				if (type != AggrType.NONE) {
					String name = rsre.getInfo().getFieldName() != null ? rsre.getInfo().getFieldName() : rsre.getInfo().getName();
					rsre.setAlias(RepastSRecordableElement.toString(type) + " of " + name);
				}
				treeModel.insertNodeInto(new DefaultMutableTreeNode(rsre),node,node.getChildCount());
			}
			  
			wizard.getRecordersPage().clearAllSelection();
			wizard.enableDisableButtons();
		}
	}
}
