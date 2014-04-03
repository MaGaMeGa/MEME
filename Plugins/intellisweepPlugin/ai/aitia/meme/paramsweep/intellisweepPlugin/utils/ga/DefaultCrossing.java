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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga;

import java.io.Serializable;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class DefaultCrossing implements IGAOperator, Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = 5057839219261005249L;
	protected transient JPanel settingsPanel = null;
	protected transient JLabel comboProbLabel = 
						new JLabel( "Combination probability of genes: " );

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		// TODO Auto-generated method stub
		
	}

	public String getDescription() {
		return "Default crossing";
	}

	public String getName() {
		return "Default crossing";
	}

	public JPanel getSettingspanel() {
		// TODO Auto-generated method stub
		if( settingsPanel == null ){
			settingsPanel = new JPanel();
			settingsPanel.add( new JLabel( "Default crossing settings" ) );
		}
		return settingsPanel;
	}

	public String saveSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPackageName() {
		// TODO Auto-generated method stub
		return null;
	}

}
