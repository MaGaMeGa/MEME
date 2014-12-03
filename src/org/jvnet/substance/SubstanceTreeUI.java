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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.jvnet.lafwidget.layout.TransitionLayout;
import org.jvnet.lafwidget.utils.FadeTracker;
import org.jvnet.lafwidget.utils.FadeTracker.FadeKind;
import org.jvnet.lafwidget.utils.FadeTracker.FadeTrackerCallback;
import org.jvnet.substance.utils.SubstanceCoreUtilities;

import ai.aitia.meme.paramsweep.gui.component.ListAsATree;

/**
 * UI for lists in <b>Substance</b> look and feel.
 * 
 * @author Kirill Grouchnikov
 * 
 * modified by Rajmund Bocsi
 */
public class SubstanceTreeUI extends BasicTreeUI {
	/**
	 * Name for the internal client property that holds the list of currently
	 * selected paths.
	 */
	public static String SELECTED_INDICES = "substancelaf.internal.treeSelectedIndices";

	/**
	 * Name for the internal client property that holds the currently
	 * rolled-over path.
	 */
	public static String ROLLED_OVER_INDEX = "substancelaf.internal.treeRolledOverIndex";

	/**
	 * Delegate for painting the background.
	 */
	private static SubstanceGradientBackgroundDelegate backgroundDelegate = new SubstanceGradientBackgroundDelegate();

	/**
	 * Listener that listens to changes on
	 * {@link SubstanceLookAndFeel#WATERMARK_TO_BLEED} property.
	 */
	protected PropertyChangeListener substancePropertyChangeListener;

	/**
	 * Listener for selection animations.
	 */
	protected TreeSelectionListener substanceSelectionFadeListener;

	/**
	 * Listener for fade animations on tree rollovers.
	 */
	protected RolloverFadeListener substanceFadeRolloverListener;

