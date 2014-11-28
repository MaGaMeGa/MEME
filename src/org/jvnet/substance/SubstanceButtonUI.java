/*
 * Copyright (c) 2005-2007 Substance Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of Substance Kirill Grouchnikov nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package org.jvnet.substance;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicButtonUI;

import org.jvnet.lafwidget.layout.TransitionLayout;
import org.jvnet.lafwidget.utils.FadeStateListener;
import org.jvnet.lafwidget.utils.FadeTracker;
import org.jvnet.lafwidget.utils.FadeTracker.FadeKind;
import org.jvnet.substance.button.BaseButtonShaper;
import org.jvnet.substance.button.SubstanceButtonShaper;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.combo.SubstanceComboBoxButton;
import org.jvnet.substance.scroll.SubstanceScrollBarButton;
import org.jvnet.substance.utils.ButtonBackgroundDelegate;
import org.jvnet.substance.utils.ButtonColorDelegate;
import org.jvnet.substance.utils.GhostingListener;
import org.jvnet.substance.utils.PulseTracker;
import org.jvnet.substance.utils.RolloverButtonListener;
import org.jvnet.substance.utils.SubstanceColorUtilities;
import org.jvnet.substance.utils.SubstanceConstants.ButtonTitleKind;
import org.jvnet.substance.utils.SubstanceConstants.FocusKind;
import org.jvnet.substance.utils.SubstanceCoreUtilities;

import ai.aitia.meme.paramsweep.gui.component.DefaultJButton;

/**
 * UI for buttons in <b>Substance</b> look and feel.
 * 
 * @author Kirill Grouchnikov
 */
public class SubstanceButtonUI extends BasicButtonUI {
	/**
	 * Property used during the button shaper switch.
	 */
	public static final String BORDER_COMPUTED = "substancelaf.buttonbordercomputed";

	/**
	 * Property used during the button shaper switch.
	 */
	public static final String BORDER_COMPUTING = "substancelaf.buttonbordercomputing";

	/**
	 * Property used to store the original (pre-<b>Substance</b>) button
	 * border.
	 */
	public static final String BORDER_ORIGINAL = "substancelaf.buttonborderoriginal";

	/**
	 * Property used to store the original (pre-<b>Substance</b>) button
	 * opacity.
	 */
	public static final String OPACITY_ORIGINAL = "substancelaf.buttonopacityoriginal";

	/**
	 * Internal property to store the current icon rectangle.
	 */
	public static String ICON_RECT = "substancelaf.internal.buttonIconRect";

	/**
	 * Painting delegate.
	 */
	private ButtonBackgroundDelegate delegate;

	/**
	 * The rollover button listener.
	 */
	private RolloverButtonListener substanceButtonListener;

	/**
	 * Model change listener for ghost image effects.
	 */
	private GhostingListener substanceModelChangeListener;

	/**
	 * Property change listener. Listens on changes to the
	 * {@link SubstanceLookAndFeel#BUTTON_SHAPER_PROPERTY} property and
	 * {@link AbstractButton#MODEL_CHANGED_PROPERTY} property.
	 */
	protected PropertyChangeListener substancePropertyListener;

	/**
	 * Listener for fade animations.
	 */
	protected FadeStateListener substanceFadeStateListener;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.ComponentUI#createUI(javax.swing.JComponent)
	 */
	public static ComponentUI createUI(JComponent b) {
		AbstractButton button = (AbstractButton) b;
		
		button.setRolloverEnabled(true);
		button.setOpaque(false);
			
		return new SubstanceButtonUI();
	}

