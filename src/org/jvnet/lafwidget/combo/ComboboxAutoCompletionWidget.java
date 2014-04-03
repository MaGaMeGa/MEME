package org.jvnet.lafwidget.combo;

import java.awt.Component;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.text.*;

import org.jvnet.lafwidget.*;

/**
 * Adds auto-completion on editable combo boxes.
 * 
 * @author Kirill Grouchnikov
 * @author Thomas Bierhance http://www.orbital-computer.de/JComboBox/
 * @author inostock
 * @author Daniel Kjellin http://www.daik.se/
 */
/* 
 * Modified by Bocsi Rajmund 
 * using case sensitive auto-completion
 */
public class ComboboxAutoCompletionWidget extends LafWidgetAdapter {
	protected JComboBox comboBox;

	/**
	 * Property change handler on <code>enabled</code> property.
	 */
	protected ComboBoxPropertyChangeHandler changeHandler;

	protected ComboBoxModel model;

	protected Component editor;

	// flag to indicate if setSelectedItem has been called
	// subsequent calls to remove/insertString should be ignored
	protected boolean selecting = false;

	protected boolean hidePopupOnFocusLoss;

	protected boolean hitBackspace = false;

	protected boolean hitBackspaceOnSelection;

	protected ActionListener completionActionListener;

	protected PropertyChangeListener completionPropertyListener;

	protected KeyListener editorKeyListener;

	protected FocusListener editorFocusListener;

	protected CompletionPlainDocument completionDocument;

	protected ActionMap oldActionMap;

	/**
	 * Code contributed by Thomas Bierhance from
	 * http://www.orbital-computer.de/JComboBox/
	 */
	@SuppressWarnings("serial")
	protected class CompletionPlainDocument extends PlainDocument {
		protected JComboBox comboBox;

		protected ComboBoxModel model;

		public CompletionPlainDocument(JComboBox combo) {
			super();
			this.comboBox = combo;
			this.model = this.comboBox.getModel();
		}

		@Override
		public void remove(int offs, int len) throws BadLocationException {
			// return immediately when selecting an item
			if (ComboboxAutoCompletionWidget.this.selecting)
				return;

			if (ComboboxAutoCompletionWidget.this.hitBackspace) {
				// user hit backspace => move the selection backwards
				// old item keeps being selected
				if (offs > 0) {
					if (ComboboxAutoCompletionWidget.this.hitBackspaceOnSelection)
						offs--;
				} else {
					// User hit backspace with the cursor positioned on the
					// start => beep
					this.comboBox.getToolkit().beep(); // when available use:
					// UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
				}
				// highlight when auto-completion is required
				if (LafWidgetUtilities.hasAutoCompletion(this.comboBox))
					this.highlightCompletedText(offs);
			} else {
				super.remove(offs, len);
			}
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			// return immediately when selecting an item
			if (ComboboxAutoCompletionWidget.this.selecting)
				return;

			// insert the string into the document
			super.insertString(offs, str, a);

			// return immediately when no auto-completion is required
			if (!LafWidgetUtilities.hasAutoCompletion(this.comboBox))
				return;

			// lookup and select a matching item
			Object item = this.lookupItem(this.getText(0, this.getLength()));
			if (LafWidgetUtilities.hasUseModelOnlyProperty(this.comboBox)) {
				if (item != null) {
					this.setSelectedItem(item);
				} else {
					// keep old item selected if there is no match
					item = this.comboBox.getSelectedItem();
					// imitate no insert (later on offs will be incremented by
					// str.length(): selection won't move forward)
					offs = offs - str.length();
					// provide feedback to the user that his input has been
					// received but can not be accepted
					this.comboBox.getToolkit().beep();
				}
				this.setText(item.toString());
				// select the completed part
				this.highlightCompletedText(offs + str.length());
			} else {
				if (item != null) {
					this.setSelectedItem(item);
					this.setText(item.toString());
					// select the completed part
					this.highlightCompletedText(offs + str.length());
				} else {
					offs = offs - str.length();
				}
			}
		}

