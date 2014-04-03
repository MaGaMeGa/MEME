package edu.thirdparty;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** A simple easy-to-use Drag&Drop JTree with mouse-over and drop-under feedback.
* <p>
* Use it exactly the same way you use a JTree. No gimmicks of Transferable,
* TransferHandler ...
* 
* This version only supports MOVE within the tree.
* 
* To make a node not accepting drop, simply let its getAllowsChildren() return false.
* To customize TreeCellRenderer, extends the DnDTree.DnDTreeCellRenderer instead of
* the DefaultTreeCellRenderer.
* 
* Have to use many back doors. So make no sense to support a DataFlavor.
* It is more useful to provide visual feedback during D&D within a JTree, 
* than to accept object from other sources.
* 
* @version 0.02 03/20/2005
* @author Jing Ding
* 
* 06/05/2007 : modified by Bocsi Rajmund
*    > root node is not draggable
*    > if new child is an ancestor there is no move instead calls the illegalDrop() method.
*      This method does nothing by default but any descendant overwrite it to display
*      the error for the user
*    > popup menu with Cut and Paste
*    > specialized tree: everyone (except root) can have zero or one children. 
*    
* 10/15/2009 : modified by Bocsi Rajmund
*    > drop and paste invokes an own Drop event
*    > defining the Drop event, listener and related methods
*/
@SuppressWarnings("serial")
public class DnDTree extends JTree {
	
	//====================================================================================================
	// members
	
	/** The Drop position. */
	@SuppressWarnings("unused")
	private DropTarget dropTarget; 				// The Drop position.
	
	/** The Drag node. */
	private DragSource dragSource;				// The Drag node.
	
	/** The tree model for the tree. */
	private DefaultTreeModel treemodel;			// The TreeModel for the tree.
	
	/** Back door for cursor change. */
	private DragSourceContext dsc;				// Back door for cursor change.
	
	/** Back door for cell renderer. */
	private int highlightRow = -1;				// Back door for cellrenderer.
	
	/** Back door for transfer node. */
	private DefaultMutableTreeNode dragnode;	// Back door for transfer node.
	
	/** Context menu. */
	private JPopupMenu contextMenu = new JPopupMenu();
	
	/** The last cut-out node. */
	private DefaultMutableTreeNode cutnode;
	
	private List<DnDTreeDropSuccessfulListener> dropSuccessfulListeners = null;

	/** Dummy data to start drag. */
	private static Transferable dummy = new StringSelection("");	

	/** Initiate drag events. */
	private DragGestureListener dgListener = new DragGestureListener() {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public void dragGestureRecognized(DragGestureEvent event){
			TreePath selectedPath = getSelectionPath();
			if (selectedPath != null){
				dragnode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
				if (dragnode.getParent() != null)
					dragSource.startDrag(event,DragSource.DefaultMoveDrop,dummy,dsListener);
			}
		}
	};

	/** Handle drag events, especially cursor change. */
	private DragSourceListener dsListener = new DragSourceAdapter() {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		/* Leave a back door for cursor change. During a D&D process, only the
		 * DragSourceContext (dsc) can change cursor. However, the DropTargetListener, 
		 * which is responsible to determine if drop is accepted, doesn't know the dsc.
		 * For D&D implementations involving many different components, the back door 
		 * should be accessible to all drop targets.  
		 *  (non-Javadoc)
		 * @see java.awt.dnd.DragSourceListener#dragEnter(java.awt.dnd.DragSourceDragEvent)
		 */
		@Override
		public void dragEnter(DragSourceDragEvent event){
			dsc = event.getDragSourceContext();
		}

		//----------------------------------------------------------------------------------------------------
		/*
		 * Move tree source node through a back door to the target node.
		 * So D&D's cleanup is not necessary. 
		 */
		//	public void dragDropEnd(DragSourceDropEvent event){
		//		if(event.getDropSuccess()){
		//		  int act = event.getDropAction();
		//		  if(act == DnDConstants.ACTION_MOVE){
		//		    System.err.println("Remove " + dragnode + " from parent");
		//		    treemodel.removeNodeFromParent(dragnode);
		//		  }
		//    }
		//	}
	};