	/**
	 * Simple constructor.
	 */
	public SubstanceButtonUI() {
		this.delegate = new ButtonBackgroundDelegate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#installDefaults(javax.swing.AbstractButton)
	 */
	@Override
	public void installDefaults(final AbstractButton b) {
		super.installDefaults(b);

		if (b.getClientProperty(SubstanceButtonUI.BORDER_ORIGINAL) == null)
			b.putClientProperty(SubstanceButtonUI.BORDER_ORIGINAL, b
					.getBorder());
		
		SubstanceButtonShaper shaper = SubstanceCoreUtilities
				.getButtonShaper(b);

		if (b.getClientProperty(SubstanceButtonUI.BORDER_COMPUTED) == null) {
			b.setBorder(shaper.getButtonBorder(b));
		} else {
			Border currBorder = b.getBorder();
			if (!(currBorder instanceof SubstanceButtonBorder)) {
				b.setBorder(shaper.getButtonBorder(b));
			} else {
				SubstanceButtonBorder sbCurrBorder = (SubstanceButtonBorder) currBorder;
				if (shaper.getClass() != sbCurrBorder.getButtonShaperClass())
					b.setBorder(shaper.getButtonBorder(b));
			}
		}
		b.putClientProperty(SubstanceButtonUI.OPACITY_ORIGINAL, b.isOpaque());
		if (b instanceof SubstanceComboBoxButton) {
			b.setBorder(new BorderUIResource.CompoundBorderUIResource(
					new EmptyBorder(1, 1, 1, 1), b.getBorder()));
		}

		// Color fg = b.getForeground();
		// if ((fg == null) || (fg instanceof UIResource)) {
		// b.setForeground(new ColorUIResource(SubstanceCoreUtilities
		// .getDefaultScheme(b).getForegroundColor()));
		// }
		//		
		Color bg = b.getBackground();
		if (bg instanceof UIResource) {
			b.setBackground(new ButtonColorDelegate(b, false));
		}

		Color fg = b.getForeground();
		if (fg instanceof UIResource) {
			b.setForeground(new ButtonColorDelegate(b, true));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#uninstallDefaults(javax.swing.AbstractButton)
	 */
	@Override
	public void uninstallDefaults(AbstractButton b) {
		super.uninstallDefaults(b);

		b.setBorder((Border) b
				.getClientProperty(SubstanceButtonUI.BORDER_ORIGINAL));
		b.setOpaque((Boolean) b
				.getClientProperty(SubstanceButtonUI.OPACITY_ORIGINAL));
		b.putClientProperty(SubstanceButtonUI.OPACITY_ORIGINAL, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#createButtonListener(javax.swing.AbstractButton)
	 */
	@Override
	protected BasicButtonListener createButtonListener(AbstractButton b) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#installListeners(javax.swing.AbstractButton)
	 */
	@Override
	protected void installListeners(final AbstractButton b) {
		super.installListeners(b);

		this.substanceButtonListener = new RolloverButtonListener(b);
		b.addMouseListener(this.substanceButtonListener);
		b.addMouseMotionListener(this.substanceButtonListener);
		b.addFocusListener(this.substanceButtonListener);
		b.addPropertyChangeListener(this.substanceButtonListener);
		b.addChangeListener(this.substanceButtonListener);

		this.substancePropertyListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY.equals(evt
						.getPropertyName())) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							SwingUtilities.updateComponentTreeUI(b);
						}
					});
				}
				if (AbstractButton.MODEL_CHANGED_PROPERTY.equals(evt
						.getPropertyName())) {
					if (substanceFadeStateListener != null)
						substanceFadeStateListener.unregisterListeners();
					substanceFadeStateListener = new FadeStateListener(b, b
							.getModel(), null);
					substanceFadeStateListener
							.registerListeners(b instanceof SubstanceScrollBarButton);

					if (substanceModelChangeListener != null)
						substanceModelChangeListener.unregisterListeners();
					substanceModelChangeListener = new GhostingListener(b, b
							.getModel());
					substanceModelChangeListener.registerListeners();
				}
			}
		};
		b.addPropertyChangeListener(this.substancePropertyListener);

		this.substanceFadeStateListener = new FadeStateListener(b,
				b.getModel(), null);
		this.substanceFadeStateListener
				.registerListeners(b instanceof SubstanceScrollBarButton);

		this.substanceModelChangeListener = new GhostingListener(b, b
				.getModel());
		this.substanceModelChangeListener.registerListeners();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#uninstallListeners(javax.swing.AbstractButton)
	 */
	@Override
	protected void uninstallListeners(AbstractButton b) {
		b.removeMouseListener(this.substanceButtonListener);
		b.removeMouseMotionListener(this.substanceButtonListener);
		b.removeFocusListener(this.substanceButtonListener);
		b.removePropertyChangeListener(this.substanceButtonListener);
		b.removeChangeListener(this.substanceButtonListener);
		this.substanceButtonListener = null;

		b.removePropertyChangeListener(this.substancePropertyListener);
		this.substancePropertyListener = null;

		this.substanceFadeStateListener.unregisterListeners();
		this.substanceFadeStateListener = null;

		this.substanceModelChangeListener.unregisterListeners();
		this.substanceModelChangeListener = null;

		super.uninstallListeners(b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void update(Graphics g, JComponent c) {
		AbstractButton button = (AbstractButton) c;
		
		if (button instanceof DefaultJButton) {
			button.setOpaque(false);
			this.paint(g, button);
			return;
		}
		
		if (button instanceof JButton) {
			JButton jb = (JButton) button;
			if (PulseTracker.isPulsating(jb)) {
				PulseTracker.update(jb);
			} else {
				// System.out.println(System.currentTimeMillis() + ":"
				// + button.getText());
			}
		}
		// if ("2".equals(button.getText()))
		// System.out.println(((AlphaComposite) TransitionLayout
		// .getAlphaComposite(button)).getAlpha()
		// + ":" + button.getClientProperty(TransitionLayout.ALPHA));
		this.delegate.updateBackground(g, button);
		this.paint(g, c);

		// Some ugly hack to allow fade-out of focus ring. The code
		// in BasicButtonUI doesn't call paintFocus() at all
		// when the component is not focus owner.
		AbstractButton b = (AbstractButton) c;
		FontMetrics fm = c.getFontMetrics(c.getFont());

		Insets i = c.getInsets();

		// Dimension size = new Dimension();
		Rectangle viewRect = new Rectangle();
		Rectangle iconRect = new Rectangle();
		Rectangle textRect = new Rectangle();
		viewRect.x = i.left;
		viewRect.y = i.top;
		viewRect.width = b.getWidth() - (i.right + viewRect.x);
		viewRect.height = b.getHeight() - (i.bottom + viewRect.y);

		textRect.x = textRect.y = textRect.width = textRect.height = 0;
		iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;

		Font f = c.getFont();
		g.setFont(f);

		// layout the text and icon
		@SuppressWarnings("unused")
		String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), b
				.getIcon(), b.getVerticalAlignment(), b
				.getHorizontalAlignment(), b.getVerticalTextPosition(), b
				.getHorizontalTextPosition(), viewRect, iconRect, textRect, b
				.getText() == null ? 0 : b.getIconTextGap());

		b.putClientProperty(ICON_RECT, iconRect);

		if (!(b.hasFocus() && b.isFocusPainted())) {
			if (FadeTracker.getInstance().isTracked(c, FadeKind.FOCUS))
				this.paintFocus(g, b, viewRect, textRect, iconRect);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.ComponentUI#getPreferredSize(javax.swing.JComponent)
	 */
	@Override
	public Dimension getPreferredSize(JComponent c) {
		AbstractButton button = (AbstractButton) c;
		SubstanceButtonShaper shaper = SubstanceCoreUtilities
				.getButtonShaper(button);

		return shaper.getPreferredSize(button, super.getPreferredSize(button));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#paintFocus(java.awt.Graphics,
	 *      javax.swing.AbstractButton, java.awt.Rectangle, java.awt.Rectangle,
	 *      java.awt.Rectangle)
	 */
	@Override
	protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
			Rectangle textRect, Rectangle iconRect) {
		
		FadeTracker fadeTracker = FadeTracker.getInstance();
		FocusKind focusKind = SubstanceCoreUtilities.getFocusKind(b);
		if ((focusKind == FocusKind.NONE)
				&& (!fadeTracker.isTracked(b, FadeKind.FOCUS)))
			return;

		Graphics2D graphics = (Graphics2D) g.create();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		float alpha = 1.0f;
		if (fadeTracker.isTracked(b, FadeKind.FOCUS)) {
			alpha = fadeTracker.getFade10(b, FadeKind.FOCUS) / 10.f;
		}
		graphics.setComposite(TransitionLayout.getAlphaComposite(b, alpha));

		ColorScheme currScheme = SubstanceCoreUtilities.getActiveScheme(b);
		if (currScheme == null)
			return;
		Shape contour = null;
		if (focusKind == FocusKind.TEXT) {
			graphics.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 0.0f, new float[] { 2.0f, 1.0f },
					0.0f));

			contour = BaseButtonShaper.getBaseOutline(textRect.width + 2,
					textRect.height, 2, null);

			Color color = SubstanceCoreUtilities
					.isThemeDark(SubstanceCoreUtilities.getActiveTheme(b, true)) ? SubstanceColorUtilities
					.getInterpolatedColor(currScheme.getUltraLightColor(),
							currScheme.getForegroundColor(), 0.4)
					: currScheme.getDarkColor();
			graphics.setColor(color);
			graphics.translate(textRect.x - 1, textRect.y);
			graphics.draw(contour);
			graphics.dispose();
			return;
		}

		if (focusKind == FocusKind.UNDERLINE) {
			graphics.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 0.0f, new float[] { 2.0f, 1.0f },
					0.0f));

			Color color = SubstanceCoreUtilities
					.isThemeDark(SubstanceCoreUtilities.getActiveTheme(b, true)) ? SubstanceColorUtilities
					.getInterpolatedColor(currScheme.getUltraLightColor(),
							currScheme.getForegroundColor(), 0.4)
					: currScheme.getDarkColor();
			graphics.setColor(color);
			graphics.translate(textRect.x - 1, textRect.y);
			graphics.drawLine(0, textRect.height - 1, textRect.width,
					textRect.height - 1);
			graphics.dispose();
			return;
		}

		if (focusKind == FocusKind.STRONG_UNDERLINE) {
			Color color = SubstanceCoreUtilities
					.isThemeDark(SubstanceCoreUtilities.getActiveTheme(b, true)) ? SubstanceColorUtilities
					.getInterpolatedColor(currScheme.getUltraLightColor(),
							currScheme.getForegroundColor(), 0.4)
					: currScheme.getDarkColor();
			graphics.setColor(color);
			graphics.translate(textRect.x - 1, textRect.y);
			graphics.drawLine(0, textRect.height - 1, textRect.width,
					textRect.height - 1);
			graphics.dispose();
			return;
		}

		SubstanceButtonShaper shaper = SubstanceCoreUtilities
				.getButtonShaper(b);
		if (shaper == null)
			return;
		Shape currClip = graphics.getClip();
		if ((focusKind == FocusKind.ALL) || (!shaper.isProportionate())) {
			graphics.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 0.0f, new float[] { 2.0f, 1.0f },
					0.0f));

			contour = shaper.getButtonOutline(b, null);

			int height = b.getHeight();
			int width = b.getWidth();
			// Draw border
			Color neg = SubstanceCoreUtilities.isThemeDark(SubstanceLookAndFeel
					.getTheme()) ? Color.white : Color.black;
			Color topBorderColor = neg;
			Color topBorderColor2 = currScheme.getUltraLightColor();
			Color midBorderColor = currScheme.getDarkColor();
			Color bottomBorderColor = currScheme.getUltraDarkColor();
			GradientPaint gradientBorderTop = new GradientPaint(0, 0,
					topBorderColor, 0, height / 2, midBorderColor);
			graphics.setPaint(gradientBorderTop);
			// fix for defect 114 - not ignoring current clip
			graphics.clipRect(0, 0, width, height / 2);
			graphics.draw(contour);

			graphics.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 0.0f, new float[] { 1.0f, 2.0f },
					1.0f));
			GradientPaint gradientBorderTop2 = new GradientPaint(0, 0,
					topBorderColor2, 0, height / 2, midBorderColor);
			graphics.setPaint(gradientBorderTop2);
			graphics.draw(contour);
			// fix for defect 114 - not ignoring current clip
			graphics.setClip(currClip);

			GradientPaint gradientBorderBottom = new GradientPaint(0,
					height / 2, midBorderColor, 0, height - 2,
					bottomBorderColor);
			graphics.setPaint(gradientBorderBottom);
			// fix for defect 114 - not ignoring current clip
			graphics.clipRect(0, height / 2, width, height - height / 2);
			graphics.draw(contour);
			graphics.dispose();

			return;
		}

		if (focusKind == FocusKind.ALL_INNER) {
			graphics.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_ROUND, 0.0f, new float[] { 2.0f, 1.0f },
					0.0f));

			Insets insets = (focusKind == FocusKind.ALL) ? null : new Insets(2,
					2, 2, 2);
			contour = shaper.getButtonOutline(b, insets);

			int height = b.getHeight();
			int width = b.getWidth();
			// Draw border
			Color topBorderColor = SubstanceCoreUtilities
					.isThemeDark(SubstanceLookAndFeel.getTheme()) ? Color.white
					: Color.black;
			Color midBorderColor = currScheme.getDarkColor();
			Color bottomBorderColor = currScheme.getUltraDarkColor();
			GradientPaint gradientBorderTop = new GradientPaint(0, 0,
					topBorderColor, 0, height / 2, midBorderColor);
			graphics.setPaint(gradientBorderTop);
			// fix for defect 114 - not ignoring current clip
			graphics.clipRect(0, 0, width, height / 2);
			graphics.draw(contour);
			// fix for defect 114 - not ignoring current clip
			graphics.setClip(currClip);

			GradientPaint gradientBorderBottom = new GradientPaint(0,
					height / 2, midBorderColor, 0, height - 2,
					bottomBorderColor);
			graphics.setPaint(gradientBorderBottom);
			// fix for defect 114 - not ignoring current clip
			graphics.clipRect(0, height / 2, width, height - height / 2);
			graphics.draw(contour);
			graphics.dispose();
			return;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#paintButtonPressed(java.awt.Graphics,
	 *      javax.swing.AbstractButton)
	 */
	@Override
	protected void paintButtonPressed(Graphics g, AbstractButton b) {
		// overriden to remove default metal effect
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.ComponentUI#contains(javax.swing.JComponent, int,
	 *      int)
	 */
	@Override
	public boolean contains(JComponent c, int x, int y) {
		return ButtonBackgroundDelegate.contains((JButton) c, x, y);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicButtonUI#paintIcon(java.awt.Graphics,
	 *      javax.swing.JComponent, java.awt.Rectangle)
	 */
	@Override
	protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
		Graphics2D graphics = (Graphics2D) g.create();
		FadeTracker fadeTracker = FadeTracker.getInstance();
		AbstractButton b = (AbstractButton) c;
		if ((ButtonBackgroundDelegate.getKind(b) == ButtonTitleKind.NONE)
		// && (b.getText() != null) && (b.getText().length() > 0)
				&& fadeTracker.isTracked(c, null,
						FadeKind.GHOSTING_ICON_ROLLOVER, true)) {
			float fade10 = fadeTracker.getFade10(c,
					FadeKind.GHOSTING_ICON_ROLLOVER);
			// 0.0 --> 0.5
			// 10.0 --> 0.0
			float opFactor = -0.5f * fade10 / 10.0f + 0.5f;
			graphics.setComposite(TransitionLayout.getAlphaComposite(c,
					opFactor));
			// System.out.println(opFactor);

			Icon icon = SubstanceCoreUtilities.getIcon(b);

			if (icon != null) {
				double iFactor = 1.0 + fade10 / 10.0;
				double iWidth = icon.getIconWidth() * iFactor;
				double iHeight = icon.getIconHeight() * iFactor;
				BufferedImage iImage = SubstanceCoreUtilities.getBlankImage(
						(int) iWidth, (int) iHeight);
				Graphics2D iGraphics = iImage.createGraphics();
				iGraphics.scale(iFactor, iFactor);
				icon.paintIcon(b, iGraphics, 0, 0);
				iGraphics.dispose();
				int dx = (int) ((iWidth - icon.getIconWidth()) / 2);
				int dy = (int) ((iHeight - icon.getIconHeight()) / 2);
				graphics.drawImage(iImage, iconRect.x - dx, iconRect.y - dy,
						null);
			}
		}

		graphics.setComposite(TransitionLayout.getAlphaComposite(c));
		super.paintIcon(graphics, c, iconRect);
		graphics.dispose();
	}
}
