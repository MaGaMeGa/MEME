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
package ai.aitia.meme.paramsweep.plugin;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

@SuppressWarnings("serial")
public class TestIntelliPlugin implements IIntelliStaticMethodPlugin {

	public boolean alterParameterTree(IIntelliContext ctx) {
		return false;
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("This is for testing."),BorderLayout.CENTER);
		return panel;
	}

	public String getDescription() {
		return null;
	}

	public int getMethodType() {
		return IIntelliMethodPlugin.STATIC_METHOD;
	}

	public boolean getReadyStatus() {
		return false;
	}

	public String getReadyStatusDetail() {
		return "It's not doing anything, so why would you Finish?";
	}

	public void invalidatePlugin() {
	}

	public boolean isImplemented() {
		return true;
	}

	public void load(IIntelliContext context, Element element)
	        throws WizardLoadingException {
	}

	public void save(Node node) {
	}

	public String getLocalizedName() {
		return "Ez nem k�ls� jarban van";
	}

	public int getBlockingVariableValueCountsFor(int blockingVariableCount) {
		return 0;
	}

	public int getMaxBlockingVariables() {
		return 0;
	}

	public boolean isBlockingHelpSupported() {
		return false;
	}

	public List<Object> getBlockingVariableValues(RngSeedManipulatorModel.BlockingParameterInfo info) {
		return null;
	}

	public int getDesignSize() { return 0; }

	public long getNumberOfRuns() {
		return 1;
	}

}
