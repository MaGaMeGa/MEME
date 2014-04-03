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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;

public class VCloudConnectionDialog extends JDialog implements ActionListener {

	//====================================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	public static final int OK = 0;
	public static final int CANCEL = 1;
	public static final int ERROR = 2;
	
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
	private JTextField hostField = new JTextField();
	private JTextField portField = new JTextField();
	private JPanel bottom = new JPanel();
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public VCloudConnectionDialog(final Frame owner, final boolean cancel) {
		super(owner,"Connection settings",true);
		this.owner = owner;
		layoutGUI(cancel);
		initialize(cancel);
	}
	
	//----------------------------------------------------------------------------------------------------
	public int showDialog(final String hostname, final int port) {
		if (hostname != null)
			hostField.setText(hostname);
		if (port >= 1024 && port < 65535)
			portField.setText(String.valueOf(port));
		setVisible(true);
		return returnValue;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getHostname() { return hostField.getText().trim(); }
	public int getPort() { return Integer.parseInt(portField.getText().trim()); }
	
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
		if ("CANCEL".equals(command)) {
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
	private void layoutGUI(final boolean cancel) {
		
		center = FormsUtils.build("p ~ p:g",
				   				  "[DialogBorder]01||" +
				   				  				"23||",
				                  "Hostname: ",hostField,
				                  "Port: ",portField).getPanel();

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
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				returnValue = cancel ? CANCEL : ERROR;
				setVisible(false);
			}
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize(final boolean cancel) {
		infoScr.setBorder(null);
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		infoText = Utils.htmlPage(DEFAULT_TEXT);
		Utilities.setTextPane(infoPane,infoText);
		infoPane.setPreferredSize(new Dimension(500,50));
		
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		cancelButton.setEnabled(cancel);
		
		GUIUtils.addActionListener(this,okButton,cancelButton);
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setPreferredSize(new Dimension(500,200));
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
		this.setLocationRelativeTo(owner);
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
		final List<String> errors = new ArrayList<String>(); 

		if ("".equals(hostField.getText().trim()))
			errors.add("Field 'Hostname' cannot be empty.");
		final String portStr = portField.getText().trim();
		if ("".equals(portStr))
			errors.add("Field 'Port' cannot be empty.");
		else {
			try {
				final int portNumberTmp = Integer.parseInt(portStr);
				if (portNumberTmp < 1024 || portNumberTmp > 65535)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				errors.add("Port number must be an integer between 1024 and 65535.");
			}
		}
		return errors;	
	}
}
