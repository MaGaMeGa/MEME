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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import ai.aitia.meme.paramsweep.gui.info.TimeInfo.Mode;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** This class provides the graphical user interface where the users can define the 
 *  writing time of a recorder.
 */
public class WriteToFileDialog extends JDialog implements ActionListener {
	
	//=================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	/** Return value constant: indicates that the user closes the dialog by pressing Apply button. */
	public static final int APPLY_OPTION = 0;
	/** Return value constant: indicates that the user closes the dialog by pressing Cancel button (or 'x' in the right top corner). */
	public static final int CANCEL_OPTION = -1;
	
	/** The return value of the dialog. */
	private int returnValue = CANCEL_OPTION;
	/** The currently selected writing mode. */
	private WriteMode currentType = null;
	/** The argument of the currently selected writing mode (-1 if there isn't any argument). */
	private long currentArg = -1;
	/** The argument of the recording mode of the current recorder (0 if there isn't any relevant argument). */
	private long recorderArg = 0;
	
	//=================================================================================
	// GUI-members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	private JRadioButton runButton = new JRadioButton("At the end of the runs");
	private JRadioButton tickIntervalButton = new JRadioButton("After");
	private JTextField tickIntervalField = new JTextField();
	private JLabel tickLabel = new JLabel("iterations");
	private JRadioButton recordButton = new JRadioButton("After every recording");
	private JPanel center = null;
	private JButton applyButton = new JButton("Apply");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel bottom = new JPanel();
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param currentType the initial writing mode
	 * @param currentArg the argument of  the initial writing mode
	 */
	public WriteToFileDialog(Frame owner, WriteMode currentType, long currentArg) {
		super(owner,"Advanced recorder settings",true);
		this.setName("dial_writetofile");
		if (isValidInitValue(currentType)) {
			this.currentType = currentType;
			this.currentArg = currentArg;
		} else {
			this.currentType = PlatformSettings.getGUIControllerForPlatform().getDefaultWriteMode();
			this.currentArg = this.currentType == WriteMode.ITERATION_INTERVAL ? 10 : -1;
		}
		layoutGUI();
		initialize();
		this.setLocationRelativeTo(owner);
	}
	
	//--------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param currentType the initial writing mode
	 */
	public WriteToFileDialog(Frame owner, WriteMode currentType) {
		this(owner,currentType,-1);
	}
	
