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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import _.unknown;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.IScriptSupport;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.parser.ScriptParser;
import ai.aitia.meme.paramsweep.platform.IScriptChecker;
import ai.aitia.meme.paramsweep.platform.Platform;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.ReturnTypeElement;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;

public class UserVariableDialog extends JDialog implements ActionListener {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "Please define the name and type of the variable and optionally provide some initialization " +
	  											  "code.";
	
	private static final int PROBLEM_TIMEOUT = 5 * 1000;
	public static final int MESSAGE	= 0;
	public static final int WARNING	= 1;
	
	private String infoText = null;
	private Pair<String,Integer> warning = null;
	private Utils.TimerHandle warningTimer = null;

	private UserDefinedVariable returnValue = null;
	private final List<UserDefinedVariable> userVariables;
	private final IScriptSupport scriptSupport;
	private final ParameterSweepWizard wizard;
	private final List<String> imports;
		
	//====================================================================================================
	// GUI members
	
	private final JDialog owner;
	private final JPanel content = new JPanel(new BorderLayout());
	private final JTextPane infoPane = new JTextPane();
	private final JScrollPane infoPaneScr = new JScrollPane(infoPane);
	private JPanel center = null;
	private final JTextField nameField = new JTextField();
	private final JComboBox typeBox = new JComboBox();
	private final JTextArea codeArea = new JTextArea();
	private final JScrollPane codeAreaScr = new JScrollPane(codeArea);
	private final JPanel bottom = new JPanel();
	private final JButton createButton = new JButton("Create");
	private final JButton cancelButton = new JButton("Cancel");

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public UserVariableDialog(final JDialog owner, final IScriptSupport scriptSupport, final ParameterSweepWizard wizard, final List<String> imports) {
		super(owner,"Defining Variables",true);
		this.owner = owner;
		this.scriptSupport = scriptSupport;
		this.wizard = wizard;
		this.imports = imports;
		userVariables = Collections.unmodifiableList(this.scriptSupport.getUserVariables());
		layoutGUI();
		initialize();
	}
	
	//----------------------------------------------------------------------------------------------------
	public UserVariableDialog(final JDialog owner, final IScriptSupport scriptSupport, final UserDefinedVariable variable, 
							  final ParameterSweepWizard wizard, final List<String> imports, final boolean referenced) {
		this(owner,scriptSupport,wizard,imports);
		returnValue = variable;
		setGUI(referenced);
	}
	
	//----------------------------------------------------------------------------------------------------
	public UserDefinedVariable showDialog() {
		setVisible(true);
		final UserDefinedVariable result = returnValue;
		dispose();
		return result;
	}
	
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
	public boolean warning(boolean condition, String message, int level, boolean clear) {
		String before = warning == null ? null : warning.getFirst();
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
		final String cmd = e.getActionCommand();
		if ("CREATE".equals(cmd)) {
			String name = nameField.getText().trim();
			// I. name is empty
			if (warning(name == null || name.equals(""),"Variable name is empty.",WARNING,true)) return;
			// II. spaces
			if (warning(name.contains(" "),"Invalid identifier: the variable name cannot contain spaces",WARNING,true))
				return;
			// III. valid name
			if (warning(!scriptSupport.isValidVariableName(name),"Invalid identifier: the name in not a valid identifier on the platform",WARNING,
						true)) return;
			// IV. name is used
			if (warning(!isUniqueName(name),String.format("Variable name '%s' is already used in the model",name),WARNING,true)) return;

			ReturnTypeElement selectedItem = (ReturnTypeElement) typeBox.getSelectedItem();
			Class<?> returnType = selectedItem.getJavaType();
			String initCode = codeArea.getText().trim();
			final boolean noInitialization = "".equals(initCode);
			if (noInitialization)
				initCode = getDefaultInitialization(name,selectedItem);
			returnValue = new UserDefinedVariable(name,returnType,initCode);
			if (!noInitialization)
				returnValue.setDefaultInitialized(false);
			final String error = checkInitCode();
			if (error != null) {
				final String str = "Syntax error: " + error + "\n";
				warning(true,str,WARNING,false);
				return;
			}
			setVisible(false);
		} else if ("CANCEL".equals(cmd)) {
			returnValue = null;
			setVisible(false);
		} else if ("TYPE".equals(cmd)) {
			Object obj = typeBox.getSelectedItem();
			if (obj instanceof String) {
				String text = (String) obj;
				if (!warning(text.trim().equals("") || text.trim().equals("void"),"Invalid return type",WARNING,true)) {
					DefaultComboBoxModel model = (DefaultComboBoxModel) typeBox.getModel();
					Class<?> javaType = getJavaType(text);
					if (warning(javaType == null,"Unknown type. Please use the fully qualified name.",WARNING,true)) return;
					final ReturnTypeElement re = new ReturnTypeElement(text,javaType);
					if (model.getIndexOf(re) == -1) 
						model.addElement(re);
					typeBox.setSelectedItem(re);
				} else if (typeBox.getItemCount() > 0)
					typeBox.setSelectedIndex(0);
				else
					typeBox.setSelectedIndex(-1);
			} 
		}
	}
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		center = FormsUtils.build("~ p ~ p:g ~",
								  "|01||" +
								  "23||" +
								  "44 f:p:g||", 
								  "Variable name: ",nameField,
								  "Variable type: ",typeBox,
								  codeAreaScr).getPanel();
		
		bottom.add(createButton);
		bottom.add(cancelButton);
		
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoPaneScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		content.add(center,BorderLayout.CENTER);
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(bottom);
		
