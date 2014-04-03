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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class ImportCSVForDOE extends JDialog implements ActionListener{
    private static final long serialVersionUID = 1L;

	private static final String CANCEL_BUTTON = "cancel";

	private static final String OK_BUTTON = "OK";

	private static final String BROWSE_BUTTON = "browse";

    private JPanel content = null;
    private JTextField fileNameField = new JTextField(40);
    private JTextField delimiterField = new JTextField(",",2);
    private JTextField decimalSignField = new JTextField(".",2);
    private JTextField stringQuotField = new JTextField("\"",2);
    private JLabel firstLineLabel = new JLabel("X");
    private JLabel secondLineLabel = new JLabel("X");
    private JCheckBox clearDataBeforeImportCheckBox = new JCheckBox("Clear data before import", true);
    private JFileChooser fileChooser = new JFileChooser();
    
    private JButton browseButton = new JButton("Browse...");
    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");
    private boolean okPressed = false;
    
    private String fileName = null;
    private String delimiter = ",";
    private String decimalSign = ".";
    @SuppressWarnings("unused")
	private String stringQuot = "\"";
    
    private File file = null;
    private List<ParameterInfo> paramList = null;
    private String[] importedParameterNames = null;
    
	/**
     * @param owner
     * @param title
     * @param modal
     * @throws HeadlessException
     */
    public ImportCSVForDOE(Frame owner, String title, boolean modal, List<ParameterInfo> paramList) throws HeadlessException {
	    super(owner, title, modal);
	    this.paramList = paramList;
    }

	public void makeGUI(){
    	content = FormsUtils.build(
    			"~ p ~ p ~ p ~ f:p:g ~ p ~ p ~ p ~", 
    			"0111112 p|" +
    			"_34_56_ p|" +
    			"_78____ p||" +
    			"999_AB_ p||" +
    			"CCCCCCC p||" +
    			"DDDDDDD p|" +
    			"EEEEEEE p|",
    			"CSV filename:", fileNameField, browseButton,
    			"delimiter:", delimiterField, "decimal sign:", decimalSignField,
    			"string qoutation:", stringQuotField,
    			clearDataBeforeImportCheckBox, okButton, cancelButton,
    			"The first two lines of the file:",
    			firstLineLabel,
    			secondLineLabel).getPanel();
    	cancelButton.setActionCommand(CANCEL_BUTTON);
    	okButton.setActionCommand(OK_BUTTON);
    	browseButton.setActionCommand(BROWSE_BUTTON);
    	GUIUtils.addActionListener(this, cancelButton, okButton, browseButton);
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
    	firstLineLabel.setText("");
    	secondLineLabel.setText("");
    }

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CANCEL_BUTTON)){
			okPressed = false;
			this.setVisible(false);
		} else if (e.getActionCommand().equals(OK_BUTTON)){
			if(isClearData()) clearData();
			try {
	            parseCSV();
				okPressed = true;
	            this.setVisible(false);
            } catch (Exception e1) {
	            e1.printStackTrace();
            }
		} else if (e.getActionCommand().equals(BROWSE_BUTTON)){
			int result = fileChooser.showOpenDialog(this);
			if(result == JFileChooser.APPROVE_OPTION){
				file = fileChooser.getSelectedFile();
				fileName = file.getAbsolutePath();
				fileNameField.setText(fileName);
				try {
	                preParseCSV();
                } catch (Exception e1) {
	                e1.printStackTrace();
                }
			}
		}
    }

	private void clearData() {
		for (int i = 0; i < paramList.size(); i++) {
	        Object value = paramList.get(i).getValue();
	        paramList.get(i).clear();
	        paramList.get(i).setRuns(1);
	        paramList.get(i).setValue(value);
        }
    }

	private void preParseCSV() throws Exception {
		if (file == null || !file.exists() || file.isDirectory()) 
			throw new Exception("Cannot find file");
		FileInputStream is = new FileInputStream(file);
		long fSize = is.available();
		int buffSize = (int)Math.min(Math.max(fSize/100,80),8192);
		BufferedReader in = new BufferedReader(new InputStreamReader(is),buffSize);
		String line = in.readLine();
		String firstLine = line;
		if(line == null || line.equals("")){
			in.close();
			is.close();
			throw new Exception("Missing header");
		}
		delimiter = delimiterField.getText();
		if(line.indexOf(delimiter) == -1){
			swapDelimiters();
			if(line.indexOf(delimiter) == -1){
				in.close();
				is.close();
				throw new Exception("Cannot determine delimiter");
			}
		}
		line = in.readLine();
		String secondLine = line;
		firstLineLabel.setText(firstLine);
		secondLineLabel.setText(secondLine);
		String[] headerItems = firstLine.trim().split(delimiter);
		for (int i = 0; i < headerItems.length; i++) {
	        boolean found = false;
	        for (int j = 0; j < paramList.size(); j++) {
	            if(paramList.get(j).getName().equals(headerItems[i])) found = true;
            }
	        if(!found){
	        	throw new Exception("Could not find matching parameter for the header item:" + headerItems[i]);
	        }
        }
		in.close();
		is.close();
    }
	private void swapDelimiters(){
		if(delimiter.equals(",")) delimiter = ";";
		else delimiter = ",";
		delimiterField.setText(delimiter);
	}

	private void parseCSV() throws Exception {
		importedParameterNames = null;
		if (file == null || !file.exists() || file.isDirectory()) 
			throw new Exception("Cannot find file");
		FileInputStream is = new FileInputStream(file);
		long fSize = is.available();
		int buffSize = (int)Math.min(Math.max(fSize/100,80),8192);
		BufferedReader in = new BufferedReader(new InputStreamReader(is),buffSize);
		String line = in.readLine();
		Vector<ParameterInfo> header = new Vector<ParameterInfo>();
		String[] headerItems = line.trim().split(delimiter);
		for (int i = 0; i < headerItems.length; i++) {
	        boolean found = false;
	        for (int j = 0; j < paramList.size(); j++) {
	            if(paramList.get(j).getName().equals(headerItems[i])) {
	            	found = true;
	            	header.add(paramList.get(j));
	            	paramList.get(j).clear();
	            	paramList.get(j).setDefinitionType(ParameterInfo.LIST_DEF);
	            }
            }
	        if(!found){
	        	throw new Exception("Could not find matching parameters for the header");
	        }
        }
		while((line = in.readLine()) != null){
			String[] items = line.split(delimiter);
			//TODO: csin�ld meg, hogy a sztringquoe is menjen
			for (int i = 0; i < items.length; i++) {
				//TODO: csin�ld meg az �res sztring esetet
				if(header.get(i).getType().toLowerCase().equals("float") ||
						header.get(i).getType().toLowerCase().equals("double")){
					items[i] = items[i].replaceAll(decimalSign, ".");
				}
				header.get(i).getValues().add(ParameterInfo.getValue(items[i], header.get(i).getType()));
            }
		}
		in.close();
		is.close();
		importedParameterNames = headerItems;
    }
	
	public boolean isClearData(){
		return clearDataBeforeImportCheckBox.isSelected();
	}

	public boolean isOkPressed() {
    	return okPressed;
    }

	public String[] getImportedParameterNames() {
    	return importedParameterNames;
    }

}
