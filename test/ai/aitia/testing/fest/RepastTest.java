package ai.aitia.testing.fest;


import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.tree.TreeModel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.EmergencyAbortListener;
import org.fest.swing.core.Robot;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JComboBoxFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JMenuItemFixture;
import org.fest.swing.fixture.JPanelFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JTreeFixture;
import org.fest.swing.junit.v4_5.runner.GUITestRunner;
import org.fest.swing.keystroke.KeyStrokeMap;
import org.fest.swing.keystroke.KeyStrokeMappingProvider;
import org.fest.swing.keystroke.KeyStrokeMappingsParser;
import org.fest.swing.launcher.ApplicationLauncher;
import org.fest.swing.timing.Pause;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.paramsweep.plugin.gui.CollectionParameterGUI;
import ai.aitia.meme.paramsweep.plugin.gui.NumberParameterGUI;

@RunWith(GUITestRunner.class)
public class RepastTest{

    private static EmergencyAbortListener mEmergencyAbortListener;
    private static FrameFixture mFrame;
    private DialogFixture wizardDialog;
    private static Thread t;
    private static Robot robot;
    private static String projectPath;
    private Random r = new Random();

    
    @BeforeClass
	public static void setUpClass() {

    	try
		{
			projectPath = new File(".").getCanonicalPath();
			FileWriter fw = new FileWriter (new File("log.txt"));
			fw.close();
		}
		catch(IOException ioe){}
		

		mEmergencyAbortListener = EmergencyAbortListener.registerInToolkit();
		robot = BasicRobot.robotWithNewAwtHierarchy();
		robot.settings().delayBetweenEvents(200);
		t = new Thread(new Runnable() {
			public void run() {
				ApplicationLauncher.application(MEMEApp.class).start();
			}
		});
		t.start();

		mFrame = WindowFinder.findFrame("wnd_main").withTimeout(10000)
				.using(robot);
				
		Pause.pause(10000);

		KeyStrokeMappingsParser parser = new KeyStrokeMappingsParser();
		KeyStrokeMappingProvider provider = parser.parse(new File(
				"test/ai/aitia/testing/fest/KeyMap.txt"));
		KeyStrokeMap.addKeyStrokesFrom(provider);

		registerPlatforms();
	}

