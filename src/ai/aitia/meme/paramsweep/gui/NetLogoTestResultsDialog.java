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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class NetLogoTestResultsDialog extends JDialog implements ActionListener {
		
	//=================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	private String msg = null;
	private Throwable throwable = null;
	
	//=================================================================================
	// GUI-members
	
	private JPanel content = null;
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	private JButton closeButton = new JButton("Close");
	private JButton detailsButton = new JButton("Details");

	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	public NetLogoTestResultsDialog(Frame owner, String msg, Throwable throwable) {
		super(owner,"Test Data Sources - Results",true);
		this.msg = msg;
		this.throwable = throwable;
		layoutGUI();
		initialize();
		this.setLocationRelativeTo(owner);
	}

	//--------------------------------------------------------------------------------
	public void showDialog() {
		setVisible(true);
		dispose();
	}
	
	//=================================================================================
	// implemented interfaces

	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("CLOSE".equals(command))
			setVisible(false);
		else if ("DETAILS".equals(command)) {
			detailsButton.setEnabled(false);
			infoPane.setText(msg + "\n\n" + Util.stackTrace(throwable));
		}
	}
	
	//=================================================================================
	// GUI-methods

	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		content = FormsUtils.build("p:g ~ p ~ p ~ p:g",
								   "[DialogBorder]0000 f:p:g||" +
								  				 "_12_ p|",
								   infoScr,
								   closeButton,
								   detailsButton).getPanel();
		
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		infoScr.setPreferredSize(new Dimension(590,250)); 
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),BorderFactory.createEmptyBorder(b,b,b,b)));
		infoPane.setText(msg != null ? msg : Util.stackTrace(throwable));
		infoPane.setCaretPosition(0);
		
		closeButton.setActionCommand("CLOSE");
		detailsButton.setActionCommand("DETAILS");
		detailsButton.setEnabled(msg != null && throwable != null);
		
		GUIUtils.addActionListener(this,closeButton,detailsButton);
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
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
}