		content.add(tmp,BorderLayout.SOUTH);this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				returnValue = null;
			}
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		infoPaneScr.setBorder(null);
		infoPaneScr.setPreferredSize(new Dimension(400,50));
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		Utilities.setTextPane(infoPane,Utils.htmlPage(DEFAULT_MESSAGE));
		
		typeBox.setEditable(true);
		
		codeAreaScr.setBorder(BorderFactory.createTitledBorder("Initialization code (optional)"));
		codeAreaScr.setPreferredSize(new Dimension(380,200));
		codeArea.setBorder(null);
		
		createButton.setActionCommand("CREATE");
		cancelButton.setActionCommand("CANCEL");
		typeBox.setActionCommand("TYPE");
		
		GUIUtils.addActionListener(this,createButton,cancelButton,typeBox);
		
		initializeTypeBox();
		
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
		this.setLocationRelativeTo(owner);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setGUI(final boolean referenced) {
		nameField.setText(returnValue.getName());
		DefaultComboBoxModel model = (DefaultComboBoxModel) typeBox.getModel();
		ReturnTypeElement re = new ReturnTypeElement(getTypeName(returnValue.getType()),returnValue.getType());
		final int idx = model.getIndexOf(re);
		if (idx == -1) { 
			model.addElement(re);
			typeBox.setSelectedItem(re);
		} else
			typeBox.setSelectedIndex(idx);
		if (!returnValue.isDefaultInitialized())
			codeArea.setText(returnValue.getInitializationCode());
		if (referenced) {
			nameField.setEditable(false);
			typeBox.setEnabled(false);
		}
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String getTypeName(final Class<?> type) {
		if (type == null)
			throw new IllegalArgumentException("'type' is null");
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			if (type.equals(Boolean.TYPE)) 
				return "boolean";
			if (type.equals(String.class))
				return "String";
			if (type.equals(Double.TYPE))
				return "number";
			else
				return "other";
		}
		return returnValue.getType().getCanonicalName();
	}
	
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
	
	//------------------------------------------------------------------------------------
	private void initializeTypeBox() {
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		model.addElement(new ReturnTypeElement("boolean","false",Boolean.TYPE));
		model.addElement(new ReturnTypeElement("String","\"\"",String.class));
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			model.addElement(new ReturnTypeElement("byte","0",Byte.TYPE));
			model.addElement(new ReturnTypeElement("double","0.0",Double.TYPE));
			model.addElement(new ReturnTypeElement("float","0.0",Float.TYPE));
			model.addElement(new ReturnTypeElement("int","0",Integer.TYPE));
			model.addElement(new ReturnTypeElement("long","0",Long.TYPE));
			model.addElement(new ReturnTypeElement("short","0",Short.TYPE));
		} else {
			typeBox.setEditable(false);
			model.addElement(new ReturnTypeElement("number","0",Double.TYPE));
			model.addElement(new ReturnTypeElement("other","[]",unknown.class));
		}
		typeBox.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> getJavaType(final String type) {
		if (type == null || type.trim().equals("")) return null;
		final String candidate = type.trim();
		if (candidate.equals("char"))
			return Character.TYPE;
		Class<?> result = null;
		if (wizard.getClassLoader() instanceof RetryLoader)
			((RetryLoader)wizard.getClassLoader()).stopRetry();
		try {
			result = Class.forName(candidate,true,wizard.getClassLoader());
		} catch (ClassNotFoundException e) {
			try {
				result = Class.forName("java.lang." + candidate,true,wizard.getClassLoader());
			} catch (ClassNotFoundException ee) {
				// result == null 
			}
		} finally {
			if (wizard.getClassLoader() instanceof RetryLoader)
				((RetryLoader)wizard.getClassLoader()).startRetry();
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isUniqueName(final String name) {
		if (name == null || "".equals(name))
			throw new IllegalArgumentException("'name' is invalid");
		
		for (final UserDefinedVariable variable : userVariables) {
			if (name.equals(variable.getName())) return false;
		}
		
		final List<MemberInfo> all = new ArrayList<MemberInfo>(scriptSupport.getAllMembers());
		all.addAll(scriptSupport.getInnerScripts());
		for (final MemberInfo mi : all) {
			final String miName = mi.getName();
			if (name.equals(miName)) return false;
		}
		
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getDefaultInitialization(final String name, final ReturnTypeElement type) {
		return scriptSupport.getDefaultInitialization(name,type.getDefaultValue());
	}
	
	//----------------------------------------------------------------------------------------------------
	private String checkInitCode() {
		if (PlatformSettings.getPlatformType() == PlatformType.REPAST || PlatformSettings.getPlatformType() == PlatformType.CUSTOM
				|| PlatformSettings.getPlatformType() == PlatformType.SIMPHONY2) {
			final File f = new File(wizard.getModelFileName());
			CtClass	clazz = null;
			try {
				final InputStream ins = new FileInputStream(f);
				clazz = wizard.getClassPool().makeClass(ins);
				clazz.stopPruning(true);
				ins.close();
			} catch (IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			} finally {
				if (clazz != null)
					clazz.defrost();
			}
			final ScriptParser parser = new ScriptParser(wizard.getClassPool(),clazz,wizard.getClassLoader(),new ArrayList<GeneratedMemberInfo>(),
														 null);
			return parser.checkVariableInitCode(returnValue,imports);
		} else {
			Platform platform = PlatformManager.getPlatform(PlatformSettings.getPlatformType());
			if (platform instanceof IScriptChecker) {
				IScriptChecker _platform = (IScriptChecker) platform;
				List<String> errors = _platform.checkVariable(returnValue,wizard);
				if (errors.size() > 0)
					return Utils.join(errors,"\n");
			}
			return null;
		}
	}
}
