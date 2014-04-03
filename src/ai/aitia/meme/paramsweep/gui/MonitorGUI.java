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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.launch.IMonitorListener;
import ai.aitia.meme.paramsweep.launch.IMonitorListener.ModelElement;
import ai.aitia.meme.paramsweep.launch.IMonitorListener.MonitorEvent;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;

/** This class provides the main graphical user interface of the MASS/MEME Monitor component. */
public class MonitorGUI extends JPanel implements ActionListener,
											      ListSelectionListener,
											      MouseListener {

	//=================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	/** The owner of the component. */
	private Window owner = null;
	/** The connection properties to the server applications. */
	private Properties serverProperties = null;
	/** Flag that determines whether the monitor is called from the MEME or not. */
	private boolean fromMEME = false;
	/** Panel manager object (used only in MEME mode). */
	private IPanelManager manager = null;

	private boolean local = true;
	private String actModelName = null;
	
	private IMonitorListener listener = null;
	
	private ModelTableModel tmodel = new ModelTableModel();
	private TableSorter sorter = new TableSorter(tmodel);

	//=================================================================================
	// GUI-members
	
	private JTabbedPane tabbed = new JTabbedPane();
	private JPanel buttonPanel = new JPanel(new BorderLayout());
	private JTextField simulationField = new JTextField();
	private JTextArea detailsArea = new JTextArea();
	private JScrollPane scrDetails = new JScrollPane(detailsArea);
	private JTextArea progressArea = new JTextArea();
	private JScrollPane scrProgress = new JScrollPane(progressArea);
	private JTextArea timeArea = new JTextArea();
	private JScrollPane scrTime = new JScrollPane(timeArea);
	private JPanel currentPanel = null;
	private JPanel interruptPanel = new JPanel(new BorderLayout());
	private JButton interruptButton = new JButton(); // local: stop current run, remote: stop simulation 
	private JButton hardStopButton = new JButton(); // local: stop simulation, remote: kill simulation
	private JButton configureButton = new JButton("Configure...");
	private JButton aboutButton = new JButton("About...");
	private JButton closeButton = new JButton();  // local: stop & close, remote: close

	private JPanel left = null;
	private JPanel right = null;
	private JTable modelTable = new JTable(sorter) {
		private static final long serialVersionUID = 1L;
		{
			tableHeader.setReorderingAllowed(false);
		}
	};
	private JScrollPane scrModelTable = new JScrollPane(modelTable);
	private JButton deleteButton = new JButton("Remove simulation");
	private JButton updateButton = new JButton("Update list");
	private JTextArea modelDescriptionArea = new JTextArea();
	private JScrollPane scrModelDescriptionArea = new JScrollPane(modelDescriptionArea);
	private JList modelOutputList = new JList();
	private JScrollPane scrModelOutputList = new JScrollPane(modelOutputList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
															 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	private JButton downloadButton = new JButton("Download");
	private JButton importButton = new JButton("Import to database");
	private JPanel finishedPanel = null;
	
	private JList waitingSimulations = new JList();
	private JScrollPane scrWaitingSimulations = new JScrollPane(waitingSimulations);
	
	//=================================================================================
	// methods
	
	//--------------------------------------------------------------------------------
	public MonitorGUI(Window owner, boolean fromMEME, IPanelManager manager, boolean local) {
		this.owner = owner;
		this.fromMEME = fromMEME;
		this.manager = manager;
		this.local = local; 
		layoutGUI();
		initialize();
		initializeXButton(owner);
		if (!local) {
			File confFile = new File(MonitorConfigurationDialog.configurationFileName);
			MonitorConfigurationDialog d = null;
			if (owner instanceof Frame)
				d = new MonitorConfigurationDialog((Frame)owner,confFile.exists(),fromMEME);
			else
				d = new MonitorConfigurationDialog((Dialog)owner,confFile.exists(),fromMEME);
			this.serverProperties = confFile.exists() ? d.getProperties() : d.showDialog();
		}
	}
	
	//--------------------------------------------------------------------------------
	public void setServerProperties(Properties serverProperties) { this.serverProperties = serverProperties; }
	public Properties getServerProperties() { return serverProperties; }
	
	
	//----------------------------------------------------------------------------------------------------
	public void addMonitorListener(IMonitorListener listener) { this.listener = listener; }
	
	//----------------------------------------------------------------------------------------------------
	public void removeMonitorListener(IMonitorListener listener) {
		if (this.listener != null && this.listener.equals(listener))
			this.listener = null;
	}
	
	//---------------------------------------------------------------------------------
	/** Releases the resources of the component. */
	public void dispose() {
		resetSecondPage();
		tmodel.clearData();
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isLocal() { return local; }
	public String getSimulationName() { return actModelName; }
	public void setEnabledInterruptButton(boolean enabled) { interruptButton.setEnabled(enabled); }
	public void setEnabledHardStopButton(boolean enabled) { hardStopButton.setEnabled(enabled); }
	public void setEnabledRemoveSimulationButton(boolean enabled) { deleteButton.setEnabled(enabled); }
	public void setEnabledRemoveSimulationButtonIfNeed() { deleteButton.setEnabled(modelTable.getSelectedRows().length > 0); }
	public void setDetails(String details) { detailsArea.setText(details); }
	public void setProgress(String progress) { progressArea.setText(progress); }
	public void setWaitingSimulationsListModel(ListModel model) { waitingSimulations.setModel(model); }
	public String getTime() { return timeArea.getText().trim(); }
	public void setTime(String time) { timeArea.setText(time); }
	public IPanelManager getPanelManager() { return manager; }
	public void setCloseButtonText(String text) { closeButton.setText(text); }
	public void updateColumnWidths() { updateColumnWidths(modelTable,scrModelTable); }
	
	//----------------------------------------------------------------------------------------------------
	public void setEnabledTabs(boolean enabled) {
		tabbed.setEnabledAt(0,enabled);
		tabbed.setEnabledAt(1,enabled);
		tabbed.setEnabledAt(2,enabled);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setFinishedSimulations(List<ModelElement> list) { 
		tmodel.clearData();
		for (ModelElement me : list)
			tmodel.addModel(me);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setSimulationName(String simulationName) {
		actModelName = simulationName;
		simulationField.setText(simulationName);
		simulationField.setToolTipText(simulationName);
	}
	
	//--------------------------------------------------------------------------------
	/** Notifies the owner that this component needs to hide. */
	public void notifyForHide() {
		if (local)
			fireStopSimulationSignal();
		if (fromMEME) {
			dispose();
			manager.remove(this);
		} else {
			if (local)
				System.exit(0);
			else {
				final ComponentEvent event = new ComponentEvent(this,ComponentEvent.COMPONENT_HIDDEN);
				SwingUtilities.invokeLater(new Runnable(){
					public void run() { owner.dispatchEvent(event); }
				});
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void switchToResults(String name) {
		tabbed.setSelectedIndex(1);
		selectFinishedModel(name);
	}
	
	//=================================================================================
	// implemented interfaces
	
	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("CONFIGURE")) {
			MonitorConfigurationDialog dialog = null;
			if (owner instanceof Frame)
				dialog = new MonitorConfigurationDialog((Frame)owner,fromMEME);
			else
				dialog = new MonitorConfigurationDialog((Dialog)owner,fromMEME);
			serverProperties = dialog.showDialog();
			if (!dialog.isCancel()) {
				dispose();
				fireReconnectSignal();
			}
		} else if (command.equals("ABOUT")) {
			Object[] options = { "OK", "License agreement" };
			int ret = JOptionPane.showOptionDialog(ParameterSweepWizard.getFrame(),getAboutContentPane(),"About MEME Parameter Sweep Tools", 
												   JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
			if (ret == 1)
				showLicense();
		} else if (command.equals("CLOSE")) 
			notifyForHide();
		else if (command.equals("UPDATE"))
			fireUpdateSimulationListSignal();
		else if (command.equals("DOWNLOAD")) {
			ModelElement me = (ModelElement) sorter.getValueAt(modelTable.getSelectedRow(),0);
			Object[] selected = modelOutputList.getSelectedValues();
			fireDownloadSimulationResultSignal(me,selected);
		} else if (command.equals("IMPORT")) {
			ModelElement me = (ModelElement) sorter.getValueAt(modelTable.getSelectedRow(),0);
			Object[] selected = modelOutputList.getSelectedValues();
			fireImportSimulationResultSignal(me,selected);
		} else if (command.equals("DELETE")) {
			if (modelTable.getSelectedRow() == -1) return;
			int result = Utilities.askUser(this,false,"Confirmation","Are you sure?");
			if (result == 1) {
				int[] indices = modelTable.getSelectedRows();
				List<ModelElement> deletables = new ArrayList<ModelElement>(indices.length);
				for (int idx : indices) 
					deletables.add((ModelElement)sorter.getValueAt(idx,0));
				fireRemoveSimulationSignal(deletables);
				fireUpdateSimulationListSignal();
			}
		} else if (command.equals("INTERRUPT")) {
			if (actModelName == null ||	actModelName.equals("There is no currently running simulation!")) return;
			if (local)
				fireInterruptSignal();
			else {
				int result = Utilities.askUser(this,false,"Confirmation","Are you sure?");
				if (result == 1) 
					fireInterruptSignal();
			}
		} else if (command.equals("HARD_STOP")) {
			if (actModelName == null ||	actModelName.equals("There is no currently running simulation!")) return;
			int result = Utilities.askUser(this,false,"Confirmation","Are you sure?");
			if (result == 1) 
				fireHardStopSignal();
		}
	}
	
	//--------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(modelTable.getSelectionModel())) {
			if (e.getValueIsAdjusting()) return;
			int index = modelTable.getSelectedRow();
			if (index == -1)
				resetSecondPage();
			else {
				ModelElement me = (ModelElement) sorter.getValueAt(modelTable.getSelectedRow(),0);;
				modelDescriptionArea.setText(me.getDescription());
				DefaultListModel outputModel = new DefaultListModel();
				for (String s : me.getOutputs())
					outputModel.addElement(s);
				modelOutputList.setModel(outputModel);
				modelOutputList.setSelectionInterval(0,outputModel.getSize()-1);
				deleteButton.setEnabled(true);
				downloadButton.setEnabled(true);
				importButton.setEnabled(true);
			}
		} else if (e.getSource().equals(modelOutputList)) {
			downloadButton.setEnabled(!modelOutputList.isSelectionEmpty());
			importButton.setEnabled(!modelOutputList.isSelectionEmpty());
		}
	}
	
	//---------------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	//---------------------------------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 2 && e.getSource() instanceof JList &&
			modelOutputList.getSelectedIndex() != -1) 
			downloadButton.doClick();
	}
	
	//=================================================================================
	// GUI-methods
	
	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		interruptPanel.add(hardStopButton,BorderLayout.WEST);
		interruptPanel.add(interruptButton,BorderLayout.EAST);
		
		currentPanel = FormsUtils.build("p ~ p:g",
										"[DialogBorder]01||" +
										              "22 f:p:g||" +
										              "33||" +
										              "44 p||" +
										              "55",
										" Simulation: ",simulationField,
										scrDetails,
										scrProgress,
										scrTime,
										interruptPanel).getPanel();
		
		left = FormsUtils.build("p:g ~ p ~ p",
				                "000 f:p:g||" +
				                "_12 p|",
				                scrModelTable,
				                deleteButton,updateButton).getPanel();
		
		right = FormsUtils.build("p:g ~ p ~ p",
								 "000||" +
				                 "111 f:p:g||" +
				                 "222 p||" +
				                 "333 f:p:g||" +
				                 "_45 p|",
				                 new Separator("Description"),
				                 scrModelDescriptionArea,
				                 new Separator("Output files"),
				                 scrModelOutputList,
				                 downloadButton,
				                 importButton).getPanel();
		
		
		finishedPanel = FormsUtils.build("f:p:g ~ p",
										 "[DialogBorder]01 f:p:g",
										 left,right).getPanel();
		if (!local) {
			tabbed.addTab("Current simulation",currentPanel);
			tabbed.addTab("Finished simulations",finishedPanel);
			tabbed.addTab("Waiting simulations",scrWaitingSimulations);
		}
		
		if (!local || !fromMEME) {
			JPanel tmp = new JPanel();
			tmp.setLayout(new BoxLayout(tmp,BoxLayout.X_AXIS));
			if (!local)
				tmp.add(configureButton);
			if (!local && !fromMEME)
				tmp.add(Box.createRigidArea(new Dimension(5,0)));
			if (!fromMEME)
				tmp.add(aboutButton);
			buttonPanel.add(tmp,BorderLayout.WEST);
		}
		buttonPanel.add(closeButton,BorderLayout.EAST);
	
		this.setLayout(new BorderLayout());
		this.add(local? currentPanel : tabbed,BorderLayout.CENTER);
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(buttonPanel);
		this.add(tmp,BorderLayout.SOUTH);
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		simulationField.setEditable(false);
		simulationField.setPreferredSize(new Dimension(600,26));
		
		scrDetails.setBorder(BorderFactory.createTitledBorder("Details"));
		detailsArea.setEditable(false);
		detailsArea.setBorder(null);
		detailsArea.setBackground(this.getBackground());
		detailsArea.setLineWrap(true);
		detailsArea.setWrapStyleWord(true);
		detailsArea.setRows(5);

		scrProgress.setBorder(BorderFactory.createTitledBorder("Progress"));
		progressArea.setEditable(false);
		progressArea.setBorder(null);
		progressArea.setBackground(this.getBackground());
		progressArea.setLineWrap(true);
		progressArea.setWrapStyleWord(true);
		progressArea.setRows(5);
		
		scrTime.setBorder(BorderFactory.createTitledBorder("Time"));
		timeArea.setEditable(false);
		timeArea.setBorder(null);
		timeArea.setBackground(this.getBackground());
		timeArea.setLineWrap(true);
		timeArea.setWrapStyleWord(true);
		timeArea.setRows(2);
		timeArea.setVisible(PlatformSettings.getGUIControllerForPlatform().isTimeDisplayed()); 
		
		interruptPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		interruptButton.setText(local ? "Stop current run" : "Stop simulation"); 
		hardStopButton.setText("Stop simulation" + (local ? "" : " now"));
		closeButton.setText((local ? "Stop & " : "") + "Close"); 

		left.setBorder(BorderFactory.createTitledBorder("Available models"));
		modelTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		modelTable.getSelectionModel().addListSelectionListener(this);
		updateButton.setActionCommand("UPDATE");
		right.setBorder(BorderFactory.createTitledBorder("Selected simulation"));
		modelDescriptionArea.setEditable(false);
		modelDescriptionArea.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		modelDescriptionArea.setBackground(simulationField.getBackground());
		modelDescriptionArea.setLineWrap(true);
		modelDescriptionArea.setWrapStyleWord(true);
		modelOutputList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		modelOutputList.addListSelectionListener(this);
		modelOutputList.addMouseListener(this);
		downloadButton.setActionCommand("DOWNLOAD");
		downloadButton.setEnabled(false);
		importButton.setActionCommand("IMPORT");
		importButton.setEnabled(false);
		if (!fromMEME)
			importButton.setVisible(false);
		deleteButton.setEnabled(false);
		
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
		configureButton.setActionCommand("CONFIGURE");
		aboutButton.setActionCommand("ABOUT");
		closeButton.setActionCommand("CLOSE");
		deleteButton.setActionCommand("DELETE");
		interruptButton.setActionCommand("INTERRUPT");
		hardStopButton.setActionCommand("HARD_STOP");

		scrModelTable.setPreferredSize(new Dimension(350,100));
		modelTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		modelTable.setDefaultRenderer(ModelElement.class,new ModelElementRenderer());
		sorter.setTableHeader(modelTable.getTableHeader());
		scrWaitingSimulations.setBorder(BorderFactory.createTitledBorder("Waiting simulations"));

		GUIUtils.addActionListener(this,updateButton,downloadButton,importButton,configureButton,closeButton,deleteButton,interruptButton,
								   hardStopButton, aboutButton);
	}
	
	//--------------------------------------------------------------------------------
	/** Overrides the handler of the button 'x' on the top right corner. */
	private void initializeXButton(Window owner) {
		if (local) {
			if (owner instanceof JFrame) {
				JFrame frame = (JFrame)owner;
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			} else if (owner instanceof JDialog) {
				JDialog dialog = (JDialog)owner;
				dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			}
			owner.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					closeButton.doClick();
				}
			});
		}
	}
	
	//=================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	public void updateColumnWidths(JTable table, javax.swing.JScrollPane sc) {
		java.awt.FontMetrics fm = table.getFontMetrics(table.getFont());
		java.awt.Graphics g = table.getGraphics();
		javax.swing.table.TableColumnModel cm = table.getColumnModel();
		int gap = GUIUtils.dluX(4,table);
		int totalw = sc.getWidth() - sc.getVerticalScrollBar().getPreferredSize().width	- gap;
		float sum = 0, maxw = 0;
		int n = cm.getColumnCount();		
		ArrayList<Float> widths = new ArrayList<Float>(n);
		for (int i = 0;i < n;++i) {			
			String header = String.valueOf(cm.getColumn(i).getHeaderValue());
			float width;
			if (g != null) 
				width = (float) fm.getStringBounds(header,0,header.length(),g).getWidth();
			else
				width = fm.stringWidth(header);
			float w = width + gap;
			if (w > maxw)
				maxw = w;
			sum += w;
			widths.add(w);
		}
		if (n >= 0 && sum < totalw) {
			float rf = 1.0f;
			if (maxw * n < totalw) 
				maxw = totalw / (float) n;
			else
				rf = (totalw - sum) / (n * maxw - sum);
			for (int i = 0; i < n; ++i) {
				float w = widths.get(i);
				widths.set(i,w + (maxw - w) * rf);
			}
		}
		for (int i = n - 1 ; i >= 0; --i) {
			int computedWidth = Math.round(widths.get(i));
			if (i == n - 1)
				computedWidth = Math.max(computedWidth,100);
			cm.getColumn(i).setPreferredWidth(computedWidth);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Selects the last finished model in the Finished simulations list. 
	 * @param name the name of the last finished model
	 */
	private void selectFinishedModel(String name) {
		if (modelTable != null) {
			for (int i = 0;i < sorter.getRowCount(); ++i) {
				ModelElement me = (ModelElement) sorter.getValueAt(i,0);
				if (me.oldToString().equals(name)) {
					modelTable.getSelectionModel().setSelectionInterval(i,i);
					return;
				}
			}
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Resets the Finished simulations page. */
	private void resetSecondPage() {
		modelDescriptionArea.setText("");
		modelOutputList.setModel(new DefaultListModel());
		downloadButton.setEnabled(false);
		importButton.setEnabled(false);
		deleteButton.setEnabled(false);
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the component that contains the content of the About dialog. */
	private JEditorPane getAboutContentPane() {
		JEditorPane aboutContentPane = new JEditorPane();
		aboutContentPane.setEditable(false);
		aboutContentPane.setBorder(null);
		try {
			InputStream is = SimpleFileFilter.class.getResourceAsStream("icons/about/about.html");
			StringWriter s = new java.io.StringWriter();
			if (is != null) 
				Utils.copyRW(new java.io.InputStreamReader(is), s);
			String htmlPage = s.toString();
			htmlPage = htmlPage.replace("$VER$",MEMEApp.CURRENT_VERSION);
			htmlPage = htmlPage.replace("<body>",Utils.htmlBody());
			htmlPage = htmlPage.replace("src=\"","src=\"gui/icons/about/");
			GUIUtils.setTextPane(aboutContentPane,htmlPage);
		} catch (Exception e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		}
		GUIUtils.setWrapLength(aboutContentPane, GUIUtils.dluX(465, aboutContentPane));
		return aboutContentPane;
	}
	
	//-------------------------------------------------------------------------------
	/** Shows the license file. */
	private void showLicense() {
		URL url = SimpleFileFilter.class.getResource("icons/about/license.txt");
		GUIUtils.SPMSAEditorPane message;
		try { 
			message = new GUIUtils.SPMSAEditorPane(url);
		} catch (Exception e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return;
		}
		message.setEditable(false);
		message.setPreferredSize(new Dimension(GUIUtils.dluX(380),Integer.MAX_VALUE));
		JScrollPane sp = new javax.swing.JScrollPane(message);
		sp.setMaximumSize(new Dimension(GUIUtils.dluX(400),GUIUtils.getRelScrH(80)));
		JOptionPane.showMessageDialog(ParameterSweepWizard.getFrame(),sp,"MEME Parameter Sweep Tools License",JOptionPane.PLAIN_MESSAGE);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireReconnectSignal() {
		if (listener != null)
			listener.reconnectSignal(new MonitorEvent(this));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireStopSimulationSignal() {
		if (listener != null)
			listener.stopSimulationSignal(new MonitorEvent(this));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireUpdateSimulationListSignal() {
		if (listener != null)
			listener.updateSimulationListSignal(new MonitorEvent(this));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireDownloadSimulationResultSignal(ModelElement element, Object[] selectedResults) {
		if (listener != null) {
			List<String> results = new ArrayList<String>(selectedResults.length);
			for (Object o : selectedResults)
				results.add(o.toString().trim());
			listener.downloadSimulationResultSignal(new MonitorEvent(this,element,results));
		}
	}

	//----------------------------------------------------------------------------------------------------
	private void fireImportSimulationResultSignal(ModelElement element, Object[] selectedResults) {
		if (listener != null) {
			List<String> results = new ArrayList<String>(selectedResults.length);
			for (Object o : selectedResults)
				results.add(o.toString().trim());
			listener.importSimulationResultSignal(new MonitorEvent(this,element,results));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireRemoveSimulationSignal(List<ModelElement> elements) {
		if (listener != null) 
			listener.removeSimulationSignal(new MonitorEvent(this,elements));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireInterruptSignal() {
		if (listener != null) {
			MonitorEvent event = new MonitorEvent(this);
			if (local) 
				listener.stopCurrentRunSignal(event);
			else
				listener.stopSimulationSignal(event);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireHardStopSignal() {
		if (listener != null) {
			MonitorEvent event = new MonitorEvent(this);
			if (local)
				listener.stopSimulationSignal(event);
			else
				listener.killSimulationSignal(event);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class DateOfModel implements Comparable<DateOfModel> {
		
		//====================================================================================================
		// members
		
		private Date date = null;
		private String display = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public DateOfModel(Date date, String display) { this.date = date; this.display = display; }
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return display; }
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public int compareTo(DateOfModel o) { return this.date.compareTo(o.date); }
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unused")
	private static class PlatformComparator implements Comparator<PlatformType> {

		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public int compare(PlatformType o1, PlatformType o2) {
			return o1.toString().compareTo(o2.toString());
		}
		
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class ModelTableModel extends AbstractTableModel implements TableModelListener {

		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		private String[] columnNames = new String[] { "Name", "Date", "Platform" };
		private List<Object[]> data = null;
		
		//========================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ModelTableModel() {
			data = new ArrayList<Object[]>();
		}
		
		//------------------------------------------------------------------------
		public void clearData() { 
			data = new ArrayList<Object[]>();
			fireTableChanged(new TableModelEvent(this));
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override public String getColumnName(int col) { return columnNames[col]; }
		public int getColumnCount() { return columnNames.length; }
		public int getRowCount() { return data.size(); }
		public Object getValueAt(int rowIndex, int columnIndex) { return data.get(rowIndex)[columnIndex]; }
		public ModelElement getModel(int rowIndex) { return (ModelElement) data.get(rowIndex)[0]; }
		
		//----------------------------------------------------------------------------------------------------
		public void addModel(ModelElement element) {
			try {
				Date d = Utilities.getTimeStamp(element.getModelName());
				String disp = Utilities.getModelNameAndTimeStamp(element.getModelName())[1];
				data.add(new Object[] { element, new DateOfModel(d,disp), element.getPlatform() });
			} catch (ParseException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			}
		}
		
		//------------------------------------------------------------------------
		@Override
		public Class<?> getColumnClass(int c) {
	        switch (c) {
	        case 0  : return ModelElement.class;
	        case 2  : return PlatformType.class;
	        default : return DateOfModel.class;
	        }
	    }
		
		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void tableChanged(TableModelEvent e) { fireTableChanged(e); }
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class ModelElementRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		//----------------------------------------------------------------------------------------------------
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			setBorder(null);
			if (hasFocus) {
		            Border border = null;
		            if (isSelected) 
		                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
		            if (border == null) 
		                border = UIManager.getBorder("Table.focusCellHighlightBorder");
		            setBorder(border);
		    }
			setHorizontalAlignment(JLabel.LEADING);
			setFont(table.getFont());
			if (value == null) 
				setText("");
			else {
				ModelElement element = (ModelElement) value;
				if (element.isWrong()) 
					setForeground(Color.RED);
				String text = element.isWrong() ? "ERROR: " : "";
				text += element.toString();
				setText(text);
				setToolTipText(element.toString());
			}
			return this;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class TableSorter extends AbstractTableModel {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;

//		protected ModelTableModel tableModel;
		protected AbstractTableModel tableModel;

	    public static final int DESCENDING = -1;
	    public static final int NOT_SORTED = 0;
	    public static final int ASCENDING = 1;

	    private static Directive EMPTY_DIRECTIVE = new Directive(-1,NOT_SORTED);

	    public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
	        @SuppressWarnings("unchecked")
			public int compare(Object o1, Object o2) { return ((Comparable) o1).compareTo(o2); }
	    };

	    private Row[] viewToModel;
	    private int[] modelToView;

	    private JTableHeader tableHeader;
	    private MouseListener mouseListener;
	    private TableModelListener tableModelListener;
	    private Map<Class<?>,Comparator<?>> columnComparators = new HashMap<Class<?>,Comparator<?>>();
	    private List<Directive> sortingColumns = new ArrayList<Directive>();

	    //====================================================================================================
		// methods
	    
	    //----------------------------------------------------------------------------------------------------
		public TableSorter() {
	        this.mouseListener = new MouseHandler();
	        this.tableModelListener = new TableModelHandler();
	        this.columnComparators.put(PlatformType.class,new PlatformComparator());
	    }

	    //----------------------------------------------------------------------------------------------------
		public TableSorter(AbstractTableModel tableModel) {
	        this();
	        setTableModel(tableModel);
	    }

	    //----------------------------------------------------------------------------------------------------
		private void clearSortingState() {
	        viewToModel = null;
	        modelToView = null;
	    }

	    //----------------------------------------------------------------------------------------------------
//		public ModelTableModel getTableModel() { return tableModel; }
		public AbstractTableModel getTableModel() { return tableModel; }
		public JTableHeader getTableHeader() { return tableHeader; }
		public boolean isSorting() { return sortingColumns.size() != 0; }
		public int getSortingStatus(int column) { return getDirective(column).direction; }
		public int modelIndex(int viewIndex) { return getViewToModel()[viewIndex].modelIndex; }

	    //----------------------------------------------------------------------------------------------------
		public void setTableModel(AbstractTableModel tableModel) {
	        if (this.tableModel != null) 
	            this.tableModel.removeTableModelListener(tableModelListener);
	        this.tableModel = tableModel;
	        if (this.tableModel != null) 
	            this.tableModel.addTableModelListener(tableModelListener);
	        clearSortingState();
	        fireTableStructureChanged();
	    }

	    //----------------------------------------------------------------------------------------------------
		public void setTableHeader(JTableHeader tableHeader) {
	        if (this.tableHeader != null) {
	            this.tableHeader.removeMouseListener(mouseListener);
	            TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
	            if (defaultRenderer instanceof SortableHeaderRenderer) 
	                this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer)defaultRenderer).tableCellRenderer);
	        }
	        this.tableHeader = tableHeader;
	        if (this.tableHeader != null) {
	            this.tableHeader.addMouseListener(mouseListener);
	            this.tableHeader.setDefaultRenderer(
	                    new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
	        }
	    }

	    //----------------------------------------------------------------------------------------------------
		private Directive getDirective(int column) {
	        for (int i = 0;i < sortingColumns.size();i++) {
	            Directive directive = sortingColumns.get(i);
	            if (directive.column == column) 
	                return directive;
	        }
	        return EMPTY_DIRECTIVE;
	    }

	    //----------------------------------------------------------------------------------------------------
		private void sortingStatusChanged() {
	        clearSortingState();
	        fireTableDataChanged();
	        if (tableHeader != null) 
	            tableHeader.repaint();
	    }

	    //----------------------------------------------------------------------------------------------------
		public void setSortingStatus(int column, int status) {
	        Directive directive = getDirective(column);
	        if (directive != EMPTY_DIRECTIVE) 
	            sortingColumns.remove(directive);
	        if (status != NOT_SORTED) 
	            sortingColumns.add(new Directive(column,status));
	        sortingStatusChanged();
	    }

	    //----------------------------------------------------------------------------------------------------
		protected Icon getHeaderRendererIcon(int column, int size) {
	        Directive directive = getDirective(column);
	        if (directive == EMPTY_DIRECTIVE) return null;
	        return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
	    }

	    //----------------------------------------------------------------------------------------------------
		private void cancelSorting() {
	        sortingColumns.clear();
	        sortingStatusChanged();
	    }

	    //----------------------------------------------------------------------------------------------------
		public void setColumnComparator(Class type, Comparator comparator) {
	        if (comparator == null) 
	            columnComparators.remove(type);
	        else
	            columnComparators.put(type, comparator);
	    }

	    //----------------------------------------------------------------------------------------------------
		protected Comparator getComparator(int column) {
	        Class columnType = tableModel.getColumnClass(column);
	        Comparator comparator = columnComparators.get(columnType);
	        if (comparator != null) 
	            return comparator;
	        if (Comparable.class.isAssignableFrom(columnType)) 
	            return COMPARABLE_COMPARATOR;
	        throw new IllegalStateException();
	    }

	    //----------------------------------------------------------------------------------------------------
		private Row[] getViewToModel() {
	        if (viewToModel == null) {
	            int tableModelRowCount = tableModel.getRowCount();
	            viewToModel = new Row[tableModelRowCount];
	            for (int row = 0;row < tableModelRowCount;row++) 
	                viewToModel[row] = new Row(row);
	            if (isSorting()) 
	                Arrays.sort(viewToModel);
	        }
	        return viewToModel;
	    }

	    //----------------------------------------------------------------------------------------------------
	    private int[] getModelToView() {
	        if (modelToView == null) {
	            int n = getViewToModel().length;
	            modelToView = new int[n];
	            for (int i = 0;i < n;i++) 
	                modelToView[modelIndex(i)] = i;
	        }
	        return modelToView;
	    }

	    //====================================================================================================
		// implemented interfaces 

	    //----------------------------------------------------------------------------------------------------
		public int getRowCount() { return (tableModel == null) ? 0 : tableModel.getRowCount(); }
	    public int getColumnCount() { return (tableModel == null) ? 0 : tableModel.getColumnCount(); }
	    @Override public String getColumnName(int column) { return tableModel.getColumnName(column); }
	    @Override public Class<?> getColumnClass(int column) { return tableModel.getColumnClass(column); }
	    @Override public boolean isCellEditable(int row, int column) { return tableModel.isCellEditable(modelIndex(row), column); }
	    public Object getValueAt(int row, int column) { return tableModel.getValueAt(modelIndex(row),column); }
	    @Override public void setValueAt(Object aValue, int row, int column) { tableModel.setValueAt(aValue, modelIndex(row), column); }

	    //====================================================================================================
		// nested classes
	    
	    //----------------------------------------------------------------------------------------------------
		private class Row implements Comparable {
			
			//====================================================================================================
			// members
			
	        private int modelIndex;

	        //====================================================================================================
			// methods
	        
	        //----------------------------------------------------------------------------------------------------
			public Row(int index) {
	            this.modelIndex = index;
	        }

	        //----------------------------------------------------------------------------------------------------
			@SuppressWarnings("unchecked")
			public int compareTo(Object o) {
	            int row1 = modelIndex;
	            int row2 = ((Row)o).modelIndex;

	            for (Directive directive : sortingColumns) {
	                int column = directive.column;
	                Object o1 = tableModel.getValueAt(row1, column);
	                Object o2 = tableModel.getValueAt(row2, column);

	                int comparison = 0;
	                // Define null less than everything, except null.
	                if (o1 == null && o2 == null) 
	                    comparison = 0;
	                else if (o1 == null) 
	                    comparison = -1;
	                else if (o2 == null) 
	                    comparison = 1;
	                else
	                    comparison = getComparator(column).compare(o1, o2);
	                if (comparison != 0) 
	                    return directive.direction == DESCENDING ? - comparison : comparison;
	            }
	            return 0;
	        }
	    }

	    //----------------------------------------------------------------------------------------------------
		private class TableModelHandler implements TableModelListener {
	        public void tableChanged(TableModelEvent e) {
	            // If we're not sorting by anything, just pass the event along.             
	            if (!isSorting()) {
	                clearSortingState();
	                fireTableChanged(e);
	                return;
	            }
	                
	            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
	                cancelSorting();
	                fireTableChanged(e);
	                return;
	            }

	            int column = e.getColumn();
	            if (e.getFirstRow() == e.getLastRow()
	                && column != TableModelEvent.ALL_COLUMNS
	                && getSortingStatus(column) == NOT_SORTED
	                && modelToView != null) {
	                int viewIndex = getModelToView()[e.getFirstRow()];
	                fireTableChanged(new TableModelEvent(TableSorter.this, 
	                                                     viewIndex, viewIndex, 
	                                                     column, e.getType()));
	                return;
	            }

	            // Something has happened to the data that may have invalidated the row order. 
	            clearSortingState();
	            fireTableDataChanged();
	            return;
	        }
	    }

	    //----------------------------------------------------------------------------------------------------
		private class MouseHandler extends MouseAdapter {
	        @Override
			public void mouseClicked(MouseEvent e) {
	            JTableHeader h = (JTableHeader) e.getSource();
	            TableColumnModel columnModel = h.getColumnModel();
	            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
	            int column = columnModel.getColumn(viewColumn).getModelIndex();
	            if (column != -1) {
	                int status = getSortingStatus(column);
	                if (!e.isControlDown()) 
	                    cancelSorting();
	                // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or 
	                // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed. 
	                status = status + (e.isShiftDown() ? -1 : 1);
	                status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
	                setSortingStatus(column, status);
	            }
	        }
	    }

		//----------------------------------------------------------------------------------------------------
		private static class Arrow implements Icon {
			
			//====================================================================================================
			// members
			
	        private boolean descending;
	        private int size;
	        private int priority;

	        //====================================================================================================
			// methods
	        
	        //----------------------------------------------------------------------------------------------------
			public Arrow(boolean descending, int size, int priority) {
	            this.descending = descending;
	            this.size = size;
	            this.priority = priority;
	        }

	        //----------------------------------------------------------------------------------------------------
			public void paintIcon(Component c, Graphics g, int x, int y) {
	            Color color = c == null ? Color.GRAY : c.getBackground();             
	            // In a compound sort, make each succesive triangle 20% 
	            // smaller than the previous one. 
	            int dx = (int)(size / 2 * Math.pow(0.8,priority));
	            int dy = descending ? dx : -dx;
	            // Align icon (roughly) with font baseline. 
	            y = y + 5 * size / 6 + (descending ? -dy : 0);
	            int shift = descending ? 1 : -1;
	            g.translate(x,y);

	            // Right diagonal. 
	            g.setColor(color.darker());
	            g.drawLine(dx / 2,dy,0,0);
	            g.drawLine(dx / 2,dy + shift,0,shift);
	            
	            // Left diagonal. 
	            g.setColor(color.brighter());
	            g.drawLine(dx / 2,dy,dx,0);
	            g.drawLine(dx / 2,dy + shift,dx,shift);
	            
	            // Horizontal line. 
	            if (descending) {
	                g.setColor(color.darker().darker());
	            } else {
	                g.setColor(color.brighter().brighter());
	            }
	            g.drawLine(dx,0,0,0);

	            g.setColor(color);
	            g.translate(-x,-y);
	        }

	        //----------------------------------------------------------------------------------------------------
			public int getIconWidth() { return size; }
	        public int getIconHeight() { return size; }
	    }

	    //----------------------------------------------------------------------------------------------------
		private class SortableHeaderRenderer implements TableCellRenderer {
			
			//====================================================================================================
			// members
			
	        private TableCellRenderer tableCellRenderer;

	        //====================================================================================================
			// methods
	        
	        //----------------------------------------------------------------------------------------------------
			public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
	            this.tableCellRenderer = tableCellRenderer;
	        }

	        //----------------------------------------------------------------------------------------------------
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	            Component c = tableCellRenderer.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
	            if (c instanceof JLabel) {
	                JLabel l = (JLabel) c;
	                l.setHorizontalTextPosition(JLabel.LEFT);
	                int modelColumn = table.convertColumnIndexToModel(column);
	                l.setIcon(getHeaderRendererIcon(modelColumn,l.getFont().getSize()));
	            }
	            return c;
	        }
	    }

	    //----------------------------------------------------------------------------------------------------
		private static class Directive {
	        private int column;
	        private int direction;
	        public Directive(int column, int direction) {
	            this.column = column;
	            this.direction = direction;
	        }
		}
	}
}