		private void setText(String text) {
			try {
				// remove all text and insert the completed string
				super.remove(0, this.getLength());
				super.insertString(0, text, null);
			} catch (BadLocationException e) {
				throw new RuntimeException(e.toString());
			}
		}

		private void highlightCompletedText(int start) {
			if (ComboboxAutoCompletionWidget.this.editor instanceof JTextComponent) {
				JTextComponent textEditor = (JTextComponent) ComboboxAutoCompletionWidget.this.editor;
				// Fix for defect 2 (defect 151 on Substance) by Daniel Kjellin
				textEditor.setCaretPosition(textEditor.getDocument()
						.getLength());
				textEditor.moveCaretPosition(start);
			}
		}

		private void setSelectedItem(Object item) {
			ComboboxAutoCompletionWidget.this.selecting = true;
			this.model.setSelectedItem(item);
			ComboboxAutoCompletionWidget.this.selecting = false;
		}

		private Object lookupItem(String pattern) {
			Object selectedItem = this.model.getSelectedItem();
			// only search for a different item if the currently selected does
			// not match
			if ((selectedItem != null)
					&& this.startsWithIgnoreCase(selectedItem.toString(),
							pattern)) {
				return selectedItem;
			} else {
				// iterate over all items
				for (int i = 0, n = this.model.getSize(); i < n; i++) {
					Object currentItem = this.model.getElementAt(i);
					// current item starts with the pattern?
					if ((currentItem != null)
							&& this.startsWithIgnoreCase(
									currentItem.toString(), pattern)) {
						return currentItem;
					}
				}
			}
			// no item starts with the pattern => return null
			return null;
		}

		// checks if str1 starts with str2 - do not ignore case
		private boolean startsWithIgnoreCase(String str1, String str2) {
//			return str1.toUpperCase().startsWith(str2.toUpperCase());
			return str1.startsWith(str2);

		}
	}