	/** Handle drop events */
	private DropTargetListener dtListener = new DropTargetAdapter() {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		/*	Check if the node accepts drop.
		 * 	  (non-Javadoc)
		 * @see java.awt.dnd.DropTargetListener#dragOver(java.awt.dnd.DropTargetDragEvent)
		 */
		@Override
		public void dragOver(DropTargetDragEvent event){
			DefaultMutableTreeNode n = mouseOnNode(event);
			if (n != null && n.getAllowsChildren()) {
				dsc.setCursor(DragSource.DefaultMoveDrop);
				event.acceptDrag(event.getDropAction());
				highlight(event);
			} else {
				dsc.setCursor(DragSource.DefaultMoveNoDrop);
				event.rejectDrag();
				highlightRow = -1;
			}
			repaint();	// evoke TreeCellRenderer to draw a red border.
		}
	
		//----------------------------------------------------------------------------------------------------
		public void drop(DropTargetDropEvent event) {
			DefaultMutableTreeNode parent = mouseOnNode(event);
			//System.err.println(dragnode + " dropped on " + parent);
			highlightRow = -1;
			if (parent != null) {
				if (parent.equals(dragnode)) {
					repaint();
					return;
				}
				
				//////////////////////////////////////////////////////////////////
				// MODIFICATION                                                 //
				// Every node (except root) has zero or one children            //
				//////////////////////////////////////////////////////////////////
				
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) treemodel.getRoot();
				if (!parent.equals(root)) {
					while (parent.getChildCount() > 0) 
						parent = (DefaultMutableTreeNode) parent.getFirstChild();
				}
				
				//////////////////////////////////////////////////////////////////
				// END OF MODIFICATION                                          //
				//////////////////////////////////////////////////////////////////
				
				DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) dragnode.getParent();
				int oldIndex = oldParent.getIndex(dragnode);
				try {
					/* Get around D&D's transferable mechnism to re-use the dragnode.
					 * The transferable mechnism creates a clone of the original node.
					 */
					treemodel.removeNodeFromParent(dragnode);
					treemodel.insertNodeInto(dragnode,parent,parent.getChildCount());
					event.dropComplete(true);
					fireDropSuccessfulEvent(oldParent,parent,dragnode);
				} catch (IllegalArgumentException e) {
					treemodel.insertNodeInto(dragnode,oldParent,oldIndex);
					event.rejectDrop();
					illegalDrop(e);
				} catch(Exception e) {
					// TODO this should probably be a MEMEApp.log()
					e.printStackTrace();
				}
			} else
				event.rejectDrop();
			}
		};
		
		//====================================================================================================
		// methods

		//----------------------------------------------------------------------------------------------------
		/** Returns a new instance of the DnDTree for the specified TreeModel. */
		public DnDTree(DefaultTreeModel model) {
			super(model);
			treemodel = model;
			dropSuccessfulListeners = new ArrayList<DnDTreeDropSuccessfulListener>();
			dropTarget = new DropTarget(this,dtListener);
			dragSource = DragSource.getDefaultDragSource();
			dragSource.createDefaultDragGestureRecognizer(this,DnDConstants.ACTION_MOVE,dgListener);
			setCellRenderer(new DnDTreeCellRenderer());
			initializeContextMenu();
		}

		//----------------------------------------------------------------------------------------------------
		/* (non-Javadoc)
		 * 	@see javax.swing.JTree#setModel(javax.swing.tree.TreeModel)
		 */
		public void setModel(DefaultTreeModel model) {
			super.setModel(treemodel = model);
		}

		//----------------------------------------------------------------------------------------------------
		/**
		 *  Leave a back door for TreeCellRenderer to highlight the node.
		 */
		public int getHighlightedRow(){ return highlightRow; }
		
		//----------------------------------------------------------------------------------------------------
		public JPopupMenu getContextMenu() { return contextMenu; }
		
		//----------------------------------------------------------------------------------------------------
		public void addDropSuccessfulListener(DnDTreeDropSuccessfulListener listener) { dropSuccessfulListeners.add(listener); }
		public void removeDropSuccessfulListner(DnDTreeDropSuccessfulListener listener) { dropSuccessfulListeners.remove(listener); }
		
		//====================================================================================================
		// assistant methods
		
		//----------------------------------------------------------------------------------------------------
		/** Invokes when an ancestor of the node would be the new child of the node. */
		protected void illegalDrop(Exception e) {}
		
		//----------------------------------------------------------------------------------------------------
		protected boolean mouseReleasedCondition() { return true; }
		
		//----------------------------------------------------------------------------------------------------
		private void fireDropSuccessfulEvent(final DefaultMutableTreeNode oldParent, final DefaultMutableTreeNode newParent, 
											 final DefaultMutableTreeNode target) {
			final DnDTreeDropSuccessfulEvent event = new DnDTreeDropSuccessfulEvent(oldParent,newParent,target);
			for (int i = dropSuccessfulListeners.size() - 1;i >= 0;dropSuccessfulListeners.get(i--).dropSuccessful(event));
		}
		
		//----------------------------------------------------------------------------------------------------
		private void initializeContextMenu() {
			JMenuItem item = new JMenuItem("Cut");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TreePath selectedPath = getSelectionPath();
					if (selectedPath != null)
						cutnode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
				}
			});
			item.setName("btn_cut");
			contextMenu.add(item);
			item = new JMenuItem("Paste");
			item.setName("btn_paste");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getSelectionPath().getLastPathComponent();
					if (parent != null){
						if (parent.equals(cutnode)) return;

						//////////////////////////////////////////////////////////////////
						// MODIFICATION                                                 //
						// Every node (except root) has zero or one children            //
						//////////////////////////////////////////////////////////////////
						
						DefaultMutableTreeNode root = (DefaultMutableTreeNode) treemodel.getRoot();
						if (!parent.equals(root)) {
							while (parent.getChildCount() > 0) 
								parent = (DefaultMutableTreeNode)parent.getFirstChild();
						}
						
						//////////////////////////////////////////////////////////////////
						// END OF MODIFICATION                                          //
						//////////////////////////////////////////////////////////////////
						
						DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) cutnode.getParent();
						int oldIndex = oldParent.getIndex(cutnode);
						try {
							treemodel.removeNodeFromParent(cutnode);
							treemodel.insertNodeInto(cutnode,parent,parent.getChildCount());
							fireDropSuccessfulEvent(oldParent,parent,cutnode);
						} catch (IllegalArgumentException e1) {
							treemodel.insertNodeInto(cutnode,oldParent,oldIndex);
							illegalDrop(e1);
						} catch (Exception e1) {}
						cutnode = null;
					}
				}
			});
			contextMenu.add(item);
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (mouseReleasedCondition()) { 
						if (SwingUtilities.isRightMouseButton(e) && e.getComponent().isEnabled()) {
							DefaultMutableTreeNode node = mouseOnNode(e);
							if (node == null) 
								node = (DefaultMutableTreeNode) getPathForRow(getRowCount() - 1).getLastPathComponent();
							setSelectionPath(new TreePath(node.getPath()));
							contextMenu.getComponent(0).setEnabled(node.getParent() != null);
							contextMenu.getComponent(1).setEnabled(cutnode != null);
							contextMenu.show(e.getComponent(),e.getX(),e.getY());
						}
					}
				}
			});
		}

		//----------------------------------------------------------------------------------------------------
		/** Marks the location of the event to highlighting. */
		private void highlight(DropTargetDragEvent event) {
			Point loc = event.getLocation();
			highlightRow = getRowForLocation(loc.x,loc.y);
		}

		//----------------------------------------------------------------------------------------------------
		/** Returns the node that belongs to the location of the event. */
		private DefaultMutableTreeNode mouseOnNode(DropTargetDropEvent event){
			Point loc = event.getLocation();
			TreePath path = getPathForLocation(loc.x,loc.y);
			if (path != null)
				return (DefaultMutableTreeNode) path.getLastPathComponent();
			return null;
		}

		//----------------------------------------------------------------------------------------------------
		/** Returns the node that belongs to the location of the event. */
		private DefaultMutableTreeNode mouseOnNode(DropTargetDragEvent event){
			Point loc = event.getLocation();
			TreePath path = getPathForLocation(loc.x,loc.y);
			if (path != null)
				return (DefaultMutableTreeNode) path.getLastPathComponent();
			return null;
		}
		
		//----------------------------------------------------------------------------------------------------
		/** Returns the node that belongs to the location of the event. */
		private DefaultMutableTreeNode mouseOnNode(MouseEvent event) {
			Point loc = event.getPoint();
			TreePath path = null; 
			for (int i = 20;path == null && i <= loc.x;path = getPathForLocation(i,loc.y),i += 20);
			if (path != null)
				return (DefaultMutableTreeNode) path.getLastPathComponent();
			return null;
		}
		
		//====================================================================================================
		// nested classes
		
		//----------------------------------------------------------------------------------------------------
		public static class DnDTreeDropSuccessfulEvent {
			
			//====================================================================================================
			// members
			
			private final DefaultMutableTreeNode oldParent, newParent, target;
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			public DnDTreeDropSuccessfulEvent(final DefaultMutableTreeNode oldParent, final DefaultMutableTreeNode newParent,
											  final DefaultMutableTreeNode target) {
				this.oldParent = oldParent;
				this.newParent = newParent;
				this.target = target;
			}
			
			//----------------------------------------------------------------------------------------------------
			public DefaultMutableTreeNode getOldParent() { return oldParent; }
			public DefaultMutableTreeNode getNewParent() { return newParent; }
			public DefaultMutableTreeNode getTarget() { return target; }
		}
		
		//----------------------------------------------------------------------------------------------------
		public static interface DnDTreeDropSuccessfulListener {
			
			//====================================================================================================
			// methods
			public void dropSuccessful(DnDTreeDropSuccessfulEvent e);
		}

		//----------------------------------------------------------------------------------------------------
		/** This class renders the elements of the tree. */
		@SuppressWarnings("serial")
		public static class DnDTreeCellRenderer extends DefaultTreeCellRenderer {
			
			//====================================================================================================
			// members
			
			private Border highlightFrame = new LineBorder(Color.BLACK); 
			private Border noFrame = BorderFactory.createEmptyBorder(); 

			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value,	boolean sel, boolean expanded, boolean leaf, int row,
														  boolean focus) {
				Component com = super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,focus);
				int highlightRow = ((DnDTree)tree).getHighlightedRow();
				if (row == highlightRow)
					((JLabel)com).setBorder(highlightFrame);
				else
					((JLabel)com).setBorder(noFrame);
				return com;
			}
		}
}
