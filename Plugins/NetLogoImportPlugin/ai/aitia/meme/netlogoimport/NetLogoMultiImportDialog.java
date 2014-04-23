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
package ai.aitia.meme.netlogoimport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.paramsweep.utils.SettingsFileFilter;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class NetLogoMultiImportDialog extends JDialog implements ActionListener {

	//=============================================================================
	// members
	
	private static final long serialVersionUID = 9019708705855255389L;
	private boolean	scheduleReset = false;
	private IImportPluginContext ctx = null;
	private File[] files = null;
	
	//=============================================================================
	// GUI members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane messagePane = new JTextPane();
	private JScrollPane messageScrollPane = new JScrollPane(messagePane,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
															JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JRadioButton batchButton = new JRadioButton(" Every file a new batch ",true);
	private JRadioButton versionButton = new JRadioButton(" Every file a new version ");
	private JTextField modelField = new JTextField();
	private JTextField versionField = new JTextField();
	private JLabel statusLabel = new JLabel(" (Invalid!) ");
	private JCheckBox include0thTickCheckBox = new JCheckBox("Include 0th ticks (initial states)",true);
	private JCheckBox intelliCheckBox = new JCheckBox("IntelliSweep results",false);
	private JLabel intelliLabel = new JLabel(" IntelliSweep settings XML:");
	private JTextField intelliSettingsFile = new JTextField();
	private JButton intelliBrowseButton = new JButton("...");
	private JPanel mainPanel = FormsUtils.build("p ~ p:g ~ p ~ p:g ~ p",
												"[DialogBorder]00000 p||" +
															  "12345||" +
															  "66666||" +
															  "77777||" +
															  "8899A||" +
															  "BBBBB||" +
															  "CCDD_",
															  messageScrollPane,
															  " Model name: ",modelField," Version: ",versionField,statusLabel,
															  include0thTickCheckBox,
															  intelliCheckBox,
															  intelliLabel,intelliSettingsFile,intelliBrowseButton,
															  new Separator("Import strategy"),
															  batchButton,versionButton).getPanel();
	private JButton okButton = new JButton("Ok");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel buttonsPanel = FormsUtils.build("p:g p ~ p p:g",
												   "[DialogBorder]_01_",
												   okButton,cancelButton).getPanel();
	
	//=============================================================================
	// methods
	
	//-----------------------------------------------------------------------------
	public NetLogoMultiImportDialog(IImportPluginContext ctx) {
		super((ctx != null ? ctx.getAppWindow() : null),"NetLogo Import Settings",true);
		this.ctx = ctx;
		initialize();
	}
	
	//------------------------------------------------------------------------
	public void start(File[] files) {
		if (files == null || files.length < 2)
			throw new IllegalArgumentException();
		this.files = files;
		this.setVisible(true);
	}
	
	//=============================================================================
	// implemented interfaces

	//-----------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("CANCEL")) {
			ctx.put("EXIT_CODE",cancelButton.getText().trim().equals("Close") ? "OK" : "CANCEL");
			System.gc();
			this.setVisible(false);
		} else if (command.equals("BATCH_BUTTON")) {
			if (versionField.getText().endsWith("%")) {
				messagePane.setText("");
				String text = versionField.getText().substring(0,versionField.getText().length() - 1);
				versionField.setText(text);
				modelVersionChanged(false);
			}
		} else if (command.equals("VERSION_BUTTON")) {
			if (versionField.getText().indexOf('%') == -1) {
				messagePane.setText("In the version field there is a % character. This " +
									"character will be replaced by the zero-based index " +
									"of the actual file.");		
				String text = versionField.getText() + "%";
				versionField.setText(text);
				modelVersionChanged(false);
			}
		} else if (command.equals("OK")) {
			int errorNumber = 0;
			messagePane.setText("");
			GUIUtils.setBusy(this,true);
			for (int i = 0;i < files.length;++i) {
				System.gc();
				String text = messagePane.getText();
				if (!text.equals(""))
					text += "\n";
				text += String.format("Completed %d out of %d. Actual file: %s",i,files.length,files[i].getName());
				messagePane.setText(text);
				// bug fix #1478
				final BufferedNetLogoResultParser parser = new BufferedNetLogoResultParser(files[i]);
//				final NetLogoResultParser parser = new NetLogoResultParser(files[i]);
				Throwable t = null;
				try {
					t = (Throwable) MEMEApp.LONG_OPERATION.execute("Loading...",new Callable<Object>() {
						public Object call() {
							try {
								parser.readFile();
								return null;
							} catch (Exception e) {
								return e;
							}
						}
					});
				} catch (Exception e1) {};
				text = messagePane.getText();
				if (t != null) {
					text += "\nERROR: " + t.getLocalizedMessage() + "\nIgnoring file: " + files[i].getName();
					messagePane.setText(text);
					errorNumber += 1;
					continue;
				}
				if (0 < parser.warnings.length())
					messagePane.setText(text + "\nWARNINGS:\n" + parser.warnings);
				try {
					final int nr = i;
					t = (Throwable) MEMEApp.LONG_OPERATION.execute("Writing to database...",new Callable<Object>() {
						String model = modelField.getText().trim();
						String version = batchButton.isSelected() ? versionField.getText().trim() 
																  : versionField.getText().trim().replace("%",String.valueOf(nr));
						
						public Object call() {
							try {
								String isPluginXML = intelliSettingsFile.getText().trim();
								// bug fix #1478
								if (intelliCheckBox.isSelected() && new File(isPluginXML).exists())
									parser.writeIntelliSweepResults(ctx.getResultsDb(),isPluginXML,model,version,null,include0thTickCheckBox.isSelected()); 
//									parser.writeIntelliSweepResults(ctx.getResultsDb(),isPluginXML,model,version,include0thTickCheckBox.isSelected());
								else
									parser.write(ctx.getResultsDb(),model,version,null,include0thTickCheckBox.isSelected());
								return null;
							} catch (Exception e) {
								return e;
							}
						}
					});
					if (t != null) {
						Logger.logException("NetLogoMultiImportDialog.okButton",t);
						text = messagePane.getText();
						text += "\nError while writing to database: " + Utils.getLocalizedMessage(t);
						text += "\nFile: " + files[i].getName();
						messagePane.setText(text);
						errorNumber += 1;
					}
				} catch (Exception e1) {}
				
			}
			GUIUtils.setBusy(this,false);
			String text = messagePane.getText();
			text += String.format("\nCompleted %d out of %d. Done.",files.length,files.length);
			if (errorNumber != 0)
				text += String.format("\nNumber of unparseable files: %d",errorNumber);
			messagePane.setText(text);
			okButton.setEnabled(false);
			batchButton.setEnabled(false);
			versionButton.setEnabled(false);
			modelField.setEnabled(false);
			versionField.setEnabled(false);
			cancelButton.setText("Close");
			cancelButton.grabFocus();
		} else if (command.equals("INTELLI_CHECKBOX"))
			enableDisableIntelliSweepPart();
		else if (command.equals("INTELLI_BROWSE")) {
			JFileChooser chooser = new JFileChooser(MEMEApp.getLastDir());
			chooser.addChoosableFileFilter(new SettingsFileFilter());
			chooser.setMultiSelectionEnabled(false);
			int returnVal = chooser.showOpenDialog(ctx.getAppWindow());
			if (returnVal == JFileChooser.APPROVE_OPTION) 
				intelliSettingsFile.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//-----------------------------------------------------------------------------
	private void initialize() {
		
		// message pane
		messagePane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		messagePane.setFont(new Font("Dialog",Font.BOLD,12));
		messagePane.setEditable(false);
		messagePane.setPreferredSize(new Dimension(400,70));
		messagePane.setMaximumSize(new Dimension(500,70));
		
		// message scrollpane
		messageScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		messageScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(8,15,8,15));
		messageScrollPane.setBackground(messagePane.getBackground());
		messageScrollPane.setPreferredSize(new Dimension(450,100));
		
		// model name and version
		modelVersionBindings(modelField);
		modelVersionBindings(versionField);
		new VersionTextFieldFilter().install(versionField);
		
		// radiobuttons
		batchButton.setActionCommand("BATCH_BUTTON");
		versionButton.setActionCommand("VERSION_BUTTON");
		GUIUtils.createButtonGroup(batchButton,versionButton);
		
		//checkboxes
		intelliCheckBox.setActionCommand("INTELLI_CHECKBOX");
		
		// status label
		statusLabel.setFont(new Font("Dialog",Font.ITALIC,12));
		
		// buttons
		okButton.setEnabled(false);
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		intelliBrowseButton.setActionCommand("INTELLI_BROWSE");

		GUIUtils.addActionListener(this,batchButton,versionButton,okButton,cancelButton,intelliBrowseButton,intelliCheckBox);
		
		enableDisableIntelliSweepPart();

		content.add(mainPanel,BorderLayout.CENTER);
		content.add(buttonsPanel,BorderLayout.SOUTH);
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
	
	//------------------------------------------------------------------------
	private boolean isModelValid(String modelName, String modelVer) {
		if (modelName == null)
			modelName = modelField.getText().trim();
		if (modelVer == null)
			modelVer  = versionField.getText().trim();
		return (modelName.length() > 0 && modelVer.length() > 0);
	}
	
	//-------------------------------------------------------------------------
	private void modelVersionChanged(boolean edit) {
		if (edit) {
			okButton.setEnabled(false);
			scheduleReset = true;
		} else if (scheduleReset) {
			scheduleReset = false;
			String modelStatus;
			final String modelName = modelField.getText().trim();
			final String modelVer  = versionField.getText().trim();
			Model model = null;
			if (!modelName.equals("") && !modelVer.equals("")) {
				GUIUtils.setBusy(this,true);
				model = (Model) MEMEApp.LONG_OPERATION.executeNE("Searching...",null,new Callable<Object>(){
					public Object call() throws Exception {
						if (batchButton.isSelected())
							return ctx.getResultsDb().findModel(modelName,modelVer);
						else {
							for (int i = 0;i < files.length;++i) {
								String ver_str = modelVer.replace("%",String.valueOf(i));
								Model temp = ctx.getResultsDb().findModel(modelName,ver_str);
								// if it found at least one model, it displays 'Exists' status.
								if (temp != null)
									return temp;
							}
							return null;
						}
					}
				});
				GUIUtils.setBusy(this,false);
			}
			if (model != null) {
				modelStatus = " (Exists) ";
				okButton.setEnabled(true);
			} else if (isModelValid(modelName,modelVer)) { 
				// if modelVersion as pattern is valid, all version string will
				// be valid, too.
				modelStatus = " (New) ";
				okButton.setEnabled(true);
			} else {
				modelStatus = " (Invalid!) ";
				okButton.setEnabled(false);
			}
			statusLabel.setText(modelStatus);
		}
	}

	//--------------------------------------------------------------------------------
	private void modelVersionBindings(JTextField tf) {
		tf.getDocument().addDocumentListener(new DocumentListener() {
			 public void changedUpdate(DocumentEvent e)	{ modelVersionChanged(true); } 
			 public void insertUpdate(DocumentEvent e) { modelVersionChanged(true); }
			 public void removeUpdate(DocumentEvent e) { modelVersionChanged(true); }
		});
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { modelVersionChanged(false); }
		});
		tf.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) { modelVersionChanged(false); }
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void enableDisableIntelliSweepPart() {
		boolean toThis = intelliCheckBox.isSelected();
		intelliSettingsFile.setEnabled(toThis);
		intelliBrowseButton.setEnabled(toThis);
		intelliLabel.setEnabled(toThis);
    }

	//==============================================================================
	// nested classes
	
	//-------------------------------------------------------------------------
	/** In the case of 'Every file a new version' we not allow to delete the %
	 *  character from the version string. */
	private class VersionTextFieldFilter extends DocumentFilter {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		<T extends JTextComponent> T install(T comp) { 
			((AbstractDocument)comp.getDocument()).setDocumentFilter(this);
			return comp;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override 
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			if (versionButton.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0,len));
				sb.delete(offset,offset + length);
				int i = sb.indexOf("%");
				if (i < 0) 
					fb.insertString(len,"%",null);
			}
			super.remove(fb,offset,length);
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (versionButton.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0,len));
				sb.replace(offset,offset + length,text);
				int i = sb.indexOf("%");
				if (i < 0) 
					fb.insertString(len,"%",null);
			}
			super.replace(fb,offset,length,text,attrs);
		}
	}
}
