package ai.aitia;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.Key;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class CmdLineFileGen
{
	public static String	FILENAME_TO_GENERATE	= "MASS_act.dat";
	public static int		TRIAL_PERIOD_IN_DAYS	= 90; 

	public static void Usage() {
		System.err.println("Usage: java -cp . ai.aitia.CmdLineFileGen <passwordString>\n" + 
				"Creates an activation file, " + FILENAME_TO_GENERATE + " in the current directory." 
		);
		System.exit(1);
	}

	//-------------------------------------------------------------------------
	/**
	 * See {@link #Usage()} 
	 */
	public static void main(String[] args) {
		if (args.length < 1 || "--help".equals(args[0]))
			Usage();

		String actCode = args[0].toString();
		String currDir = System.getProperty("user.dir");

		File destFile = new File(currDir, FILENAME_TO_GENERATE);

		System.out.println("Activation code: " + actCode);
		System.out.println("Destination file: " + destFile);
		try {
			generateFile(actCode, destFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------------
	public static void generateFile(String actCode, File destfile) throws Exception {
		final String pwd = "FvHBedW4st1snvg18Y1B1dJ4";
		Key key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(pwd.getBytes()));
		Cipher c = Cipher.getInstance("DESede");
		c.init(Cipher.DECRYPT_MODE, key);
		// az aktiv�ci�s k�d Triple DES k�dolt v�ltozata egy 36-os sz�mrendszerben �rtelmezett long �rt�k sztringk�nt
		byte[] decoded = c.doFinal(longToByteArray(Long.parseLong(actCode, 36))); //dek�dol�s
		Date date = new Date();
		date.setTime(date.getTime() + TRIAL_PERIOD_IN_DAYS * 24 * 3600 * 1000L);
		byte[] bdate = longToByteArray(date.getTime()); // lej�rati d�tum b�jtt�mbk�nt
		byte[] all = new byte[decoded.length + bdate.length];	// a v�lasz 1 b�jtt�mbben
		System.arraycopy(decoded, 0, all, 0, decoded.length);
		System.arraycopy(bdate, 0, all, decoded.length, bdate.length);
		c = Cipher.getInstance("DESede");
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encoded = c.doFinal(all); // a v�lasz k�dol�sa

		// ki�r�s file-ba
		BufferedOutputStream bf = new BufferedOutputStream(new FileOutputStream(destfile));
		bf.write(encoded);
		bf.flush();
		bf.close();
	}

	//-------------------------------------------------------------------------
	public static byte[] longToByteArray(long input) {
		byte[] output = new byte[8];
		longToByteArray(input, output, 0);
		return output;
	}

	//-------------------------------------------------------------------------
	// big-endian kodolas
	public static void longToByteArray(long input, byte[] output, int offset) {    
		output[offset + 0] = (byte) (0xFF & (input >> 56));
		output[offset + 1] = (byte) (0xFF & (input >> 48));
		output[offset + 2] = (byte) (0xFF & (input >> 40));
		output[offset + 3] = (byte) (0xFF & (input >> 32));
		output[offset + 4] = (byte) (0xFF & (input >> 24));
		output[offset + 5] = (byte) (0xFF & (input >> 16));
		output[offset + 6] = (byte) (0xFF & (input >> 8));
		output[offset + 7] = (byte) (0xFF & input);
	}

}
