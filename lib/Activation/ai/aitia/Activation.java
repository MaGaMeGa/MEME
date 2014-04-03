package ai.aitia;

import java.io.File;

import java.util.Date;
import java.util.prefs.Preferences;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.filechooser.FileFilter;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.safehaus.uuid.EthernetAddress;
import org.safehaus.uuid.NativeInterfaces;

/**
 * See the usage example in HowToUseActivation.java.
 */
public class Activation extends JPanel implements java.awt.event.ActionListener
{
		private static final long serialVersionUID = 1L;
		private static final byte NO_ETHERNET[] = { 0, 0, 0, 0, 0, 0 };

		public static enum Message { 
			NOT_ACTIVATED, BAD_ACTIVATION, SUCCESSFUL,
			HERE_IS_YOUR_CODE,
			PLEASE_ENTER_FILENAME,
			ACTIVATION_ERROR;

			String lines[] = { "" };
			public void	setMessage(String[] lines)	{ this.lines = lines; }
			public String[]	getMessage()				{ return lines; }
		};

		static {
			Message.NOT_ACTIVATED.setMessage(new String[] { 
								"This instance of the program has not been activated yet (or it has expired).",
							    "You must activate it before continue.",
					            "Please find the information about the activation in the user guide." });
			Message.BAD_ACTIVATION.setMessage(new String[] {
								"The specified file is not valid or expired." });
			Message.SUCCESSFUL.setMessage(new String[] { 
								"You have successfully activated the program.",
								"Thank you!" });
			Message.HERE_IS_YOUR_CODE.setMessage(new String[] {
								"This is your activation code:  "
								});
			Message.PLEASE_ENTER_FILENAME.setMessage(new String[] {
								"<html>Please enter the location of the activation file " +
								"that you have received from AITIA's website into the field below:"
								});
			Message.ACTIVATION_ERROR.setMessage(new String[] {
								"Activation cannot be performed due to the following error:"
								});
		}

		private static String		title	= "Activation";

		private JLabel		jLabel			= null;
		private JTextField	jTextField		= null;
		private JButton		jBrowseButton	= null;

		//=========================================================================
		//	Public methods

		//-------------------------------------------------------------------------
		public Activation() {
			super();
			initialize();
		}

		//-------------------------------------------------------------------------
		public static void setNativeLibDir(File arg) {
			NativeInterfaces.setLibDir(arg);
		}

		//-------------------------------------------------------------------------
		public static void setTitle(String new_title) {
			title = new_title;
		}

		//-------------------------------------------------------------------------
		public static byte[] showMessage(java.awt.Component parent, Message m) throws Exception
		{
			switch (m) {
			case NOT_ACTIVATED : {
				Object[] options = { "Activate now", "Later" };
				int choice = JOptionPane.showOptionDialog(parent, m.getMessage(), title, 
						JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
						null, options, options[0]);
				if (choice != 0)
					break;

				HereIsYourCodeDialog ap = new HereIsYourCodeDialog();
				JOptionPane.showMessageDialog(parent, ap, "Activation code", JOptionPane.PLAIN_MESSAGE, null);

				final Activation _this = new Activation();
				JOptionPane op = new JOptionPane(_this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
					private static final long serialVersionUID = 1L;
					@Override public void setValue(Object newValue) {
						// Block the OK button while the specified file is invalid
						if (newValue != null && newValue.equals(JOptionPane.OK_OPTION)
								&& !new File(_this.getJTextField().getText()).isFile())
							return;
						super.setValue(newValue);
					}
				};
				javax.swing.JDialog dialog = op.createDialog(parent, title);
				op.selectInitialValue();
				dialog.setVisible(true);
				dialog.dispose();
				if (op.getValue() instanceof Integer && ((Integer)op.getValue()).equals(JOptionPane.OK_OPTION))  
					return _this.getActivationData();
				break;
			}

			case BAD_ACTIVATION :
				JOptionPane.showMessageDialog(parent, m.getMessage(), title, JOptionPane.ERROR_MESSAGE);
				break;

			case SUCCESSFUL :
				JOptionPane.showMessageDialog(parent, m.getMessage(), title, JOptionPane.INFORMATION_MESSAGE);
				break;

			}
			return null;
		}

		//-------------------------------------------------------------------------
		public static void showActivationErrorMsg(java.awt.Component parent, Throwable error) {
			if (error != null) {
				String err = error.getLocalizedMessage();
				if (err == null)
					err = error.getClass().getName();
				String tmp[] = err.split(";");

				int n = Message.ACTIVATION_ERROR.getMessage().length;
				String[] lines = new String[n + tmp.length];
				System.arraycopy(Message.ACTIVATION_ERROR.getMessage(), 0, lines, 0, n);
				System.arraycopy(tmp, 0, lines, n, tmp.length);
				JOptionPane.showMessageDialog(parent, lines, "Activation error", JOptionPane.ERROR_MESSAGE);
			}
		}

