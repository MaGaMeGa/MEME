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
package ai.aitia.meme.gui.lop;

/** This generic class represents a task. It implements the Callable<E>, ILopListener and IKnowsReqAndGUI
 *  interfaces. Its call() method does nothing so you should override it in the descendant classes.
 */
public class LongCallable<E> implements java.util.concurrent.Callable<E>, ILopListener, IKnowsReqAndGUI {
	/** The request which contains 'this' task. */
	private volatile RQ.Request	req = null;
	public void		setReq(RQ.Request req)		{ this.req = req; assert (req.task == this); }
	public RQ.Request	getReq()					{ return req; }
	public LOPProducer	getGUI()					{ return getReq().getGUI(); }

	public	E			call() throws Exception		{ return null; }	
	public void		finished() throws Exception	{}	
}
