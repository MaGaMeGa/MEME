/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.gui;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.*;

//-----------------------------------------------------------------------------
/**
 * This class implements a container that can contain one child only,
 * and allows dragging it when the size of the container is smaller 
 * than the minimum size of the child, similarly to 3DStudio MAX's panels.
 */
@SuppressWarnings("serial")
public class DraggableViewport extends JViewport implements MouseListener, MouseMotionListener
{
	java.awt.Point lastmp = null;

	public DraggableViewport(java.awt.Component content) { setView(content); }

	@Override
	public void setView(Component view) {
		super.setView(view);
		if (view != null) {
			addMouseMotionListener(this);
			addMouseListener(this);
		} else {
			removeMouseMotionListener(this);
			removeMouseListener(this);
		}
	}

	public void mouseMoved(MouseEvent e)	{}
	public void mouseClicked(MouseEvent e)	{}
	public void mouseEntered(MouseEvent e)	{}
	public void mouseExited(MouseEvent e)	{}
	public void mousePressed(MouseEvent e)	{}

	public void mouseReleased(MouseEvent e) {
		if ((e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) != 0)
			lastmp = null;
	}

	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiers() & java.awt.event.InputEvent.BUTTON1_MASK) == 0)
			return;
		if (lastmp == null) {
			lastmp = e.getPoint();
		} else {
			translate(e.getPoint().x - lastmp.x, e.getPoint().y - lastmp.y); 
			lastmp = e.getPoint();
		}
		e.consume();
	}

	private void translate(int dx, int dy) {
		java.awt.Point p = getViewPosition();
		p.x -= dx;
		p.y -= dy;
		if (p.x <= 0) p.x = 0; 
		else {
			int maxx = getView().getWidth()  - getWidth();
			if (p.x > maxx) p.x = maxx;
		}
		if (p.y <= 0) p.y = 0;
		else {
			int maxy = getView().getHeight() - getHeight();
			if (p.y > maxy) p.y = maxy;
		}
		setViewPosition(p);
	}
} 