	private static void registerPlatforms() {

		Pause.pause(1500);
		mFrame.button(CompNames.Btn.Main.RUN).click();		
		DialogFixture wd = WindowFinder.findDialog(CompNames.Dial.WIZARD)
				.using(robot);

		String sep = File.separator;
		wd.button(CompNames.Btn.Platforms.REGISTER).click();
		DialogFixture prefDial = Utils.getDialog(CompNames.Dial.PREFERENCES, robot);
		JTreeFixture jtf = prefDial.tree(CompNames.Tree.Preferences.PREFTREE);
		
		jtf.selectPath("/General");
		prefDial.button(CompNames.Btn.Preferences.BROWSE).click();
		JFileChooserFixture jfc = JFileChooserFinder.findFileChooser().using(
				robot);
		jfc.setCurrentDirectory(new File(projectPath
				+sep+ "wizardsettings"));
		jfc.approve();
		
		jtf.selectPath("/Platforms/Repast Simphony 2.0");
		prefDial.button(CompNames.Btn.Preferences.BROWSE).click();
		jfc = JFileChooserFinder.findFileChooser().using(
				robot);
		jfc.setCurrentDirectory(new File(projectPath
				+sep+"Platforms"+sep+"Simphony2.0"));
		jfc.approve();
		prefDial.button(CompNames.Btn.Preferences.REGISTER).click();

		jtf.selectPath("/Platforms/NetLogo-5");
		prefDial.button(CompNames.Btn.Preferences.BROWSE).click();
		jfc = JFileChooserFinder.findFileChooser().using(robot);
		jfc.setCurrentDirectory(new File(projectPath
				+sep+ "Platforms"+sep+"NetLogo5.0.3"));
		jfc.approve();
		prefDial.button(CompNames.Btn.Preferences.REGISTER).click();

		jtf.selectPath("/Platforms/MASON");
		prefDial.button(CompNames.Btn.Preferences.REGISTER).click();

		prefDial.button(CompNames.Btn.OK).click();
		
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		mEmergencyAbortListener.unregister();
	}


    
	//@Ignore
    @GUITest
    @Test
    public void testRepast()
    {
    	Pause.pause(1000);
       	wizardDialog = WindowFinder.findDialog(CompNames.Dial.WIZARD).using(robot);
       	
       	
       	int methodIndex = setModel("repast", "PrisonersModel.class","factorial", true);
       	if(methodIndex==0)setParameters(true);
        setRecorders(true);
        
		
		if(methodIndex == 0)
		{
			wizardDialog.button(CompNames.Btn.FINISH).click();
			
		}
		
		else
		{
			wizardDialog.button(CompNames.Btn.NEXT).click();
			testFactorial();
		}
		
		while(Utils.findMissingClass(robot, new File("PlatformPlugins"), new File("Documents")));
		//DialogFixture repastDialog = Utils.getDialog(CompNames.Dial.REPASTIMPORT);
		DialogFixture repastDialog = WindowFinder.findDialog(CompNames.Dial.REPASTIMPORT).withTimeout(30,TimeUnit.MINUTES).using(robot);
		Pause.pause(1000);
        repastDialog.textBox(CompNames.Fld.RepastImport.MODELNAME).deleteText();
        repastDialog.textBox(CompNames.Fld.RepastImport.MODELNAME).enterText("IPD model test");
        
        repastDialog.textBox(CompNames.Fld.RepastImport.BATCHNAME).deleteText();
        repastDialog.textBox(CompNames.Fld.RepastImport.BATCHNAME).enterText("proba123");
        
        JButtonFixture outputButton = repastDialog.button(CompNames.Btn.RepastImport.OUTPUT);
		
        while(true)
        {
        	try{
        		outputButton.requireEnabled();
        		outputButton.click();
        	}
        	catch(java.lang.AssertionError ae)
        	{
        		System.err.println("FEST test : output button disabled");
        		break;
        	}
        }
        repastDialog.button(CompNames.Btn.OK).click();
        Utils.popupYesOk(robot);
        if(methodIndex!=0)
        {
	        DialogFixture ipDialog = Utils.getDialog(CompNames.Dial.INTELLIPROCESS, robot);
	    	ipDialog.button(CompNames.Btn.CANCEL).click();
	        Utils.popupYesOk(robot);
        }
}

    
    private int setModel(String platformName, String modelName,String methodName , boolean skip)
    {
    	wizardDialog.list(CompNames.Lst.Platforms.PLATFORMLIST).selectItem(Pattern.compile(".*"+platformName+".*",Pattern.CASE_INSENSITIVE));
    	wizardDialog.button(CompNames.Btn.NEXT).click();
        Utils.popupYesOk(robot);
        wizardDialog.button(CompNames.Btn.Models.BROWSE).click();
        File f = Utils.findFile(modelName,new File("Documents"));
        JFileChooserFixture fChooser = JFileChooserFinder.findFileChooser(CompNames.FileChooser.PARAMSWEEP).using(robot);
        fChooser.setCurrentDirectory(new File(f.getParent()));
        fChooser.selectFile(new File(f.getName()));
        fChooser.approve();
        
        while(Utils.findMissingClass(robot, new File("PlatformPlugins")));
        
      
        if(skip)
    	{
    		wizardDialog.button(CompNames.Btn.NEXT).click();
    	}
        else
        {
	        JListFixture jlf = wizardDialog.list(CompNames.Lst.Models.CLASSPATH);
	        
	        jlf.selectItem(r.nextInt(jlf.contents().length-1));
	        wizardDialog.button(CompNames.Btn.Models.DOWN).click();
	        jlf.selectItem(r.nextInt(jlf.contents().length-1));
	        wizardDialog.button(CompNames.Btn.Models.UP).click();
	        jlf.selectItem(r.nextInt(jlf.contents().length-1));
	        wizardDialog.button(CompNames.Btn.Models.BOTTOM).click();
	        jlf.selectItem(r.nextInt(jlf.contents().length-1));
	        wizardDialog.button(CompNames.Btn.Models.TOP).click();
	        jlf.selectItem(r.nextInt(jlf.contents().length-1));
	        wizardDialog.button(CompNames.Btn.Models.DOWN).click();
	        
	        jlf.selectItem(jlf.contents().length-1);
	        wizardDialog.button(CompNames.Btn.Models.DOWN).click();
	        jlf.selectItem(jlf.contents().length-1);
	        wizardDialog.button(CompNames.Btn.Models.BOTTOM).click();
	        
	        jlf.selectItem(0);
	        wizardDialog.button(CompNames.Btn.Models.UP).click();
	        jlf.selectItem(0);
	        wizardDialog.button(CompNames.Btn.Models.TOP).click();
    		wizardDialog.button(CompNames.Btn.NEXT).click();
        }
        
        wizardDialog.list(CompNames.Lst.Methods.METHOD).
        	selectItem(Pattern.compile(".*".concat(methodName).concat(".*"),Pattern.CASE_INSENSITIVE));
        int methodIndex = wizardDialog.list(CompNames.Lst.Methods.METHOD).component().getSelectedIndex();
        wizardDialog.button(CompNames.Btn.NEXT).click();
    	return methodIndex;
    }
    private void setRecorders(boolean skip)
    {
    	if(skip)
    	{
    		wizardDialog.button(CompNames.Btn.Recorders.ADDRECORDER).click();
    		wizardDialog.list(CompNames.Lst.Recorders.VARIABLES).
    			selectItem(Pattern.compile(".*int.*|.*String.*|.*boolean.*|.*long.*|.*double.*"));
            wizardDialog.button(CompNames.Btn.Recorders.ADD).click();
    		return;
    	}
    	wizardDialog.button(CompNames.Btn.Recorders.ADDRECORDER).click();
        wizardDialog.tree(CompNames.Tree.Recorders.RECORDERTREE).showPopupMenu().menuItem(CompNames.Btn.Recorders.CMENUREMOVE).click();
        Utils.popupYesOk(robot);
        
        wizardDialog.tree(CompNames.Tree.Recorders.RECORDERTREE).showPopupMenu().menuItem(CompNames.Btn.Recorders.CMENUNEW).click();
     
        
        wizardDialog.radioButton(CompNames.RBtn.Recorders.STOPCONDITION).click();
        
        wizardDialog.button(CompNames.Btn.Recorders.STOPEXTENDED).click();
        
        DialogFixture eeditor = WindowFinder.findDialog(CompNames.Dial.EEDITOR).using(robot);
        eeditor.textBox(CompNames.Fld.EEditor.TEXT).deleteText().enterText("false");
        eeditor.button(CompNames.Btn.OK).click();
        
        wizardDialog.radioButton(CompNames.RBtn.Recorders.STOPITERATION).click();
        
        wizardDialog.radioButton(CompNames.RBtn.Recorders.WHENCONDITION).click();
        wizardDialog.button(CompNames.Btn.Recorders.WHENEXTENDED).click();
        
        eeditor = WindowFinder.findDialog(CompNames.Dial.EEDITOR).using(robot);
        eeditor.textBox(CompNames.Fld.EEditor.TEXT).deleteText().enterText("false");
        eeditor.button(CompNames.Btn.OK).click();
        
        wizardDialog.button(CompNames.Btn.Recorders.ADVANCED).click();
        DialogFixture wtf = WindowFinder.findDialog(CompNames.Dial.WTF).using(robot);
        wtf.radioButton(CompNames.RBtn.WriteToFile.EVERYRECORDING).click();
        wtf.button(CompNames.Btn.CANCEL).click();
        
        wizardDialog.textBox(CompNames.Fld.Recorders.STOP).deleteText();
        wizardDialog.textBox(CompNames.Fld.Recorders.STOP).setText("100");
        wizardDialog.textBox(CompNames.Fld.Recorders.OUTPUT).deleteText();
        wizardDialog.textBox(CompNames.Fld.Recorders.OUTPUT).setText("IPD.out");
        
        wizardDialog.radioButton(CompNames.RBtn.Recorders.ENDITERATION).click();
        
        wizardDialog.button(CompNames.Btn.Recorders.ADDRECORDER).click();
        
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(0);
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(1);
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(2);
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(3);
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(0);
        
        wizardDialog.list(CompNames.Lst.Recorders.VARIABLES).showPopupMenu().menuItem(CompNames.Btn.CMENUSELECTALL).click();
        wizardDialog.list(CompNames.Lst.Recorders.VARIABLES).selectItems(0,1);
        wizardDialog.button(CompNames.Btn.Recorders.ADD).click();
        
        wizardDialog.tabbedPane(CompNames.Pane.Recorders.RECORDABLES).selectTab(2);
        wizardDialog.button(CompNames.Btn.Recorders.CREATE).click();

        testDataSourceGeneratedGUI();

        JListFixture scriptList = wizardDialog.list(CompNames.Lst.Recorders.SCRIPTS);
        //scriptList.selectItems(Range.from(0),Range.to(scriptList.component().getModel().getSize()-1));
        scriptList.selectItem(0);
        wizardDialog.button(CompNames.Btn.Recorders.ADD).click();
    }
    
    
    
