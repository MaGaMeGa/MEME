package ai.aitia.meme.utils;

import java.awt.Component;
import java.awt.HeadlessException;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.UIManager;

public class TestableDialog extends JComponent implements Accessible
{
	
	
	public static int showConfirmDialog(Component parentComponent,
        Object message, String name) throws HeadlessException {
        return showConfirmDialog(parentComponent, message,
                                 UIManager.getString("OptionPane.titleText"),
                                 JOptionPane.YES_NO_CANCEL_OPTION, name);
    }
	
	public static int showConfirmDialog(Component parentComponent,
        Object message, String title, int optionType, String name)
        throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType,
                                 JOptionPane.QUESTION_MESSAGE, name);
    }
	
	public static int showConfirmDialog(Component parentComponent,
        Object message, String title, int optionType, int messageType, String name)
        throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType,
                                messageType, null, name);
    }
	
	public static int showConfirmDialog(Component parentComponent,
        Object message, String title, int optionType,
        int messageType, Icon icon, String name) throws HeadlessException {
        return showOptionDialog(parentComponent, message, title, optionType,
                                messageType, icon, null, null, name);
    }
	
	public static String showInputDialog(Object message, Object initialSelectionValue, String name) {
        return showInputDialog(null, message, initialSelectionValue, name);
    }
	
	public static String showInputDialog(Component parentComponent,
        Object message, String name) throws HeadlessException {
        return showInputDialog(parentComponent, message, UIManager.getString(
        "OptionPane.inputDialogTitle", getLocale(parentComponent)), JOptionPane.QUESTION_MESSAGE, name);
    }
	
	public static String showInputDialog(Component parentComponent, Object message,
		Object initialSelectionValue, String name) {
		return (String)showInputDialog(parentComponent, message,
		UIManager.getString("OptionPane.inputDialogTitle",
		getLocale(parentComponent)), JOptionPane.QUESTION_MESSAGE, null, null,
		initialSelectionValue, name);
	}
	
	public static String showInputDialog(Component parentComponent,
	        Object message, String title, int messageType, String name)
	        throws HeadlessException {
	        return (String)showInputDialog(parentComponent, message, title,
	                                       messageType, null, null, null, name);
	    }
	
	public static Object showInputDialog(Component parentComponent,Object message, String title, int messageType, Icon icon,
            Object[] selectionValues, Object initialSelectionValue, String name)
	{
		JOptionPane pane = new JOptionPane(message, messageType,
				JOptionPane.OK_CANCEL_OPTION, icon,null,null);

		pane.setWantsInput(true);
		pane.setSelectionValues(selectionValues);
		pane.setInitialSelectionValue(initialSelectionValue);
		pane.setComponentOrientation(parentComponent.getComponentOrientation());
		int style = styleFromMessageType(messageType);
		JDialog dialog = pane.createDialog(parentComponent, title);
		pane.selectInitialValue();

		Component[] gombok = ((JPanel)pane.getComponents()[1]).getComponents();
		gombok[0].setName(name.concat("btn_ok"));
		gombok[1].setName(name.concat("btn_cancel"));
		
		//dialog.setName(name);
		dialog.setName("popupdialog");
		dialog.setVisible(true);
		
		
		
		dialog.dispose();
		return pane.getInputValue();
		
	}
	
	public static void showMessageDialog(Component parentComponent,
	        Object message, String name) throws HeadlessException {
		
        	

	        showMessageDialog(parentComponent, message, UIManager.getString(
	                    "OptionPane.messageDialogTitle", getLocale(parentComponent)),
	                    JOptionPane.INFORMATION_MESSAGE,name);
	    }
	
	public static void showMessageDialog(Component parentComponent,
	        Object message, String title, int messageType, String name)
	        throws HeadlessException {
	        showMessageDialog(parentComponent, message, title, messageType, null, name);
	    }
	
	public static void showMessageDialog(Component parentComponent,
	        Object message, String title, int messageType, Icon icon, String name)
	        throws HeadlessException {
	        showOptionDialog(parentComponent, message, title, JOptionPane.DEFAULT_OPTION,
	                         messageType, icon, null, null, name);
	    }
	
	public static int showOptionDialog(Component parentComponent,
	        Object message, String title, int optionType, int messageType,
	        Icon icon, Object[] options, Object initialValue, String name)
	        throws HeadlessException {
	        JOptionPane             pane = new JOptionPane(message, messageType,
	                                                       optionType, icon,
	                                                       options, initialValue);

	        pane.setInitialValue(initialValue);
	        pane.setComponentOrientation(((parentComponent == null) ?
	            JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());
	        
	        Component[] gombok = ((JPanel)pane.getComponents()[1]).getComponents();
	        
	        switch(optionType)
	        {
	        	case JOptionPane.OK_CANCEL_OPTION:
	        		gombok[0].setName("btn_ok");
	        		gombok[1].setName("btn_cancel");
	        		break;
	        	case JOptionPane.YES_NO_CANCEL_OPTION:
	        		gombok[0].setName("btn_yes");
	        		gombok[1].setName("btn_no");
	        		gombok[2].setName("btn_cancel");
	        		break;
	        	case JOptionPane.YES_NO_OPTION:
	        		gombok[0].setName("btn_yes");
	        		gombok[1].setName("btn_no");
	        		break;
	        	case JOptionPane.DEFAULT_OPTION:
	        		if(options!=null)for(int i=0; i<options.length;i++)gombok[i].setName("btn_".concat((String)options[i]).toLowerCase());
	        		else gombok[0].setName("btn_ok");
	        		break;
	        }
	        int style = styleFromMessageType(messageType);
	        JDialog dialog = pane.createDialog(parentComponent, title);

	        pane.selectInitialValue();
	        //dialog.setName(name);
	        dialog.setName("popupdialog");
	        dialog.setVisible(true);
	        		
			dialog.dispose();

	        Object selectedValue = pane.getValue();

	        if(selectedValue == null)
	            return JOptionPane.CLOSED_OPTION;
	        if(options == null) {
	            if(selectedValue instanceof Integer)
	                return ((Integer)selectedValue).intValue();
	            return JOptionPane.CLOSED_OPTION;
	        }
	        for(int counter = 0, maxCounter = options.length;
	            counter < maxCounter; counter++) {
	            if(options[counter].equals(selectedValue))
	                return counter;
	        }
	        return JOptionPane.CLOSED_OPTION;
	    }

	private static int styleFromMessageType(int messageType) {
	    switch (messageType) {
	    case JOptionPane.ERROR_MESSAGE:
	        return JRootPane.ERROR_DIALOG;
	    case JOptionPane.QUESTION_MESSAGE:
	        return JRootPane.QUESTION_DIALOG;
	    case JOptionPane.WARNING_MESSAGE:
	        return JRootPane.WARNING_DIALOG;
	    case JOptionPane.INFORMATION_MESSAGE:
	        return JRootPane.INFORMATION_DIALOG;
	    case JOptionPane.PLAIN_MESSAGE:
	    default:
	        return JRootPane.PLAIN_DIALOG;
	    }
	}
	
	private static Locale getLocale(Component c){return (c == null) ? Locale.getDefault() : c.getLocale();}
		
}

