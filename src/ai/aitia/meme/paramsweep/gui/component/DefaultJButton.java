package ai.aitia.meme.paramsweep.gui.component;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class DefaultJButton extends JButton {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -3708017061765474773L;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton() {}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final Icon icon) {
		super(icon);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final String text) {
		super(text);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final Action a) {
		super(a);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final String text, final Icon icon) {
		super(text, icon);
	}
}