    @SuppressWarnings("unused")
	private void randomTestDataSourceGeneratedGUI()
    {
    	DialogFixture createData = Utils.getDialog(CompNames.Dial.DATASOURCE, robot);
    	JListFixture statList = createData.list(CompNames.Lst.DataSource.STATS);
    	statList.selectItem(r.nextInt(statList.component().getModel().getSize()-1));
    	final JPanel gg = createData.panel(CompNames.Pane.DataSource.GENERATEDGUI).component();
    	    	    	
    	int cpgs =0;
    	LinkedList<NumberParameterGUI> npgs =new LinkedList<NumberParameterGUI>();
    	
    	for(Component c : gg.getComponents())
    	{
    		if(c instanceof CollectionParameterGUI)cpgs++;
    		if(c instanceof NumberParameterGUI)npgs.add((NumberParameterGUI)c);
    	}
    	
    	for(NumberParameterGUI npg : npgs)
    	{
    		JComboBoxFixture jcbf = createData.
    				comboBox(CompNames.CBox.DataSource.NAME.concat(npg.getParameterName().toLowerCase()));
    		jcbf.selectItem(r.nextInt(jcbf.component().getModel().getSize()-1));
    	}
    	
    	for(int i=0;i<cpgs;i++)
    	{
    		createData.textBox(CompNames.Fld.DataSource.CONST.concat(Integer.toString(i))).
    			deleteText().enterText(Integer.toString(r.nextInt()));
    		createData.button(CompNames.Btn.DataSource.CONST.concat(Integer.toString(i))).click();
    		JTreeFixture jtf = createData.tree
    				(CompNames.Tree.DataSource.PARAMTREE.concat(Integer.toString(i)));
    		TreeModel tm = jtf.component().getModel();
    		int childCount = jtf.component().getRowCount();
    		
    		
    		int row=0;
    		int offset=0;
    		while(row<tm.getChildCount(tm.getRoot()))
    		{
    			Object child = tm.getChild(tm.getRoot(), row);
    			if(!tm.isLeaf(child))
    			{
    				jtf.expandRow(row+offset+1);
    				offset+=tm.getChildCount(child);
    			}
				row++;
    		}
    		
    		
    		for(int j=0;j<r.nextInt(5)+5;j++)
    		{
    			jtf.selectRow(r.nextInt(childCount-2)+1);
    			createData.button(CompNames.Btn.DataSource.ADD.concat(Integer.toString(i))).click();
    		}
    	}
    	createData.button(CompNames.Btn.OK).click();
    	
    }
    public void testDataSourceGeneratedGUI()
    {
    	DialogFixture createData = Utils.getDialog(CompNames.Dial.DATASOURCE, robot);
    	JListFixture opList = createData.list(CompNames.Lst.DataSource.OPS);
    	JListFixture statList = createData.list(CompNames.Lst.DataSource.STATS);
    	opList.selectItem("Timeseries");
    	JPanelFixture gg = createData.panel(CompNames.Pane.DataSource.GENERATEDGUI);
    	gg.comboBox().selectItem(Pattern.compile(".*payoff1.*"));
    	createData.textBox(CompNames.Fld.DataSource.NAME).deleteText().enterText("timeseriesPayoff1");
    	createData.button(CompNames.Btn.OK).click();
    	
    	statList.selectItem(Pattern.compile("Standard deviation"));
    	gg=createData.panel(CompNames.Pane.DataSource.GENERATEDGUI);
    	JTreeFixture jtf = gg.tree(CompNames.Tree.DataSource.PARAMTREE.concat("0"));
    	jtf.selectRow(jtf.component().getModel().getChildCount(jtf.component().getModel().getRoot()));
    	createData.textBox(CompNames.Fld.DataSource.NAME).deleteText().enterText("standardDeviationPayoff1");
    	gg.button(CompNames.Btn.DataSource.ADD.concat("0")).click();
    	createData.button(CompNames.Btn.OK).click();
    	
    	createData.button(CompNames.Btn.CANCEL).click();
    	
    }
    
    
    
