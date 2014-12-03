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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.Popup;
import javax.swing.SpinnerNumberModel;

import org.jgap.Configuration;
import org.jgap.GeneticOperator;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.MutationOperator;

import com.jgoodies.forms.layout.CellConstraints;

import ai.aitia.meme.paramsweep.batch.IModelInformation.ModelInformationException;
import ai.aitia.meme.paramsweep.gui.DescriptionPopupFactory;
import ai.aitia.meme.paramsweep.gui.Page_IntelliExtension;
import ai.aitia.meme.paramsweep.gui.component.DefaultJButton;
import ai.aitia.meme.utils.FormsUtils;

/**
 * @author Tamás Máhr
 *
 */
public class MutationOperatorConfigurator implements IGAOperatorConfigurator {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1633083328605423977L;
	
	private transient JSpinner mutationRateSpinner;
	private SpinnerNumberModel model = new SpinnerNumberModel(0,0,Integer.MAX_VALUE,1);
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public String getName() {
		return "Mutation operator";
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public String getDescription() {
		return "The mutation operator runs through the genes in each of the Chromosomes in the population and mutates them in statistical accordance to the given"
				+ " mutation rate. Mutated Chromosomes are then added to the list of candidate Chromosomes destined for the natural selection process.";
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	public GeneticOperator getConfiguredOperator(final Configuration config) throws InvalidConfigurationException {
		final int mutationRate = (Integer) model.getValue();
		return new MutationOperator(config,mutationRate);
	}
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getSettingsPanel() {
		mutationRateSpinner = new JSpinner(model);

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

		return FormsUtils.build("p ~ p p f:p:g",
				"0123||" +
				"444_", 
				"Mutation rate", new JLabel("1 / "), mutationRateSpinner, infoButton, CellConstraints.RIGHT,
				"(Zero value disables mutation entirely.)").getPanel();
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString(){
		return getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	public Map<String,String> getConfiguration() {
		final Map<String,String> result = new HashMap<String,String>();
		result.put("mutationRate",String.valueOf(model.getValue()));
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setConfiguration(final Map<String,String> configuration) throws ModelInformationException {
		final String mutationRateStr = configuration.get("mutationRate");
		if (mutationRateStr == null)
			throw new ModelInformationException("Missing setting: mutationRate.");
		
		try {
			final int mutationRate = Integer.parseInt(mutationRateStr.trim());
			if (mutationRate < 0)
				throw new NumberFormatException();
			model.setValue(mutationRate);
		} catch (final NumberFormatException e) {
			throw new ModelInformationException("Invalid setting for 'mutationRate': " + mutationRateStr + ".");

		}
	}
}