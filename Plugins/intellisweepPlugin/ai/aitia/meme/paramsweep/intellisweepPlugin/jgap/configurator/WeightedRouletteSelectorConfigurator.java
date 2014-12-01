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
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelector;
import org.jgap.impl.WeightedRouletteSelector;

import ai.aitia.meme.paramsweep.gui.DescriptionPopupFactory;
import ai.aitia.meme.paramsweep.gui.Page_IntelliExtension;
import ai.aitia.meme.paramsweep.gui.component.DefaultJButton;
import ai.aitia.meme.utils.FormsUtils;

import com.jgoodies.forms.layout.CellConstraints;

/**
 * @author Tamás Máhr
 * 
 */
public class WeightedRouletteSelectorConfigurator implements IGASelectorConfigurator {


	//====================================================================================================
	// members

	private static final long serialVersionUID = 5335744229036369293L;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "Weighted roulette selector";
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "This selector models a roulette wheel. When a Chromosome is added, it gets a number of 'slots' on the wheel equal to its fitness value. When the "
				+ "select method is invoked, the wheel is 'spun' and the Chromosome occupying the spot on which it lands is selected. Then the wheel is spun again "
				+ "and again until the requested number of Chromosomes have been selected. Since Chromosomes with higher fitness values get more slots on the wheel,"
				+ " there's a higher statistical probability that they'll be chosen, but it's not guaranteed.";
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
	@Override
	public String toString() {
		return getName();
	}

	//----------------------------------------------------------------------------------------------------
	public NaturalSelector getSelector(final Configuration config) throws InvalidConfigurationException {
		final WeightedRouletteSelector selector = new WeightedRouletteSelector(config);

		return selector;
	}

	//----------------------------------------------------------------------------------------------------
	public Map<String,String> getConfiguration() {
		return Collections.<String,String>emptyMap();
	}

	//----------------------------------------------------------------------------------------------------
	public void setConfiguration(final Map<String,String> configuration) {}
}