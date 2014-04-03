package ai.aitia;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class HowToUseActivation 
{
	public static void main(String[] args) 
	{
		// This frame mimics the main window of the application
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel p = new JPanel();
		frame.setContentPane(p);
		frame.setSize(300,200);
		frame.setVisible(true);

		boolean isActivated = false;
		try {
			// The following will fail if the native libraries are deleted
			Activation.setNativeLibDir(new File(System.getProperty("user.dir"), "lib"));
			isActivated = Activation.isActivated();
			while (!isActivated) {
				byte[] actData = Activation.showMessage(frame, Activation.Message.NOT_ACTIVATED);
				if (actData == null) break;
				isActivated = Activation.verifyAndActivate(actData);
				if (isActivated)
					Activation.showMessage(frame, Activation.Message.SUCCESSFUL);
				else
					Activation.showMessage(frame, Activation.Message.BAD_ACTIVATION);
			};
		} catch (Throwable t) {
			isActivated = false;
			// log the error...
			t.printStackTrace();
			Activation.showActivationErrorMsg(frame, t);
		}

		System.out.println("Activated: " + isActivated);
		if (!isActivated)
			System.exit(0);

		// Continue the application
		// ...
		System.exit(0);
		
	}
}