    private void testFactorial()
    {
    	wizardDialog.list(CompNames.Lst.Factorial.PARAMS)
    		.selectItem(Pattern.compile(".*strat1.*", Pattern.CASE_INSENSITIVE));
    	wizardDialog.button(CompNames.Btn.Factorial.ADD).click();
    	wizardDialog.textBox(CompNames.Fld.Factorial.HIGH).deleteText().enterText("4");
    	wizardDialog.textBox(CompNames.Fld.Factorial.LOW).deleteText().enterText("2");
    	wizardDialog.button(CompNames.Btn.Factorial.SETLEVEL).click();
    	wizardDialog.list(CompNames.Lst.Factorial.PARAMS)
    		.selectItem(Pattern.compile(".*strat2.*", Pattern.CASE_INSENSITIVE));
    	wizardDialog.button(CompNames.Btn.Factorial.ADD).click();
    	wizardDialog.textBox(CompNames.Fld.Factorial.HIGH).deleteText().enterText("4");
    	wizardDialog.textBox(CompNames.Fld.Factorial.LOW).deleteText().enterText("2");
    	wizardDialog.button(CompNames.Btn.Factorial.SETLEVEL).click();
    	wizardDialog.list(CompNames.Lst.Factorial.PARAMS)
    	.selectItem(Pattern.compile(".*both.*", Pattern.CASE_INSENSITIVE));
    	wizardDialog.button(CompNames.Btn.Factorial.ADD).click();
    	wizardDialog.textBox(CompNames.Fld.Factorial.HIGH).deleteText().enterText("4");
    	wizardDialog.textBox(CompNames.Fld.Factorial.LOW).deleteText().enterText("2");
    	wizardDialog.button(CompNames.Btn.Factorial.SETLEVEL).click();
    	
    	wizardDialog.checkBox(CompNames.CBox.Factorial.FRACTIONAL).check();
    	wizardDialog.button(CompNames.Btn.FINISH).click();
    	
    }
    
