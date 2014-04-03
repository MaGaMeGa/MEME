package ai.aitia;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FileGen extends javax.swing.JPanel implements java.awt.event.ActionListener 
{
	JLabel		l1				= new JLabel("Filename:");
	JTextField	fnTextField		= new JTextField("p.dat");
	JButton		browseButton	= new JButton("Browse");

	JLabel		l2				= new JLabel("Activation code:");
	JTextField	codeTextField	= new JTextField(16);
	JButton		genButton		= new JButton("Generate file");
	JButton		closeButton		= new JButton("Exit");
	
	//-------------------------------------------------------------------------
	public FileGen() {
		this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		makeGrid(this, new String[] {	"0112",
										"3455",
										"_67_" },
					l1,				"e r5",			// 0
					fnTextField,	"we wx1",		// 1
					browseButton,	"e l5",			// 2
					l2,				"e r5 t10",		// 3
					codeTextField,	"we wx1 t10",	// 4
					genButton,		"we l5 t10",	// 5
					closeButton,	"n wy1 t15",	// 6
					new JLabel("          "), ""	// 7
				);

		javax.swing.event.DocumentListener dl = 
			java.beans.EventHandler.create(javax.swing.event.DocumentListener.class, this, "enableDisableButtons");
		fnTextField.getDocument().addDocumentListener(dl);
		fnTextField.setText(new java.io.File(System.getProperty("user.dir"), fnTextField.getText()).toString());
		codeTextField.getDocument().addDocumentListener(dl);
		browseButton.addActionListener(this);
		genButton.addActionListener(this);
		closeButton.addActionListener(this);

		genButton.setEnabled(false);
	}
	
	//-------------------------------------------------------------------------
	public void enableDisableButtons() {
		boolean enabled = (codeTextField.getText().length() > 0);
		if (enabled) {
			File tmp = new File(fnTextField.getText()).getParentFile();
			enabled = (tmp == null || tmp.isDirectory());
		}
		genButton.setEnabled(enabled);
	}


	//-------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == browseButton) {
		    JFileChooser chooser = new JFileChooser();
		    chooser.setAcceptAllFileFilterUsed(true);
		    int retVal = chooser.showSaveDialog(this.getTopLevelAncestor());
		    if (retVal == JFileChooser.APPROVE_OPTION) {
		    	fnTextField.setText(chooser.getSelectedFile().toString());
		    }
		}
		else if (e.getSource() == genButton) {
			try {
				CmdLineFileGen.generateFile(codeTextField.getText(), new File(fnTextField.getText()));
			} catch (Throwable t) {
				String msg[] = { String.format("Exception %s:", t.getClass().getName()), t.getLocalizedMessage() };
				javax.swing.JOptionPane.showMessageDialog(this, msg);
			}
		}
		else if (e.getSource() == closeButton) {
			System.exit(0);
		}
	}

	//-------------------------------------------------------------------------
	public static void makeGrid(java.awt.Container parent, String cells[], Object... components) {
		parent.setLayout(new GridBagLayout());
		java.util.HashSet<Character> done = new java.util.HashSet<Character>(components.length); 
		int h = cells.length;
		for (int y = 0; y < cells.length; ++y) {
			int w = cells[y].length();
			for (int x = 0; x < w; ++x) {
				char ch = cells[y].charAt(x);
				if (" _".indexOf(ch) >= 0 || done.contains(ch)) continue;
				GridBagConstraints g = new GridBagConstraints();
				g.gridx	= x;
				g.gridy	= y;
				g.fill	= GridBagConstraints.NONE;
				while (x+g.gridwidth < w && cells[y].charAt(x+g.gridwidth) == ch) g.gridwidth += 1;
				while (y+g.gridheight< h && cells[y+g.gridheight].charAt(x) == ch) g.gridheight += 1;
				x += (g.gridwidth - 1);
				int idx = Integer.parseInt(String.valueOf(ch), 36) * 2;
				Component comp= (Component)components[idx];
				String props = components[idx+1].toString();
				String prop[] = props.split("\\s");
				int sticky	=(prop[0].indexOf('n') >= 0 ?  1 : 0)
							+(prop[0].indexOf('s') >= 0 ?  2 : 0)
							+(prop[0].indexOf('w') >= 0 ?  4 : 0)
							+(prop[0].indexOf('e') >= 0 ?  8 : 0)
							+(prop[0].indexOf('c') >= 0 ? 16 : 0);
				if (sticky > 15) g.anchor = GridBagConstraints.CENTER;
				switch (sticky & 15) {
				        /*ewsn fill anchor */
				case  0:/*0000 -  -  */	break;
				case  1:/*0001 -  n  */	g.anchor= GridBagConstraints.NORTH;		break;
				case  2:/*0010 -  s  */	g.anchor= GridBagConstraints.SOUTH;		break;
				case  3:/*0011 v  c? */	g.fill	= GridBagConstraints.VERTICAL;	break;
				case  4:/*0100 -  w  */	g.anchor= GridBagConstraints.WEST;		break;
				case  5:/*0101 -  nw */	g.anchor= GridBagConstraints.NORTHWEST;	break;
				case  6:/*0110 -  sw */	g.anchor= GridBagConstraints.SOUTHWEST;	break;
				case  7:/*0111 v  w  */	g.anchor= GridBagConstraints.WEST;	g.fill = GridBagConstraints.VERTICAL;	break;
				case  8:/*1000 -  e  */	g.anchor= GridBagConstraints.EAST;		break;
				case  9:/*1001 -  ne */	g.anchor= GridBagConstraints.NORTHEAST;	break;
				case 10:/*1010 -  se */	g.anchor= GridBagConstraints.SOUTHEAST;	break;
				case 11:/*1011 v  e  */	g.anchor= GridBagConstraints.EAST;	g.fill = GridBagConstraints.VERTICAL;	break;
				case 12:/*1100 h  -  */	g.fill	= GridBagConstraints.HORIZONTAL; break;
				case 13:/*1101 h  n  */	g.anchor= GridBagConstraints.NORTH;	g.fill = GridBagConstraints.HORIZONTAL;	break;
				case 14:/*1110 h  s  */	g.anchor= GridBagConstraints.SOUTH;	g.fill = GridBagConstraints.HORIZONTAL;	break;
				case 15:/*1111 b  -  */	g.fill	= GridBagConstraints.BOTH;		break;
				}
				for (int i = prop.length - 1; i > 0; --i) {
					      if (prop[i].startsWith("wx")) g.weightx		= Double.parseDouble(prop[i].substring(2));
					else if (prop[i].startsWith("wy")) g.weighty		= Double.parseDouble(prop[i].substring(2));
					else if (prop[i].startsWith("t")) g.insets.top		= Integer.parseInt(prop[i].substring(1));
					else if (prop[i].startsWith("l")) g.insets.left		= Integer.parseInt(prop[i].substring(1));
					else if (prop[i].startsWith("b")) g.insets.bottom	= Integer.parseInt(prop[i].substring(1));
					else if (prop[i].startsWith("r")) g.insets.right	= Integer.parseInt(prop[i].substring(1));
				}
				parent.add(comp, g);
				done.add(ch);
			}
		}
	}
	
	
	//-------------------------------------------------------------------------
	public static void main(String[] args) {

		// See java.awt.Window.setLocationByPlatform()
		System.setProperty("java.awt.Window.locationByPlatform", "true");

		JFrame frame = new JFrame();
		frame.setTitle("MASS Activation File Generator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		FileGen _this = new FileGen();
		if (args != null && args.length > 0)
			_this.codeTextField.setText(args[0]);
		frame.getContentPane().add(_this);
		frame.pack();
		frame.setVisible(true);
	}

}