	public boolean isSimple() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.lafwidget.LafWidgetAdapter#setComponent(javax.swing.JComponent)
	 */
	@Override
	public void setComponent(JComponent jcomp) {
		super.setComponent(jcomp);
		this.comboBox = (JComboBox) jcomp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.lafwidget.LafWidgetAdapter#installUI()
	 */
	@Override
	public void installUI() {
		final ComboBoxEditor cbe = this.comboBox.getEditor();
		final Component cbc = cbe.getEditorComponent();
		if (cbc instanceof JTextComponent) {
			this.installTextEditor((JTextComponent) cbc);
		} else {
			this.installEditor(cbc);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.lafwidget.LafWidgetAdapter#installListeners()
	 */
	@Override
	public void installListeners() {
		this.changeHandler = new ComboBoxPropertyChangeHandler();
		this.comboBox.addPropertyChangeListener(this.changeHandler);
	}

	protected void installTextEditor(final JTextComponent c) {
		// Code contributed by Thomas Bierhance from
		// http://www.orbital-computer.de/JComboBox/
		if (this.comboBox.isEditable()) {
			this.completionDocument = new CompletionPlainDocument(this.comboBox);
			c.setDocument(this.completionDocument);
			this.model = this.comboBox.getModel();
			this.completionActionListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if ((!ComboboxAutoCompletionWidget.this.selecting)
							&& (ComboboxAutoCompletionWidget.this.completionDocument != null))
						ComboboxAutoCompletionWidget.this.completionDocument
								.highlightCompletedText(0);
				}
			};
			this.comboBox.addActionListener(this.completionActionListener);

			this.completionPropertyListener = new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					// if (e.getPropertyName().equals("editor"))
					// configureEditor((ComboBoxEditor) e.getNewValue());
					if (e.getPropertyName().equals("model"))
						ComboboxAutoCompletionWidget.this.model = (ComboBoxModel) e
								.getNewValue();
				}
			};
			this.comboBox
					.addPropertyChangeListener(this.completionPropertyListener);

			this.editorKeyListener = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (comboBox.isDisplayable()
							&& (e.getKeyCode() != KeyEvent.VK_ENTER)
							&& (e.getKeyChar() != KeyEvent.VK_ESCAPE)) {
						comboBox.setPopupVisible(true);
					}
					ComboboxAutoCompletionWidget.this.hitBackspace = false;
					switch (e.getKeyCode()) {
					// determine if the pressed key is backspace (needed by the
					// remove method)
					case KeyEvent.VK_BACK_SPACE:
						ComboboxAutoCompletionWidget.this.hitBackspace = true;
						ComboboxAutoCompletionWidget.this.hitBackspaceOnSelection = ((JTextField) ComboboxAutoCompletionWidget.this.editor)
								.getSelectionStart() != ((JTextField) ComboboxAutoCompletionWidget.this.editor)
								.getSelectionEnd();
						break;
					case KeyEvent.VK_DELETE:
						if (LafWidgetUtilities
								.hasUseModelOnlyProperty(comboBox)) {
							// ignore delete key on model-only combos
							e.consume();
							comboBox.getToolkit().beep();
						} else {
							((JTextField) ComboboxAutoCompletionWidget.this.editor)
									.replaceSelection("");
						}
						break;
					case KeyEvent.VK_ESCAPE:
						// forward to the parent - allows closing dialogs
						// with editable combos having focus.
						comboBox.getParent().dispatchEvent(e);
					}
				}
			};
			// Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when
			// tabbing out
			this.hidePopupOnFocusLoss = System.getProperty("java.version")
					.startsWith("1.5");
			// Highlight whole text when gaining focus
			this.editorFocusListener = new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					if (ComboboxAutoCompletionWidget.this.completionDocument != null)
						ComboboxAutoCompletionWidget.this.completionDocument
								.highlightCompletedText(0);
				}

				@Override
				public void focusLost(FocusEvent e) {
					// Workaround for Bug 5100422 - Hide Popup on focus loss
					if (ComboboxAutoCompletionWidget.this.hidePopupOnFocusLoss
							&& (comboBox != null))
						comboBox.setPopupVisible(false);
				}
			};
			// configureEditor(comboBox.getEditor());
			this.installEditor(c);
			// Handle initially selected object
			Object selected = this.comboBox.getSelectedItem();
			if (this.completionDocument != null) {
				if (selected != null)
					this.completionDocument.setText(selected.toString());
				this.completionDocument.highlightCompletedText(0);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.lafwidget.LafWidgetAdapter#uninstallListeners()
	 */
	@Override
	public void uninstallListeners() {
		this.comboBox.removePropertyChangeListener(this.changeHandler);
		this.changeHandler = null;

		if (this.comboBox.isEditable()) {
			this.uninstallTextEditor(null);
		}

		super.uninstallListeners();
	}

	protected void uninstallTextEditor(final JTextComponent e) {
		this.completionDocument = null;
		this.comboBox.removeActionListener(this.completionActionListener);
		this.completionActionListener = null;
		this.comboBox
				.removePropertyChangeListener(this.completionPropertyListener);
		this.completionPropertyListener = null;
		if (e == null)
			return;
		if (this.editorKeyListener != null) {
			e.removeKeyListener(this.editorKeyListener);
			this.editorKeyListener = null;
		}
		if (this.editorFocusListener != null) {
			e.removeFocusListener(this.editorFocusListener);
			this.editorFocusListener = null;
		}
		if (this.oldActionMap != null) {
			this.comboBox.setActionMap(this.oldActionMap);
			this.oldActionMap = null;
		}
	}

	protected void installEditor(final Component c) {
		if ((c == null) || (this.editor == c))
			return;

		final Component last = this.editor;
		if (last != null) {
			last.removeKeyListener(this.editorKeyListener);
			last.removeFocusListener(this.editorFocusListener);
		}

		this.editor = c;
		this.editor.addKeyListener(this.editorKeyListener);
		this.editor.addFocusListener(this.editorFocusListener);

		if (this.oldActionMap == null) {
			// due to the implementation in BasicComboBoxUI (the
			// same action map for all combos) we need to
			// create a new action map
			this.oldActionMap = this.comboBox.getActionMap();
			ActionMap newActionMap = new ActionMap();
			Object[] keys = this.oldActionMap.allKeys();
			for (int i = 0; i < keys.length; i++) {
				if ("enterPressed".equals(keys[i]))
					continue;
				newActionMap.put(keys[i], this.oldActionMap.get(keys[i]));
			}
			this.comboBox.setActionMap(newActionMap);
		}
	}

	public class ComboBoxPropertyChangeHandler implements
			PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent e) {
			String propertyName = e.getPropertyName();

			if (propertyName.equals("editable")) {
				final boolean oldValue = ((Boolean) e.getOldValue())
						.booleanValue();
				final boolean newValue = ((Boolean) e.getNewValue())
						.booleanValue();
				if (!oldValue && newValue
						&& LafWidgetUtilities.hasAutoCompletion(comboBox)) {
					final ComboBoxEditor cbe = comboBox.getEditor();
					final Component cbc = cbe.getEditorComponent();
					if (cbc instanceof JTextComponent) {
						ComboboxAutoCompletionWidget.this
								.installTextEditor((JTextComponent) cbc);
					} else {
						ComboboxAutoCompletionWidget.this.installEditor(cbc);
					}
				} else if (oldValue && !newValue) {
					ComboboxAutoCompletionWidget.this.uninstallTextEditor(null);
				}
			}

			// fix for issue 179 on Substance - allowing no auto-completion
			// mode on editable comboboxes.
			if (propertyName.equals(LafWidget.COMBO_BOX_NO_AUTOCOMPLETION)
					|| propertyName.equals("JComboBox.isTableCellEditor")) {
				final ComboBoxEditor cbe = comboBox.getEditor();
				final Component cbc = cbe.getEditorComponent();
				if (LafWidgetUtilities.hasAutoCompletion(comboBox)) {
					if (cbc instanceof JTextComponent) {
						installTextEditor((JTextComponent) cbc);
					} else {
						installEditor(cbc);
					}
				} else {
					if (cbc instanceof JTextComponent)
						uninstallTextEditor((JTextComponent) cbc);
					else
						uninstallTextEditor(null);
				}
			}

			// fix for defect 131 in 2.2_01
			if (propertyName.equals("editor")) {
				final ComboBoxEditor oldValue = (ComboBoxEditor) e
						.getOldValue();
				final ComboBoxEditor newValue = (ComboBoxEditor) e
						.getNewValue();
				if ((newValue != null) && (newValue != oldValue)
						&& LafWidgetUtilities.hasAutoCompletion(comboBox)) {
					final Component old = (oldValue != null) ? oldValue
							.getEditorComponent() : null;
					if (old instanceof JTextComponent) {
						ComboboxAutoCompletionWidget.this
								.uninstallTextEditor((JTextComponent) old);
					}
					final Component pending = newValue.getEditorComponent();
					if (pending instanceof JTextComponent) {
						ComboboxAutoCompletionWidget.this
								.installTextEditor((JTextComponent) pending);
					} else {
						ComboboxAutoCompletionWidget.this
								.installEditor(pending);
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							comboBox.doLayout();
						}
					});
				}
			}

			// fix for defect 6 - changing model on editable combo doesn't
			// track changes to the model
			if (propertyName.equals("model")) {
				if (LafWidgetUtilities.hasAutoCompletion(comboBox)) {
					ComboboxAutoCompletionWidget.this.uninstallTextEditor(null);
					final ComboBoxEditor cbe = comboBox.getEditor();
					final Component cbc = cbe.getEditorComponent();
					if (cbc instanceof JTextComponent) {
						ComboboxAutoCompletionWidget.this
								.installTextEditor((JTextComponent) cbc);
					} else {
						ComboboxAutoCompletionWidget.this.installEditor(cbc);
					}
				}
			}

			// Do not call super - fix for bug 63
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jvnet.lafwidget.LafWidget#requiresCustomLafSupport()
	 */
	public boolean requiresCustomLafSupport() {
		return false;
	}
}