	//--------------------------------------------------------------------------------
	/** Shows the dialog.
	 * @param type the recording mode of the current recorder
	 * @param arg  the argument of the recording mode of the current recorder (0 if there isn't any relevant argument)
	 * @return an int that indicates the closing mode of the dialog
	 */
	public int showDialog(Mode type, long arg) {
		recorderArg = arg;
		initRadioButtons(type);
		setVisible(true);
		int result = returnValue;
		dispose();
		return result;
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the selected writing mode.
	 * @param arg output parameter for the argument of the selected writing mode (if any)
	 */
	public WriteMode getWriteType(long[] arg) {
		arg[0] = currentArg;
		return currentType;
	}
	
	
	//=================================================================================
	// implemented interfaces

	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("RECORD") || command.equals("RUN")) {
			tickIntervalField.setEnabled(false);
			tickIntervalField.setText("");
		} else if (command.equals("TICK_INTERVAL")) {
			tickIntervalField.setEnabled(true);
			tickIntervalField.grabFocus();
		} else if (command.equals("APPLY")) {
			if (checkSettings()) {
				if (recordButton.isSelected())
					currentType = WriteMode.RECORD;
				else if (runButton.isSelected())
					currentType = WriteMode.RUN;
				else
					currentType = WriteMode.ITERATION_INTERVAL;
				returnValue = APPLY_OPTION;
				setVisible(false);
			}
		} else if (command.equals("CANCEL")) {
			returnValue = CANCEL_OPTION;
			setVisible(false);
		}
	}
	
	//=================================================================================
	// GUI-methods

	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		center = FormsUtils.build("p ~ p:g ~ p",
								  "[DialogBorder]000|" +
								  				"111|" +
								  				"234|",
								  recordButton,
								  runButton,
								  tickIntervalButton,tickIntervalField,tickLabel).getPanel();
		
		bottom.add(applyButton);
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
				returnValue = CANCEL_OPTION;
			}
		});
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		infoScr.setPreferredSize(new Dimension(270,70));
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		Utilities.setTextPane(infoPane,Utils.htmlPage("Please define when the selected recorder writes the recorded data into the output file."));
		
		center.setBorder(BorderFactory.createTitledBorder("Write to file"));
		
		GUIUtils.createButtonGroup(recordButton,runButton,tickIntervalButton);
		
		recordButton.setActionCommand("RECORD");
		recordButton.setName("rbtn_wtfdialog_record");
		runButton.setActionCommand("RUN");
		runButton.setName("rbtn_wtfdialog_run");
		tickIntervalButton.setActionCommand("TICK_INTERVAL");
		tickIntervalButton.setName("rbtn_wtfdialog_tick");
		
		tickIntervalField.setName("fld_wtfdialog_tick");
		applyButton.setActionCommand("APPLY");
		applyButton.setName("btn_ok");
		cancelButton.setActionCommand("CANCEL");
		cancelButton.setName("btn_cancel");
		
		GUIUtils.addActionListener(this,recordButton,runButton,tickIntervalButton,
								   applyButton,cancelButton);
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setPreferredSize(new Dimension(270,245));
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										    oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Initializes the radio buttons (each one represents a writing mode option)
	 *  according to the recording mode of the current recorder.
	 */
	private void initRadioButtons(Mode recorderType) {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		recordButton.setEnabled(controller.isWriteEveryRecordingEnabled());
		runButton.setEnabled(controller.isWriteEndOfTheRunsEnabled());
		tickIntervalButton.setEnabled(controller.isWriteNIterationEnabled());
		tickLabel.setEnabled(controller.isWriteNIterationEnabled());
		tickIntervalField.setEnabled(controller.isWriteNIterationEnabled());
		switch (recorderType) {
		case RUN		:
		case CONDITION	: tickIntervalButton.setEnabled(false);
						  tickLabel.setEnabled(false);
						  break;
		}
		
		switch (currentType) {
		case RECORD				: recordButton.setSelected(true);
								  break;
		case RUN				: runButton.setSelected(true);
								  break;
		case ITERATION_INTERVAL	: if (tickIntervalButton.isEnabled()) {
									  tickIntervalButton.setSelected(true);
									  tickIntervalField.setEnabled(true);
									  tickIntervalField.setText(String.valueOf(currentArg));
								  }
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Tests if the selected option is valid. */
	private boolean checkSettings() {
		if (tickIntervalButton.isSelected()) {
			String text = tickIntervalField.getText().trim();
			String error = null;
			long number = -1;
			if ("".equals(text))
			  error = "The selected recording time option is incomplete: empty field.";
			else {
			  try {
				  number = Long.parseLong(text);
				  if (number < 1)
					  throw new NumberFormatException();
			  } catch (NumberFormatException e) {
				  error = "The selected recording time option is wrong: the contain of the field is not a positive integer.";
			  }
			  if (error == null && number < recorderArg)
				  error = "The writing interval value must be greater or equals than the recorder interval value (" + recorderArg + ").";
			}
			if (error != null) {
				Utilities.userAlert(this,error);
				return false;
			} else {
				currentArg = number; // we change here this member
				return true;
			  }
		} else
			return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isValidInitValue(WriteMode value) {
		if ((value == WriteMode.RUN && !PlatformSettings.getGUIControllerForPlatform().isWriteEndOfTheRunsEnabled()) ||
			(value == WriteMode.RECORD && !PlatformSettings.getGUIControllerForPlatform().isWriteEveryRecordingEnabled()) ||
			(value == WriteMode.ITERATION_INTERVAL && !PlatformSettings.getGUIControllerForPlatform().isWriteNIterationEnabled()))
			return false;
		return true;
	}
}
