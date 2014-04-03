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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

public class Dynamic_GeneticAlgANTS implements IIntelliDynamicMethodPlugin {

	private static final long serialVersionUID = 5873974861309045276L;
	public boolean alterParameterTree(IIntelliContext ctx) { return true; }
	public String[] getSelectedVarNames() {	return null; }
	public JPanel getSettingsPanel(IIntelliContext ctx) { return null; }
	public boolean noMoreIteration() { return false; }
	public int getCurrentIteration() { return 0; }
	public ParameterTree getNextParameterTree(IParameterSweepResultReader reader) { return null; }
	public void setRecordableVariables(DefaultMutableTreeNode root) {}
	public String settingsOK(DefaultMutableTreeNode recorders) { return null; }

	public String getDescription() {
		return "With this plugin you can test a model's structure and robustness via a " +
				"simple, automatic, nonlinear search algorithm designed to actively \"break\" " +
				"the model's implications. \n" +
				"Using the active nonlinear tests (ANTs) with the " +
				"Genetic algorithm (GA), one can easily probe for key weaknesses in a " +
				"simulation's structure, and thereby begin to improve and refine the model's design.\n\n" +
				"(Active Nonlinear Tests (ANTs) of Complex Simulation Models, John H. Miller, 1996)\n\n" +
				"If you want this method enabled, please contact us at our website: http://mass.aitia.ai \nThank you.";
	}

	public int getMethodType() { return IIntelliMethodPlugin.DYNAMIC_METHOD; }
	public boolean getReadyStatus() { return false; }
	public String getReadyStatusDetail() { return "This plugin is not implemented yet";	}
	public void invalidatePlugin() {}
	public boolean isImplemented() { return false; }
	public void load(IIntelliContext context, Element element) throws WizardLoadingException {}
	public void save(Node node) {}
	public String getLocalizedName() { return "Adaptive Nonlinear Tests/Genetic Algorithm"; }
	public void setParameterTreeRoot(DefaultMutableTreeNode root) {}
	public int getNumberOfIterations() { return 0; }
}
