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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jgap.Configuration;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelector;
import org.jgap.impl.TournamentSelector;

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
public class TournamentSelectorConfigurator implements IGASelectorConfigurator {
	
	//====================================================================================================
	// members

	private static final long serialVersionUID = -1586909154409773351L;
	
	private transient JSpinner tournamentSizeSpinner;
	private SpinnerNumberModel tournamentSizeModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);

	private transient JSlider selectionProbability;
	private BoundedRangeModel selectionProbabilityModel = new DefaultBoundedRangeModel(50, 1, 0, 100);
	private transient JTextField selectionProbabilityValueField;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "Tournament selector";
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "Plays tournaments to determine the chromosomes to be taken to the next generation. The tournament size can be adjusted as well as the probability"
				+ " for selecting an individual.";
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPanel getSettingsPanel() {
		tournamentSizeSpinner = new JSpinner(tournamentSizeModel);

		selectionProbability = new JSlider(selectionProbabilityModel);
		selectionProbability.setMajorTickSpacing(99);
		selectionProbability.setMinorTickSpacing(10);
		selectionProbability.setPaintTicks(true);
		selectionProbability.setPaintLabels(true);
		selectionProbabilityValueField = new JTextField("50");
		selectionProbabilityValueField.setPreferredSize(new Dimension(40, 30));
		selectionProbability.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (selectionProbability.hasFocus())
					selectionProbabilityValueField.setText(String.valueOf(selectionProbability.getValue()));
			}
		});
		selectionProbabilityValueField.getDocument().addDocumentListener(new DocumentListener() {
			
			//====================================================================================================
			// members
			
			private Popup errorPopup;

			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			@Override
			public void removeUpdate(final DocumentEvent event) {
				textFieldChanged();
			}

			//----------------------------------------------------------------------------------------------------
			@Override
			public void insertUpdate(final DocumentEvent event) {
				textFieldChanged();
			}

			//----------------------------------------------------------------------------------------------------
			@Override
			public void changedUpdate(DocumentEvent event) {
				textFieldChanged();
			}

			//----------------------------------------------------------------------------------------------------
			private void textFieldChanged() {
				if (!selectionProbabilityValueField.hasFocus()) {
					return;
				}

				try {
					int value = Integer.parseInt(selectionProbabilityValueField.getText());
					selectionProbability.setValue(value);

					if (errorPopup != null) {
						errorPopup.hide();
						errorPopup = null;
					}
				} catch (NumberFormatException ex) {
					PopupFactory popupFactory = PopupFactory.getSharedInstance();
					Point locationOnScreen = selectionProbabilityValueField.getLocationOnScreen();
					JLabel message = new JLabel("Please specify an integer number!");
					message.setBorder(new LineBorder(Color.RED, 2, true));
					errorPopup = popupFactory.getPopup(selectionProbabilityValueField, message, locationOnScreen.x - 10, locationOnScreen.y - 30);

					errorPopup.show();
				}
			}
		});

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

		return FormsUtils.build("p ~ p p p p:g", 
								"01112 ||" +
								"3456_",
								new JLabel("Tournament size"), tournamentSizeSpinner, infoButton, CellConstraints.RIGHT,
								new JLabel("Selection probability "), selectionProbabilityValueField, new JLabel("%"), selectionProbability).getPanel();
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return getName();
	}

	//----------------------------------------------------------------------------------------------------
	public NaturalSelector getSelector(final Configuration config) throws InvalidConfigurationException {
		final int size = (Integer) tournamentSizeModel.getValue();
		final double probability = selectionProbabilityModel.getValue() / 100.0;
		final TournamentSelector selector = new TournamentSelector(config, size, probability);

		return selector;
	}

	//----------------------------------------------------------------------------------------------------
	public Map<String,String> getConfiguration() {
		final Map<String,String> result = new HashMap<String,String>();
		result.put("tournamentSize", String.valueOf(tournamentSizeModel.getValue()));
		result.put("selectionProbability", String.valueOf(selectionProbabilityModel.getValue()));
		
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public void setConfiguration(final Map<String,String> configuration) throws ModelInformationException {
		final String tournamentSizeStr = configuration.get("tournamentSize");
		if (tournamentSizeStr == null)
			throw new ModelInformationException("Missing setting: tournamentSize.");

		try {
			final int tournamentSize = Integer.parseInt(tournamentSizeStr.trim());
			if (tournamentSize < 1)
				throw new NumberFormatException();
			tournamentSizeModel.setValue(tournamentSize);
		} catch (final NumberFormatException e) {
			throw new ModelInformationException("Invalid setting for 'tournamentSize': "
					+ tournamentSizeStr + ".");
		}

		final String selectionProbabilityStr = configuration.get("selectionProbability");
		if (selectionProbabilityStr == null)
			throw new ModelInformationException("Missing setting: selectionProbability.");

		try {
			final int selectionProbabilityNumber = Integer.parseInt(selectionProbabilityStr.trim());
			if (selectionProbabilityNumber < 1
					|| selectionProbabilityNumber > 100)
				throw new NumberFormatException();
			selectionProbabilityModel.setValue(selectionProbabilityNumber);
		} catch (final NumberFormatException e) {
			throw new ModelInformationException("Invalid setting for 'selectionProbability': "
					+ selectionProbabilityStr + ".");
		}
	}
}