		//-------------------------------------------------------------------------
		/** 
		 * Throws exception if the system registry is inaccessible
		 * or there's error while retrieving the Ethernet address.
		 */
		public static boolean isActivated() throws Exception {
			byte data[] = new byte[0];
			data = Preferences.userNodeForPackage(Activation.class).getByteArray(ACTIVATION, data); 
			return isOK(data) != null;
		}

		//-------------------------------------------------------------------------
		public static boolean verifyAndActivate(byte[] data) throws Exception {
			if (data != null) {
				data = isOK(data);
				if (data != null) {
					Preferences.userNodeForPackage(Activation.class).putByteArray(ACTIVATION, data);
					return true;
				}
			}
			return false;
		}


		//=========================================================================
		//	Activation algorithm

		// A fajl akkor jo, ha a sz�m�t�g�p ethernet-c�m�t �s egy long bajtjait
		// tartalmazza triple DES k�dol�ssal az al�bbi pwd-t haszn�lva.
		// Sikeres aktivalas eseten a registry-be a fajl tartalma kerul be,
		// megpedig binarisan (base64), mert ugy nehezebben felismerheto 

		private static final String pwd = "FvHBedW4st1snvg18Y1B1dJ4";
		private static final String ACTIVATION = "Activation";		// registry field


		//-------------------------------------------------------------------------
		// data tartalma t�rolt ethernet-c�m �s d�tum triple DES-sel k�dolva 
		// decode tartalma : 0-5 byte: t�rolt ethernet-c�m
		//				     6-  byte: t�rolt d�tum (egy long byte-k�nt)
		// getEthernetAddress() miatt dobhat exceptiont
		private static byte[] isOK(byte[] data) throws Exception {
			if (data == null || data.length < 6 || 20 < data.length) 
				return null;
			byte[] decoded = decode(data);
			if (decoded == null)
				return null;
			byte[] ethdata = new byte[6];
			byte[] bdate = new byte[decoded.length - 6];
			for (int i = 0; i < decoded.length; ++i) {
				if (i < 6) ethdata[i] = decoded[i];
				else bdate[i - 6] = decoded[i];
			}

			Date date = new Date(byteArrayToLong(bdate));
			Date currentDate = new Date();
			if (currentDate.compareTo(date) > 0)	// lej�rt d�tum 
				return null;

			return java.util.Arrays.equals(getEthernetAddress(), ethdata) ? data : null;				
		}

		//-------------------------------------------------------------------------
		// from MEME application
		private static void copyStream(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException {
			in = new java.io.BufferedInputStream(in);
			byte buffer[] = new byte[10000];  
			int length;
			try {
				while ((length = in.read(buffer)) != -1)
					out.write(buffer, 0, length);
			} finally {
				in.close();
			}
		}

		//-------------------------------------------------------------------------
		private byte[] getActivationData() throws java.io.IOException {
			byte[] ans = loadBinFile(new java.io.File(getJTextField().getText()));
			return ans;
		}

		//-------------------------------------------------------------------------
		private static byte[] loadBinFile(java.io.File f) throws java.io.IOException {
			java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
			copyStream(new java.io.FileInputStream(f), bo);
			bo.close();
			return bo.toByteArray();
		}

		//-------------------------------------------------------------------------
		private static byte[] decode(byte[] data) {
			try {
				Key key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(pwd.getBytes()));
				Cipher c = Cipher.getInstance("DESede");
				c.init(Cipher.DECRYPT_MODE,key);
				return c.doFinal(data);
			} catch (Exception e) {
				return null;
			}
		}