    private void setParameters(boolean skip)
    {
    	
    	if(skip)
    	{
    		wizardDialog.button(CompNames.Btn.NEXT).click();
    		return;
    	}
    	
    	JTreeFixture tree = wizardDialog.tree(CompNames.Tree.Parameters.PARAMTREE);
        JButtonFixture cancelButton = wizardDialog.button(CompNames.Btn.CANCEL);
        JButtonFixture modifyButton = wizardDialog.button(CompNames.Btn.Parameters.MODIFY);
        
        tree.selectRow(2);
        wizardDialog.button(CompNames.Btn.Parameters.DOWN).click().click().click().click();
        wizardDialog.button(CompNames.Btn.Parameters.UP).click().click().click().click();
        
        tree.showPopupMenuAt(1).menuItem(CompNames.Btn.Parameters.CMENUEDIT).click();
        wizardDialog.radioButton(CompNames.RBtn.Parameters.CONST).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).enterText("1").pressKey('\n');
        
        tree.selectRow(2);
        wizardDialog.button(CompNames.Btn.Parameters.EDIT).click();
        wizardDialog.radioButton(CompNames.RBtn.Parameters.CONST).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).enterText("-3");
        modifyButton.click();
        
        tree.showPopupMenuAt(4).menuItem(CompNames.Btn.Parameters.CMENUEDIT).click();
        wizardDialog.radioButton(CompNames.RBtn.Parameters.INCREMENT).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.START).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.START).enterText("1");
        wizardDialog.textBox(CompNames.Fld.Parameters.END).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.END).enterText("15");
        wizardDialog.textBox(CompNames.Fld.Parameters.STEP).deleteText(); 
        wizardDialog.textBox(CompNames.Fld.Parameters.STEP).enterText("1");
        wizardDialog.pressKey('\n');
        
        tree.showPopupMenuAt(5).menuItem(CompNames.Btn.Parameters.CMENUEDIT).click();
        wizardDialog.radioButton(CompNames.RBtn.Parameters.INCREMENT).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.START).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.START).enterText("0");
        wizardDialog.textBox(CompNames.Fld.Parameters.END).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.END).enterText("4");
        wizardDialog.textBox(CompNames.Fld.Parameters.STEP).deleteText(); 
        wizardDialog.textBox(CompNames.Fld.Parameters.STEP).enterText("1").pressKey('\n');
        
        tree.selectRow(6);
        wizardDialog.button(CompNames.Btn.Parameters.EDIT).click();
        wizardDialog.radioButton(CompNames.RBtn.Parameters.LIST).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.LIST).deleteText().enterText("0 1 2 3 4");
        modifyButton.click();
        
        tree.doubleClickRow(7);
        wizardDialog.radioButton(CompNames.RBtn.Parameters.CONST).click();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).deleteText();
        wizardDialog.textBox(CompNames.Fld.Parameters.CONST).enterText("-3");
        modifyButton.click();
        
       
        tree.drag(6);
        tree.drop(5);
        tree.drag(4);
        tree.drop(5);
        
    	wizardDialog.button(CompNames.Btn.NEXT).click();
    }
    protected static Robot getRobot(){return robot;}
}
