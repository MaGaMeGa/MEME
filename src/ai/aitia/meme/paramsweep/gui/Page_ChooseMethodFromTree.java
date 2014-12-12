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


public class Page_ChooseMethodFromTree {}
//extends JPanel implements IWizardPage,
//																 IArrowsInHeader,
//		        												 TreeSelectionListener {
//	//====================================================================================================
//	// members
//	
//    private static final long serialVersionUID = 863894276442799487L;
//
//    /** The owner of the page. */
//	private ParameterSweepWizard owner = null;
//	private Vector<IntelliSweepPluginDescriptor> methods = null;
//
//	//====================================================================================================
//	// GUI members:
//	
//	private JPanel content = null;
//	//private JList methodList = null;
//	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
//	private JTree methodTree = new JTree(root);
//	private DefaultMethodCategorization categorization;
//	private JTextArea descriptionArea = null;
//	
//	//====================================================================================================
//	// methods
//	
//	//----------------------------------------------------------------------------------------------------
//	public Page_ChooseMethodFromTree(ParameterSweepWizard owner){
//		this.owner = owner;
//		methods = new Vector<IntelliSweepPluginDescriptor>();
//		categorization = new DefaultMethodCategorization();
//		methods.add(new IntelliSweepPluginDescriptor(null, true));//this is the manual method
//		categorization.putMethodToCategorization( methods.get( 0 ) );
//		PluginList<IIntelliStaticMethodPlugin> statics = ParameterSweepWizard.getPluginManager().getIntelliStaticPluginInfos();
//		PluginList<IIntelliDynamicMethodPlugin> dynamics = ParameterSweepWizard.getPluginManager().getIntelliDynamicPluginInfos();
//		for (PluginInfo<IIntelliStaticMethodPlugin> info : statics) {
//			IntelliSweepPluginDescriptor desc = 
//				new IntelliSweepPluginDescriptor(info.getInstance(),true);
//	        methods.add(desc);
//			categorization.putMethodToCategorization( desc );
//		}
//		for (PluginInfo<IIntelliDynamicMethodPlugin> info : dynamics){ 
//			IntelliSweepPluginDescriptor desc = 
//				new IntelliSweepPluginDescriptor(info.getInstance(),false);
//	        methods.add(desc);
//	        categorization.putMethodToCategorization( desc );
//		}
//		layoutGUI();
//	}
//	
//	//----------------------------------------------------------------------------------------------------
//	public int getSweepingMethodID() {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null) return 0;
//		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//		if( node.getUserObject() instanceof IntelliSweepPluginDescriptor ){
//			IntelliSweepPluginDescriptor desc = 
//					(IntelliSweepPluginDescriptor)node.getUserObject();
//			if ( desc != null && desc.getPlugin() != null)//if it is an IntelliSweep method
//				return desc.getMethodType();
//			else
//				return 0;
//		}
//
//		return -1;
//	}
//	
//	//----------------------------------------------------------------------------------------------------
//	public IIntelliStaticMethodPlugin getSelectedIntelliStaticMethodPlugin() {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null) return null;
//		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//		IntelliSweepPluginDescriptor desc = 
//				(IntelliSweepPluginDescriptor)node.getUserObject();
//	    return (IIntelliStaticMethodPlugin) desc.getPlugin();
//    }
//
//	//----------------------------------------------------------------------------------------------------
//	public IIntelliDynamicMethodPlugin getSelectedIntelliDynamicMethodPlugin() {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null) return null;
//		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//		IntelliSweepPluginDescriptor desc = 
//				(IntelliSweepPluginDescriptor)node.getUserObject();
//	    return (IIntelliDynamicMethodPlugin) desc.getPlugin();
//    }
//
//	//----------------------------------------------------------------------------------------------------
//	public void setSelectedMethodByLocalizedName(String localizedName){
//		Enumeration preorder = categorization.getCategorizationRoot().preorderEnumeration();
//		TreePath path = null;
//		
//		while( preorder.hasMoreElements() ){
//			DefaultMutableTreeNode node = (DefaultMutableTreeNode)preorder.nextElement();
//			if( node.getUserObject() instanceof IntelliSweepPluginDescriptor ){
//				IntelliSweepPluginDescriptor desc = 
//					(IntelliSweepPluginDescriptor)(node.getUserObject());
//				if( desc != null && localizedName.equals( desc.getLocalizedName() ) ){
//					path = new TreePath( node.getPath() );
//					break;
//				}	
//			}
//		}
//		
//		if( path != null )
//			methodTree.setSelectionPath( path );
//		else
//			methodTree.setSelectionPath( new TreePath( new Object[]{ categorization.getCategorizationRoot() } ) );
//	}
//
//	//----------------------------------------------------------------------------------------------------
//	public String getSweepingMethodTitleName() {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null) return null;
//		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//		IntelliSweepPluginDescriptor desc = 
//				(IntelliSweepPluginDescriptor)node.getUserObject();
//	    return desc.toString();
//    }
//	
//	//====================================================================================================
//	// implemented interfaces
//	
//	//----------------------------------------------------------------------------------------------------
//	public String getInfoText(Wizard w) { return w.getArrowsHeader("Select a method to investigate the model and its parameters"); }
//	public Container getPanel() { return this; }
//	public void onPageChange(boolean show) {}
//	public String getTitle() { return "Method selection"; }
//
//	//----------------------------------------------------------------------------------------------------
//	public boolean isEnabled(Button b) {
//		if (b == Button.NEXT || b == Button.FINISH || b == Button.CUSTOM){
//			if (getSweepingMethodID() > 0){
//				IIntelliMethodPlugin plugin = getSweepingMethodID() == IIntelliMethodPlugin.STATIC_METHOD ? getSelectedIntelliStaticMethodPlugin()
//																										  : getSelectedIntelliDynamicMethodPlugin();
//				return plugin.isImplemented();
//			}else if( getSweepingMethodID() < 0 ){
//				return false;
//			}
//		}
//		return true;
//	}
//
//	//----------------------------------------------------------------------------------------------------
//	public boolean onButtonPress(Button b) {
//		if(getSweepingMethodID() > 0 && b == Button.NEXT)
//			owner.gotoPage(3);
//		return true;
//	}
//
//	//----------------------------------------------------------------------------------------------------
///*	public void valueChanged(ListSelectionEvent e) {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null){
//			descriptionArea.setText(methods.get(0).getDescription());
//		}else{
//			DefaultMutableTreeNode node = 
//					(DefaultMutableTreeNode) path.getLastPathComponent();
//			IntelliSweepPluginDescriptor desc = 
//					(IntelliSweepPluginDescriptor)node.getUserObject();
//			descriptionArea.setText(desc.getDescription());
//		}
//		owner.enableDisableButtons();
//    }*/
//	
//	public void valueChanged(TreeSelectionEvent arg0) {
//		TreePath path = methodTree.getSelectionPath();
//		if (path == null){
//			descriptionArea.setText(methods.get(0).getDescription());
//		}else{
//			DefaultMutableTreeNode node = 
//					(DefaultMutableTreeNode) path.getLastPathComponent();
//			if( node.getUserObject() instanceof IntelliSweepPluginDescriptor ){
//				IntelliSweepPluginDescriptor desc = 
//						(IntelliSweepPluginDescriptor)node.getUserObject();
//				if( desc != null )
//					descriptionArea.setText(desc.getDescription());
//			}else if( node.getUserObject() instanceof MethodCategory ){
//				MethodCategory cat = (MethodCategory)node.getUserObject();
//				if( cat != null )
//				descriptionArea.setText(cat.getDescription());
//			}
//		}
//		owner.enableDisableButtons();
//	}
//	
//	//====================================================================================================
//	// GUI methods
//	
//	//----------------------------------------------------------------------------------------------------
//	private void layoutGUI() {
//		//methodList = new JList(methods);
//		//methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		methodTree = new JTree( categorization.getCategorizationRoot() );
//		descriptionArea = new JTextArea();
//		descriptionArea.setEditable(false);
//		descriptionArea.setLineWrap(true);
//		descriptionArea.setWrapStyleWord(true);
//		descriptionArea.setColumns(40);
//		JScrollPane scrollPane = new JScrollPane(descriptionArea);
//		scrollPane.setBorder(BorderFactory.createEtchedBorder());
//		//methodList.addListSelectionListener(this);
//		methodTree.addTreeSelectionListener( this );
//		//methodList.setSelectedIndex(0);
//		TreePath path = new TreePath( new Object[]{ categorization.getCategorizationRoot() } );
//		methodTree.setSelectionPath(path);
//		//methodList.setPreferredSize(new Dimension(270, 300));
//		methodTree.setPreferredSize(new Dimension(270, 300));
//		//methodList.setBorder(BorderFactory.createEtchedBorder());
//		methodTree.setBorder(BorderFactory.createEtchedBorder());
//		content = FormsUtils.build("p ~ f:p:g", 
//								   "[DialogBorder]01 p|" +
//								   				 "23 f:p:g", 
//								   "IntelliSweep methods:", "Description:",
//								   methodTree, descriptionArea).getPanel();
//		this.setLayout(new BorderLayout());
//		final JScrollPane sp = new JScrollPane(content);
//    	this.add(sp,BorderLayout.CENTER);
//    	this.validate();
//    }
//}
