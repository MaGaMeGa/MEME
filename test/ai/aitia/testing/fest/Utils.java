package ai.aitia.testing.fest;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fest.swing.core.Robot;
import org.fest.swing.finder.JFileChooserFinder;
import org.fest.swing.finder.WindowFinder;
import org.fest.swing.fixture.DialogFixture;
import org.fest.swing.fixture.JFileChooserFixture;
import org.fest.swing.image.ScreenshotTaker;

public class Utils {
	
	private static String screenshotDir;

	static
	{
		try
		{
			screenshotDir = new File(".").getCanonicalPath()+File.separatorChar+"screenshots"+File.separatorChar;
			System.out.println(screenshotDir);
		}
		catch(IOException ioe){}
	}
	
	private static void closeFileChooser(Robot r)
    {
		JFileChooserFixture fChooser = JFileChooserFinder.findFileChooser().using(r);
    	fChooser.cancel();
    }
	protected static boolean findMissingClass(Robot r ,File... dir)
    {
    	DialogFixture classDialog;
    	
    	try
    	{
    		classDialog = WindowFinder.findDialog(CompNames.Dial.CLASSPATH).
    				withTimeout(1000).using(r);
    	}
    	catch(org.fest.swing.exception.WaitTimedOutError wtoe)
		{
    		festErr("classpathdialog not found!");
    		return false;
		}
    	
        JFileChooserFixture fChooser = JFileChooserFinder.findFileChooser(CompNames.FileChooser.CLASSPATH).using(r);
        Pattern p = Pattern.compile("(.*<b><.*>(?<file2>.*)<.*></b>.*)|(.*<b>(?<file1>.*)</b>.*)", Pattern.DOTALL);
        String mitNem = classDialog.textBox(CompNames.Fld.CLASSNOTFOUND).text();
        Matcher m = p.matcher(mitNem);
        m.matches();
        String file = m.group("file1")==null ? m.group("file2") : m.group("file1");
        file = file.replace('.', File.separatorChar);
                 
        File fPath = findFile(file,dir);
        
        if(fPath == null)
        {
        	festErr(file+" not found!");
        	fChooser.cancel();
        	return true;
        }
        
        festErr(file+" found!");
        fChooser.setCurrentDirectory(fPath.getParentFile());
        fChooser.selectFile(new File(fPath.getName()));
        fChooser.approve();
    	
        return true;
    }
	protected static void takeScreen(String fileName)
	{
		
		ScreenshotTaker sst = new ScreenshotTaker();
		sst.saveDesktopAsPng(screenshotDir+fileName);
	}
	
    protected static void popupYesOk(Robot r)
    {
    	DialogFixture d;
		try
		{
			d = getDialog(CompNames.Dial.POPUP, r);
		}
    	catch(org.fest.swing.exception.WaitTimedOutError wtoe)
		{
    		festErr("Popup window not found!");
    		return;
		}
    	try
    	{
    		d.button(CompNames.Btn.YES).click();
    	}
    	catch(org.fest.swing.exception.ComponentLookupException clue){
    	}
    	try
    	{
    		d.button(CompNames.Btn.OK).click();
    	}
    	catch(org.fest.swing.exception.ComponentLookupException clue){
    	}
    }
    
    public static File findFile(String name, File... dir)
    {
    	File valami = null;
    	
    	if(dir.length == 0)dir[0] = new File("Documents");
    	
    	for(File ff : dir)
    	{
	    	for(File f : ff.listFiles())
	    	{
	    		if(valami!=null)break;
	    		if(f.isDirectory())
	    		{
	    			valami = findFile(name, f);
	    		}
	    		
	    		if(f.isFile())
	    		{
	    			if(f.getAbsolutePath().contains(name) && !f.getAbsolutePath().contains(".svn"))
	    			{
	    				return f.getAbsoluteFile();
	    			}
	    		}
	    		
	    	}
    	}
    	
    	return valami;
    }
    
    static DialogFixture getDialog(String name, Robot r){return WindowFinder.findDialog(name).withTimeout(3000).using(r);}
    protected static void festErr(String err){System.err.println("FEST test : "+err);}
}

