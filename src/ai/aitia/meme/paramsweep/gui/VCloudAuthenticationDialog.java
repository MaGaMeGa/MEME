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
package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.Page_Network.WorkersNumberState;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.Utils.Pair;

public class VCloudAuthenticationDialog extends JDialog implements ActionListener {

	//====================================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	public static final int OK = 0;
	public static final int CANCEL = 1;
	
	private static final int PROBLEM_TIMEOUT = 5 * 1000;
	
	private static final String DEFAULT_TEXT = "Please specify the following settings.";  
	
	public static final int MESSAGE	= 0;
	public static final int WARNING	= 1;
	
	private String infoText = null;
	private Pair<String,Integer> warning = null;
	private Utils.TimerHandle warningTimer = null;

	private final Frame owner;
	private int returnValue = 0;

	private JPanel content = new JPanel(new BorderLayout());
	private JPanel center = null;
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	private JTextField userField = new JTextField();
	private JPasswordField passwordField = new JPasswordField();
	private JRadioButton leasedMode = new JRadioButton("Leased mode");
	private JRadioButton bestEffortMode = new JRadioButton("Best effort mode");
	private JLabel workersLabel = new JLabel("Number of requested workers: ");
	private JSpinner workersNumber = new JSpinner();
	private JPanel leasedPanel = null;
	private JPanel bottom = new JPanel();
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public VCloudAuthenticationDialog(final Frame owner) {
		super(owner,"Authentication",true);
		this.owner = owner;
		layoutGUI();
		initialize();
	}
	
	//----------------------------------------------------------------------------------------------------
	public int showDialog() {
		setVisible(true);
		return returnValue;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getUsername() { return userField.getText().trim(); }
	public char[] getPassword() { return passwordField.getPassword(); }
	public boolean isLeasedMode() { return leasedMode.isSelected(); }
	public boolean isBestEffortMode() { return bestEffortMode.isSelected(); }
	public int getNumberOfRequestedWorkers() { return leasedMode.isSelected() ? ((Number)workersNumber.getValue()).intValue() : 0; }
	
	//------------------------------------------------------------------------------------
	public String getInfoText()	{
		String s;
		if (warning != null) {
			s = warning.getSecond().intValue() == WARNING ? "<img src=\"gui/icons/warning.png\">&nbsp;&nbsp;" : ""; 
			s += Utils.htmlQuote(warning.getFirst());
		} else
			s = infoText;
		return s == null ? null : Utils.htmlPage(s);
	}
	
	//------------------------------------------------------------------------------------
	public void setInfoText(final String infoText) { this.infoText = infoText; }
	
	//------------------------------------------------------------------------------------
	public boolean warning(final boolean condition, final String message, final int level, final boolean clear) {
		final String before = warning == null ? null : warning.getFirst();
		warning = condition ?  new Pair<String,Integer>(message,level) : null;
		if (warning != null && !Utils.equals(warning.getFirst(),before)) {
			updateInfo();
			if (warningTimer != null)
				warningTimer.stop();
			if (warning != null && clear)
				warningTimer = Utils.invokeAfter(PROBLEM_TIMEOUT,new Runnable() {
					public void run() { clearProblemText(); }
 				});
		}
		return condition;
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		final String command = e.getActionCommand();
		if ("LEASED".equals(command)) {
			workersLabel.setEnabled(true);
			workersNumber.setEnabled(true);
			workersNumber.setValue(1);
		} else if ("BEST_EFFORT".equals(command)) {
			workersLabel.setEnabled(false);
			workersNumber.setEnabled(false);
			workersNumber.setValue(0);
		} else if ("CANCEL".equals(command)) {
			returnValue = CANCEL;
			setVisible(false);
		} else if ("OK".equals(command)) {
			final List<String> errors = checkFields();
			if (errors.size() > 0) 
				warning(true,Utils.join(errors,"\n"),WARNING,true); 
			else {
				returnValue = OK;
				setVisible(false);
			}
		}
	}
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		leasedPanel = FormsUtils.build("p ~ p ~ p:g(0.2) ~ p:g",
				   					   "_01_|",
				   					   workersLabel,workersNumber).getPanel();
		
		center = FormsUtils.build("p ~ p ~ p:g",
				   				  "[DialogBorder]011||" +
				   				  				"233||" +
				                                "456||" +
				                                "777||" +
				                                "888|",
				                  "(Registrated) E-mail address: ",userField,
				                  "Password: ",passwordField,
				                  "Run option: ",leasedMode,bestEffortMode,
				                  new Separator("Leased mode settings"),
				                  leasedPanel).getPanel();

		bottom.add(okButton);
		bottom.add(cancelButton);

		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		content.add(center,BorderLayout.CENTER);
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(bottom);
		content.add(tmp,BorderLayout.SOUTH);
		
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				returnValue = CANCEL;
			}
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		infoText = Utils.htmlPage(DEFAULT_TEXT);
		Utilities.setTextPane(infoPane,infoText);
		infoPane.setPreferredSize(new Dimension(500,75));
		
