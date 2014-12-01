/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Popup;

import org.jgap.Configuration;
import org.jgap.GeneticOperator;
import org.jgap.InvalidConfigurationException;

import ai.aitia.meme.paramsweep.gui.DescriptionPopupFactory;
import ai.aitia.meme.paramsweep.gui.Page_IntelliExtension;
import ai.aitia.meme.paramsweep.gui.component.DefaultJButton;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.operator.GeneAveragingCrossoverOperator;
import ai.aitia.meme.utils.FormsUtils;

import com.jgoodies.forms.layout.CellConstraints;

/**
 * @author Tamás Máhr
 * 
 */
public class GeneAveragingCrossoverOperatorConfigurator implements IGAOperatorConfigurator {
	
	//====================================================================================================
	// member
	
	private static final long serialVersionUID = 6126059924386476085L;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "Gene averaging crossover operator";
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "The crossover operator randomly selects two Chromosomes from the population and 'mates' them by averaging the values of the numerical genes. The new Chromosome with the average gene values is then added to the list of candidate Chromosomes.";
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPanel getSettingsPanel() {
		final JButton infoButton = new DefaultJButton();
		infoButton.setIcon(Page_IntelliExtension.DESCRIPTION_ICON);
		infoButton.setBorder(null);
		infoButton.setBorderPainted(false);
		infoButton.setContentAreaFilled(false);
		infoButton.setFocusPainted(false);
		
		infoButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				final DescriptionPopupFactory popupFactory = DescriptionPopupFactory.getInstance();
				final Popup parameterDescriptionPopup = popupFactory.getPopup(infoButton, getDescription());
				
				parameterDescriptionPopup.show();
			}
			
		});

		return FormsUtils.build("p:g",
				"0", 
				infoButton, CellConstraints.RIGHT).getPanel();
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	public GeneticOperator getConfiguredOperator(final Configuration config) throws InvalidConfigurationException {
		final GeneAveragingCrossoverOperator operator = new GeneAveragingCrossoverOperator(config, 1.);

		return operator;
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return getName();
	}

	//----------------------------------------------------------------------------------------------------
	public Map<String,String> getConfiguration() {
		return Collections.<String,String>emptyMap();
	}

	//----------------------------------------------------------------------------------------------------
	public void setConfiguration(final Map<String,String> configuration) {}
}