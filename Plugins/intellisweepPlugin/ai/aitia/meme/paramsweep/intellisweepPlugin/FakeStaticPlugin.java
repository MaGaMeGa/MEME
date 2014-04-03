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

import java.util.List;

import javax.swing.JPanel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

/**
 * @author Ferschl
 *
 */
@SuppressWarnings("serial")
public class FakeStaticPlugin implements IIntelliStaticMethodPlugin {

	public boolean alterParameterTree(IIntelliContext ctx) {
		return false;
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		return null;
	}

	public String getDescription() {
		return "This is the base of the order & other fake plugins.";
	}

	public int getMethodType() {
		return IIntelliMethodPlugin.STATIC_METHOD;
	}

	public boolean getReadyStatus() {
		return false;
	}

	public String getReadyStatusDetail() {
		return "This plugin is not implemented yet.";
	}

	public void invalidatePlugin() {
	}

	public boolean isImplemented() {
		return false;
	}

	public void load(IIntelliContext context, Element element)
	        throws WizardLoadingException {
	}

	public void save(Node node) {
	}

	public String getLocalizedName() {
		return "Fake Static Method";
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
		return 0;
	}
}