		leasedMode.setActionCommand("LEASED");
		bestEffortMode.setActionCommand("BEST_EFFORT");
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		
		workersNumber.setModel(new SpinnerNumberModel(1,1,100000,1));
		
		GUIUtils.createButtonGroup(leasedMode,bestEffortMode);
		GUIUtils.addActionListener(this,leasedMode,bestEffortMode,okButton,cancelButton);
		initializeWidgets();
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setPreferredSize(new Dimension(500,285)); 
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth() + 10, 
										    oldD.height + sp.getHorizontalScrollBar().getHeight() + 10));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
		this.setLocationRelativeTo(owner);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeWidgets() {
		userField.setText(ParameterSweepWizard.getPreferences().getVCloudUserName());
		passwordField.setText(ParameterSweepWizard.getPreferences().getVCloudPassword());
		if (ParameterSweepWizard.getPreferences().isLeasedService()) {
			leasedMode.setSelected(true);
			workersLabel.setEnabled(true);
			workersNumber.setEnabled(true);
			workersNumber.setValue(ParameterSweepWizard.getPreferences().getNumberOfRequestedVCloudWorkers());
		} else {
			bestEffortMode.setSelected(true);
			workersLabel.setEnabled(false);
			workersNumber.setEnabled(false);
			workersNumber.setValue(0);
		}
	}

	//====================================================================================================
	// assistant methods
	
	//------------------------------------------------------------------------------------
	/** Updates the content of the information panel. */
	private void updateInfo() {
		String s = getInfoText();
		Utilities.setTextPane(infoPane, s == null ? "" : s);
	}
	
	//------------------------------------------------------------------------------------
	/** Clears the warning/message from the information panel and updates its content. */
	private void clearProblemText() {
		warning = null;
		if (warningTimer != null) {
			warningTimer.stop();
			warningTimer = null;
		}
		updateInfo();
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> checkFields() {
		List<String> errors = new ArrayList<String>(); 

		if ("".equals(userField.getText().trim()))
			errors.add("Field '(Registrated) E-mail address' cannot be empty.");
		if (passwordField.getPassword().length == 0)
			errors.add("Field 'Password' cannot be empty.");
		if (leasedMode.isSelected()) {
			final WorkersNumberState state = checkNoOfWorkers();
			if (state == WorkersNumberState.INVALID || state == WorkersNumberState.NONPOSITIVE)
				errors.add("The number of requested workers is invalid.");
			else if (state == WorkersNumberState.NOT_4_MULTIPLE)
				errors.add("The number of requested workers must be divisible by 4 because of technical reasons");
		}
		return errors;	
	}
	
	//----------------------------------------------------------------------------------------------------
	private WorkersNumberState checkNoOfWorkers() {
		try {
			int workers = (Integer) workersNumber.getValue();
			if (workers <= 0) 
				return WorkersNumberState.NONPOSITIVE;
//			else if (workers % 4 > 0)
//				return WorkersNumberState.NOT_4_MULTIPLE;
			return WorkersNumberState.ACCEPTABLE;
		} catch (final ClassCastException _) {
			return WorkersNumberState.INVALID;
		}
	}
}
