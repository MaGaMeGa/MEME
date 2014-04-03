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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.intellisweepPlugin.Static_StandardDOE;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

public abstract class AbstractDoeDisplayer implements IIntelliStaticMethodPlugin,
        IStandardDoeDisplayer, ActionListener {

	protected static final String SWITCH_TO_STANDARD = "SWITCH_TO_STANDARD";
	protected Static_StandardDOE standardPlugin = null;
	protected boolean inStandardPlugin = false;
	protected IIntelliContext context = null;
	protected JPanel standardContent = null;
	protected JPanel thisContent = null;
	protected JPanel content = null;

	public boolean alterParameterTree(IIntelliContext ctx) {
		int result = 1;
		if(inStandardPlugin && standardPlugin.isModifiedDesignTable()){
			result = Utilities.askUser(ParameterSweepWizard.getFrame(), 
					false, 
					"Warning", 
					"You have modified the design runs. This is no longer a(n) " + this.getMethodDisplayName() + " design.",
					"Do you want to continue?");
		}
		if(result == 1){
			if(!inStandardPlugin) createThisDesignInStandard();
			return standardPlugin.alterParameterTree(ctx);
		} else return false;
	}

	public int getMethodType() {
		return IIntelliMethodPlugin.STATIC_METHOD;
	}

	public void actionPerformed(ActionEvent e) {
		String command =  e.getActionCommand();
		if(command.equals(SWITCH_TO_STANDARD)){
			if (getReadyStatus()){
				createThisDesignInStandard();
				inStandardPlugin = true;
				standardPlugin.setRunFromOtherPlugin(true, this);
				standardContent = standardPlugin.getSettingsPanel(context);
				content.remove(thisContent);
				content.add(standardContent, BorderLayout.CENTER);
				content.setVisible(false);
				content.setVisible(true);
			} else{
				Utilities.userAlert(ParameterSweepWizard.getFrame(), getReadyStatusDetail());
			}
		}
    }
	protected abstract void createThisDesignInStandard();

	public abstract String getMethodDisplayName();

	public void returnToOriginalMethod() {
		inStandardPlugin = false;
		content.remove(standardContent);
		content.add(thisContent, BorderLayout.CENTER);
		content.setVisible(false);
		content.setVisible(true);
    }
}