		//-------------------------------------------------------------------------
		private static String createActivationCode() throws Exception {
			Key key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(pwd.getBytes()));
			Cipher c = Cipher.getInstance("DESede");
			c.init(Cipher.ENCRYPT_MODE,key);
			byte[] encoded = c.doFinal(getEthernetAddress());
			return Long.toString(byteArrayToLong(encoded),36).toUpperCase();
		}

		//-------------------------------------------------------------------------
		private static byte[] getEthernetAddress() {
			EthernetAddress eth = NativeInterfaces.getPrimaryInterface();
			return (eth == null) ? NO_ETHERNET : eth.asByteArray();
		}
		
		//-------------------------------------------------------------------------
		private static long byteArrayToLong(byte[] input) {
			input = correctLength(input, 8);
	    		 
			long result;
			result  = ((long)(input[0] & 0xFF)) << 56;
			result |= ((long)(input[1] & 0xFF)) << 48;
			result |= ((long)(input[2] & 0xFF)) << 40;
			result |= ((long)(input[3] & 0xFF)) << 32;
			result |= ((long)(input[4] & 0xFF)) << 24;
			result |= ((long)(input[5] & 0xFF)) << 16;
			result |= ((long)(input[6] & 0xFF)) << 8;
			result |= ((long)(input[7] & 0xFF));
			return result;
		}
		
		//-------------------------------------------------------------------------
		private static byte[] correctLength(byte[] data, int length) {
		    if (data.length >= length)
		      return data;
		    
		    byte[] result = new byte[length];

		    for (int i=0; (i<data.length) && (i<result.length); i++) 
		      result[i] = data[i];
		    return result;
		}

		//=========================================================================
		//	Internals

		//-------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == getJBrowseButton()) {
				java.io.File f = new java.io.File(getJTextField().getText());

			    JFileChooser chooser = new JFileChooser();
			    chooser.setAcceptAllFileFilterUsed(false);
			    chooser.addChoosableFileFilter(new ActivationFileFilter());
			    chooser.setCurrentDirectory(f.getParentFile());
			    int returnVal = chooser.showOpenDialog(getJBrowseButton().getTopLevelAncestor());
			    if (returnVal == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().exists()) {
			    	getJTextField().setText(chooser.getSelectedFile().toString());
			    }
			}
		}
			
		/**
		 * This method initializes this
		 * 
		 * @return void
		 */
		private void initialize() {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 1;
			gridBagConstraints2.gridy = 1;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.insets = new Insets(0, 0, 0, 10);
			gridBagConstraints1.gridx = 0;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.weightx = 1.0D;
			gridBagConstraints.insets = new Insets(10, 0, 10, 0);
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.gridy = 0;
			jLabel = new JLabel();
			jLabel.setText(Message.PLEASE_ENTER_FILENAME.getMessage()[0]);
			this.setSize(300, 200);
			this.setLayout(new GridBagLayout());
			this.add(jLabel, gridBagConstraints);
			this.add(getJTextField(), gridBagConstraints1);
			this.add(getJBrowseButton(), gridBagConstraints2);
		}

		/**
		 * This method initializes jTextField	
		 * 	
		 * @return javax.swing.JTextField	
		 */
		private JTextField getJTextField() {
			if (jTextField == null) {
				jTextField = new JTextField();
			}
			return jTextField;
		}

		/**
		 * This method initializes jBrowseButton	
		 * 	
		 * @return javax.swing.JButton	
		 */
		private JButton getJBrowseButton() {
			if (jBrowseButton == null) {
				jBrowseButton = new JButton();
				jBrowseButton.setText("Browse...");
				jBrowseButton.addActionListener(this);
			}
			return jBrowseButton;
		}

		private static class ActivationFileFilter extends FileFilter
		{
			/** Accepts all directories and all XML-files.
			 * 
			 * @param f filename
			 */
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				String extension = null;
				String s = f.getName();
				int i = s.lastIndexOf('.');
				if (i>0 && i<s.length()-1) {
					extension = s.substring(i+1).toLowerCase();
				}
				if (extension != null && extension.equals("dat")) return true;
				return false;
			}
			
			/** The description of the filter. */
			public String getDescription() {
				return "Activation data files (.dat)";
			}
		}
		
		@SuppressWarnings("serial")
		private static class HereIsYourCodeDialog extends JPanel implements java.awt.event.ActionListener
		{
			javax.swing.Action copyToClipboard = null;
			JTextField field = null;

			public HereIsYourCodeDialog() throws Exception {
				super();
				initialize();
			}

			private void initialize() throws Exception {
				this.setSize(300, 200);
				this.setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
				JLabel label = new JLabel(Message.HERE_IS_YOUR_CODE.getMessage()[0]);
				field = new JTextField(createActivationCode());		// Generate activation code
				field.setEditable(false);
				this.add(label);
				this.add(field);

				copyToClipboard = field.getActionMap().get("copy");
				if (copyToClipboard != null) {
					javax.swing.JButton copyButton = new javax.swing.JButton("Copy");
					copyButton.addActionListener(this);
					this.add(javax.swing.Box.createHorizontalStrut(10));
					this.add(copyButton);
				}
			}

			public void actionPerformed(ActionEvent e) {
				field.selectAll();
				// !!! Try to use field.copy(); instead!
				copyToClipboard.actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, null));
				field.select(0, 0);
			}
		}
}