	/**
	 * Listener for selection of an entire row.
	 */
	protected MouseListener substanceRowSelectionListener;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.ComponentUI#createUI(javax.swing.JComponent)
	 */
	public static ComponentUI createUI(JComponent tree) {
		return new SubstanceTreeUI();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#installDefaults()
	 */
	@Override
	protected void installDefaults() {
		super.installDefaults();
		if (SubstanceCoreUtilities.toBleedWatermark(this.tree))
			this.tree.setOpaque(false);

		Map<TreePathId, Object> selected = new HashMap<TreePathId, Object>();
		if (this.tree.getSelectionPaths() != null) {
			for (TreePath selectionPath : this.tree.getSelectionPaths()) {
				selected.put(new TreePathId(selectionPath), selectionPath
						.getLastPathComponent());
			}
		}
		this.tree.putClientProperty(SELECTED_INDICES, selected);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#uninstallDefaults()
	 */
	@Override
	protected void uninstallDefaults() {
		this.tree.putClientProperty(SELECTED_INDICES, null);
		super.uninstallDefaults();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#paintRow(java.awt.Graphics,
	 *      java.awt.Rectangle, java.awt.Insets, java.awt.Rectangle,
	 *      javax.swing.tree.TreePath, int, boolean, boolean, boolean)
	 */
	@Override
	protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets,
			Rectangle bounds, TreePath path, int row, boolean isExpanded,
			boolean hasBeenExpanded, boolean isLeaf) {
		// Don't paint the renderer if editing this row.
		if ((this.editingComponent != null) && (this.editingRow == row))
			return;

		int leadIndex;

		if (this.tree.hasFocus()) {
			TreePath leadPath = this.tree.getLeadSelectionPath();
			leadIndex = this.getRowForPath(this.tree, leadPath);
		} else {
			leadIndex = -1;
		}

		Component component;

		component = this.currentCellRenderer.getTreeCellRendererComponent(
				this.tree, path.getLastPathComponent(), this.tree
						.isRowSelected(row), isExpanded, isLeaf, row,
				(leadIndex == row));

		boolean isWatermarkBleed = SubstanceCoreUtilities
				.toBleedWatermark(tree);

		TreePathId pathId = new TreePathId(path);
		boolean isSelectedAnim = FadeTracker.getInstance().isTracked(this.tree,
				pathId, FadeKind.SELECTION);
		boolean isRolloverAnim = FadeTracker.getInstance().isTracked(this.tree,
				pathId, FadeKind.ROLLOVER);
		TreePathId currRoPath = (TreePathId) tree
				.getClientProperty(ROLLED_OVER_INDEX);
		boolean isRollover = ((currRoPath != null) && (currRoPath.path
				.equals(path)));

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setComposite(TransitionLayout.getAlphaComposite(this.tree));
		if ((!isWatermarkBleed) && (component.getBackground() != null)) {
			g2d.setColor(component.getBackground());
			if (this.tree.getComponentOrientation().isLeftToRight()) {
				g2d.fillRect(bounds.x, bounds.y, clipBounds.width,
						bounds.height);
			} else {
				g2d.fillRect(clipBounds.x, bounds.y, bounds.width,
						bounds.height);
			}
		}
		float alphaForBackground = 0.0f;

		// Support for selection animations (from 3.1)
		if (this.tree.isRowSelected(row) || isSelectedAnim) {
			if (isSelectedAnim) {
				// set the alpha for selection animation
				float fadeCoef = FadeTracker.getInstance().getFade10(this.tree,
						pathId, FadeKind.SELECTION);
				alphaForBackground = 0.7f * fadeCoef / 10.0f;
			} else {
				alphaForBackground = 0.7f;
			}
		}
		// Support for rollover animations (from 3.1)
		if (isRolloverAnim) {
			// set the alpha for rollover animation
			float fadeCoef = FadeTracker.getInstance().getFade10(this.tree,
					pathId, FadeKind.ROLLOVER);
			// System.out.println("Has rollover anim on " + row + "["
			// + fadeCoef + "] : " + cx + ":" + cy + "-" + cw + ":"
			// + ch);
			alphaForBackground = Math.max(alphaForBackground,
					0.4f * fadeCoef / 10.0f);
		} else {
			if (isRollover) {
				alphaForBackground = Math.max(alphaForBackground, 0.4f);
			}
		}

		// The DefaultTreeCellRenderer overrides the isOpaque method
		// so that there is no point in trying to make it non-opaque.
		// Fix for defect 181.
		boolean canHaveSubstanceEffects = !(component instanceof DefaultTreeCellRenderer);

		if (canHaveSubstanceEffects && (alphaForBackground > 0.0f)) {
			g2d.setComposite(TransitionLayout.getAlphaComposite(this.tree,
					alphaForBackground));
			// Fix for defect 180 (old code in comments) - painting the
			// highlight beneath the entire row
			backgroundDelegate.update(g2d, component, new Rectangle(this.tree
					.getInsets().left /* bounds.x */, bounds.y,
					this.tree.getWidth() - this.tree.getInsets().right
							- this.tree.getInsets().left/* bounds.x */,
					bounds.height), SubstanceCoreUtilities.getTheme(this.tree,
					true).getHighlightBackgroundTheme(), true);
			g2d.setComposite(TransitionLayout.getAlphaComposite(this.tree));
		}

		if (component instanceof JComponent) {
			// Play with opaqueness to make our own gradient background
			// on selected elements to show.
			JComponent jRenderer = (JComponent) component;
			synchronized (jRenderer) {
				boolean newOpaque = !this.tree.isRowSelected(row);
				if (SubstanceCoreUtilities.toBleedWatermark(this.tree))
					newOpaque = false;

				// fix for defect 181 - no highlight on renderers
				// that extend DefaultTreeCellRenderer
				newOpaque = newOpaque && canHaveSubstanceEffects;

				Map<Component, Boolean> opacity = new HashMap<Component, Boolean>();
				if (!newOpaque)
					SubstanceCoreUtilities.makeNonOpaque(jRenderer, opacity);
				this.rendererPane.paintComponent(g2d, component, this.tree,
						bounds.x, bounds.y, Math.max(this.tree.getWidth()
								- this.tree.getInsets().right
								- this.tree.getInsets().left - bounds.x,
								bounds.width), bounds.height, true);
				if (!newOpaque)
					SubstanceCoreUtilities.restoreOpaque(jRenderer, opacity);
			}
		} else {
			this.rendererPane.paintComponent(g2d, component, this.tree,
					bounds.x, bounds.y, Math
							.max(clipBounds.width, bounds.width),
					bounds.height, true);
		}

		// Paint the expand control once again since it has been overlayed
		// by the highlight background on selected and rolled over rows.
		if (shouldPaintExpandControl(path, row, isExpanded, hasBeenExpanded,
				isLeaf)) {
			if (!this.tree.getComponentOrientation().isLeftToRight()) {
				bounds.x -= 4;
			}
			paintExpandControl(g2d, clipBounds, insets, bounds, path, row,
					isExpanded, hasBeenExpanded, isLeaf);
		}

		// g2d.setColor(Color.blue);
		// g2d.draw(bounds);
		g2d.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#createDefaultCellRenderer()
	 */
	@Override
	protected TreeCellRenderer createDefaultCellRenderer() {
		return new SubstanceDefaultTreeCellRenderer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#installListeners()
	 */
	@Override
	protected void installListeners() {
		super.installListeners();
		this.substancePropertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (SubstanceLookAndFeel.WATERMARK_TO_BLEED.equals(evt
						.getPropertyName())) {
					tree.setOpaque(!SubstanceCoreUtilities
							.toBleedWatermark(tree));
				}
			}
		};
		this.tree
				.addPropertyChangeListener(this.substancePropertyChangeListener);

		this.substanceSelectionFadeListener = new MyTreeSelectionListener();
		this.tree.getSelectionModel().addTreeSelectionListener(
				this.substanceSelectionFadeListener);

		this.substanceRowSelectionListener = new RowSelectionListener();
		this.tree.addMouseListener(this.substanceRowSelectionListener);

		// Add listener for the fade animation
		this.substanceFadeRolloverListener = new RolloverFadeListener();
		this.tree.addMouseMotionListener(this.substanceFadeRolloverListener);
		this.tree.addMouseListener(this.substanceFadeRolloverListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.plaf.basic.BasicTreeUI#uninstallListeners()
	 */
	@Override
	protected void uninstallListeners() {
		this.tree.removeMouseListener(this.substanceRowSelectionListener);
		this.substanceRowSelectionListener = null;

		this.tree.getSelectionModel().removeTreeSelectionListener(
				this.substanceSelectionFadeListener);
		this.substanceSelectionFadeListener = null;

		this.tree
				.removePropertyChangeListener(this.substancePropertyChangeListener);
		this.substancePropertyChangeListener = null;

		// Remove listener for the fade animation
		this.tree.removeMouseMotionListener(this.substanceFadeRolloverListener);
		this.tree.removeMouseListener(this.substanceFadeRolloverListener);
		this.substanceFadeRolloverListener = null;

		super.uninstallListeners();
	}

	/**
	 * ID of a single tree path.
	 * 
	 * @author Kirill Grouchnikov
	 */
	@SuppressWarnings({ "rawtypes" })
	protected static class TreePathId implements Comparable {
		/**
		 * Tree path.
		 */
		protected TreePath path;

		/**
		 * Creates a tree path ID.
		 * 
		 * @param path
		 *            Tree path.
		 */
		public TreePathId(TreePath path) {
			this.path = path;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof TreePathId) {
				TreePathId otherId = (TreePathId) o;
				Object[] path1Objs = this.path.getPath();
				Object[] path2Objs = otherId.path.getPath();
				if (path1Objs.length != path2Objs.length)
					return 1;
				for (int i = 0; i < path1Objs.length; i++)
					if (!path1Objs[i].equals(path2Objs[i]))
						return 1;
				return 0;
			}
			return -1;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return this.compareTo(obj) == 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			Object[] pathObjs = this.path.getPath();
			int result = pathObjs[0].hashCode();
			for (int i = 1; i < pathObjs.length; i++)
				result = result ^ pathObjs[i].hashCode();
			return result;
		}
	}

	/**
	 * Selection listener for selection animation effects.
	 * 
	 * @author Kirill Grouchnikov
	 */
	protected class MyTreeSelectionListener implements TreeSelectionListener {
		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
		 */
		@SuppressWarnings("unchecked")
		public void valueChanged(TreeSelectionEvent e) {
			Map<TreePathId, Object> currSelected = (Map<TreePathId, Object>) tree
					.getClientProperty(SELECTED_INDICES);
			if (tree.getSelectionPaths() != null) {
				for (TreePath selectionPath : tree.getSelectionPaths()) {
					TreePathId pathId = new TreePathId(selectionPath);

					// check if was selected before
					if (!currSelected.containsKey(pathId)) {
						// start fading in
						// System.out.println("Fade in on index " + i);
						FadeTracker.getInstance().trackFadeIn(
								FadeKind.SELECTION, tree, pathId, false,
								new PathRepaintCallback(tree, selectionPath));
						currSelected.put(pathId, selectionPath
								.getLastPathComponent());
					}
				}
			}

			for (Iterator<Map.Entry<TreePathId, Object>> it = currSelected
					.entrySet().iterator(); it.hasNext();) {
				Map.Entry<TreePathId, Object> entry = it.next();
				if (tree.getSelectionModel()
						.isPathSelected(entry.getKey().path))
					continue;
				// fade out for deselected path
				FadeTracker.getInstance().trackFadeOut(FadeKind.SELECTION,
						tree, entry.getKey(), false,
						new PathRepaintCallback(tree, entry.getKey().path));
				it.remove();
			}

			//						
			//						
			//						
			// // check if was selected before and still points to
			// // the same element
			// if (currSelected.containsKey(cellId)) {
			// if (currSelected.get(cellId).equals(
			// table.getValueAt(i, j))) {
			// // start fading out
			// // System.out.println("Fade out on index " + i
			// // + ":" + j);
			// FadeTracker.getInstance().trackFadeOut(
			// FadeKind.SELECTION, table, cellId,
			// false,
			// new CellRepaintCallback(table, i, j));
			// }
			// currSelected.remove(cellId);
			// }
			// }
			// }
			// }
		}
	}

	/**
	 * Repaints a single path during the fade animation cycle.
	 * 
	 * @author Kirill Grouchnikov
	 */
	protected class PathRepaintCallback implements FadeTrackerCallback {
		/**
		 * Associated tree.
		 */
		protected JTree tree;

		/**
		 * Associated (animated) path.
		 */
		protected TreePath treePath;

		/**
		 * Creates a new animation repaint callback.
		 * 
		 * @param tree
		 *            Associated tree.
		 * @param treePath
		 *            Associated (animated) path.
		 */
		public PathRepaintCallback(JTree tree, TreePath treePath) {
			super();
			this.tree = tree;
			this.treePath = treePath;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.jvnet.lafwidget.utils.FadeTracker$FadeTrackerCallback#fadeEnded(org.jvnet.lafwidget.utils.FadeTracker.FadeKind)
		 */
		public void fadeEnded(FadeKind fadeKind) {
			this.repaintPath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.jvnet.lafwidget.utils.FadeTracker$FadeTrackerCallback#fadePerformed(org.jvnet.lafwidget.utils.FadeTracker.FadeKind,
		 *      float)
		 */
		public void fadePerformed(FadeKind fadeKind, float fade10) {
			this.repaintPath();
		}

		/**
		 * Repaints the associated path.
		 */
		private void repaintPath() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (SubstanceTreeUI.this.tree == null) {
						// may happen if the LAF was switched in the meantime
						return;
					}

					Rectangle boundsBuffer = new Rectangle();
					Rectangle bounds = treeState.getBounds(treePath,
							boundsBuffer);

					if (bounds != null) {
						// still visible

						// fix for defect 180 - refresh the entire row
						bounds.x = 0;
						bounds.width = tree.getWidth();

						// fix for defect 188 - rollover effects for trees
						// with insets
						Insets insets = tree.getInsets();
						bounds.x += insets.left;
						bounds.y += insets.top;

						tree.repaint(bounds);
					}
				}
			});
		}
	}

	/**
	 * Listener for rollover animation effects.
	 * 
	 * @author Kirill Grouchnikov
	 */
	private class RolloverFadeListener implements MouseListener,
			MouseMotionListener {
		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
			// if (SubstanceCoreUtilities.toBleedWatermark(list))
			// return;

			if (!tree.isEnabled())
				return;
			this.fadeOut();
			// System.out.println("Nulling RO index");
			tree.putClientProperty(ROLLED_OVER_INDEX, null);
		}

		public void mouseMoved(MouseEvent e) {
			// if (SubstanceCoreUtilities.toBleedWatermark(list))
			// return;

			if (!tree.isEnabled())
				return;
			handleMove(e);
		}

		public void mouseDragged(MouseEvent e) {
			// if (SubstanceCoreUtilities.toBleedWatermark(list))
			// return;

			if (!tree.isEnabled())
				return;
			handleMove(e);
		}

		/**
		 * Handles various mouse move events and initiates the fade animation if
		 * necessary.
		 * 
		 * @param e
		 *            Mouse event.
		 */
		private void handleMove(MouseEvent e) {
			TreePath closestPath = tree.getClosestPathForLocation(e.getX(), e
					.getY());
			Rectangle bounds = tree.getPathBounds(closestPath);
			if (bounds == null) {
				this.fadeOut();
				tree.putClientProperty(ROLLED_OVER_INDEX, null);
				return;
			}
			if ((e.getY() < bounds.y)
					|| (e.getY() > (bounds.y + bounds.height))) {
				this.fadeOut();
				tree.putClientProperty(ROLLED_OVER_INDEX, null);
				return;
			}
			// check if this is the same index
			TreePathId newPathId = new TreePathId(closestPath);
			TreePathId currPathId = (TreePathId) tree
					.getClientProperty(ROLLED_OVER_INDEX);
			if ((currPathId != null) && newPathId.equals(currPathId)) {
				// System.out.println("Same location " +
				// System.currentTimeMillis());
				// System.out.print("Current : ");
				// for (Object o1 : currPathId.path.getPath()) {
				// System.out.print(o1);
				// }
				// System.out.println("");
				// System.out.print("Closest : ");
				// for (Object o2 : newPathId.path.getPath()) {
				// System.out.print(o2);
				// }
				// System.out.println("");
				return;
			}

			this.fadeOut();
			FadeTracker.getInstance().trackFadeIn(FadeKind.ROLLOVER, tree,
					newPathId, false,
					new PathRepaintCallback(tree, closestPath));
			// System.out.println("Setting RO index to " + roIndex);
			tree.putClientProperty(ROLLED_OVER_INDEX, newPathId);
		}

		/**
		 * Initiates the fade out effect.
		 */
		private void fadeOut() {
			TreePathId prevRoPath = (TreePathId) tree
					.getClientProperty(ROLLED_OVER_INDEX);
			if (prevRoPath == null)
				return;

			FadeTracker.getInstance().trackFadeOut(FadeKind.ROLLOVER, tree,
					prevRoPath, false,
					new PathRepaintCallback(tree, prevRoPath.path));
		}
	}

	/**
	 * Listener for selecting the entire rows.
	 * 
	 * @author Kirill Grouchnikov
	 */
	private class RowSelectionListener extends MouseAdapter {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent e) {
			if (!tree.isEnabled())
				return;
			TreePath closestPath = tree.getClosestPathForLocation(e.getX(), e
					.getY());
			if (closestPath == null)
				return;
			Rectangle bounds = tree.getPathBounds(closestPath);
			// Process events outside the immediate bounds - fix for defect
			// 19 on substance-netbeans. This properly handles Ctrl and Shift
			// selections on trees.
			if ((e.getY() >= bounds.y)
					&& (e.getY() < (bounds.y + bounds.height))
					&& ((e.getX() < bounds.x) || (e.getX() > (bounds.x + bounds.width)))) {
				// tree.setSelectionPath(closestPath);

				// fix - don't select a node if the click was on the
				// expand control
				if (isLocationInExpandControl(closestPath, e.getX(), e.getY()))
					return;
				selectPathForEvent(closestPath, e);
			}
		}
	}
	
	@Override
	protected void paintHorizontalLine(Graphics g, JComponent c, int y, int left, int right) {
		if (!(c instanceof ListAsATree)) {
			super.paintHorizontalLine(g, c, y, left, right);
		}
	}
	
	@Override
	protected void paintVerticalLine(Graphics g, JComponent c, int x, int top, int bottom) {
		if (!(c instanceof ListAsATree)) {
			super.paintVerticalLine(g, c, x, top, bottom);
		}
	}
}
