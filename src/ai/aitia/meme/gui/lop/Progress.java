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

import java.util.LinkedList;

//-------------------------------------------------------------------------
/** This class represents the progress of long operations. It estimates the time to
 *  the end of the operation. */ 
public class Progress
{
	public static final long	DAY = 24*3600*1000L;
	private static int		RECENT_SPEEDS_SIZE = 4;

	public static final java.text.DateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm:ss");
	static {
		timeFmt.setTimeZone(new java.util.SimpleTimeZone(0, "noDST"));
	}

	double	current, total, lc, avgspeed;
	long	startTime, last, last2, avgleft;
	LinkedList<Double> recentSpeeds;
	String	left;
	/** Progress listener object. */
	volatile IProgressUpdate	listener = null;

	//---------------------------------------------------------------------
	public							Progress()				{ this(-1); }
	public							Progress(double total)	{ restart(total); }

	/** Returns the current position of progress. */
	public synchronized double	getCurrent()			{ return current; }
	/** Returns the length of progress. */
	public synchronized double	getEnd()				{ return total; }


	//---------------------------------------------------------------------
	/** Resets the progress object.
	 * @param total the length of the progress
	 */
	public synchronized void restart(double total) {
		this.total = total;
		startTime = last = last2 = System.currentTimeMillis();
		avgspeed = -1;
		avgleft = -1;
		recentSpeeds = null;
		lc = current = 0;
		left = "";
		if (listener != null)
			listener.onProgressUpdate(total <= 0 ? -1 : 0, 0l, left);
	}


	//---------------------------------------------------------------------
	/** Updates the progress object.
	 * @param curr the current position of the progress 
	 * @param total the length of the progress, -1 indicates that the length
	 *        is unknown  
	 */
	public synchronized void update(double curr, double tot) {
		boolean determinate_before = (total > 0 && current >= 0);

		if (tot >= 0) { if (curr == 0) restart(tot); else total = tot; }
		if (curr >= 0) current = curr; else curr = current;

		boolean determinate_after = (total > 0 && current >= 0);

		long now = System.currentTimeMillis();
		if (now - last2 < 501 && (determinate_before == determinate_after)) {
			return;
		}

		last2 = now;
		long elapsed = (now - startTime);

		if (total <= 0)
			curr = -1;
		else if (curr > 0)
			curr /= total;

		if (curr > 0 && total > 0 && now - last > 500) {
			if (curr != lc) {
				double speed = (curr - lc) / (now - last);
				lc   = curr;
				last = now;
				if (speed > 0) {
					if (avgspeed < 0) {
						avgspeed = speed;
					}
					else {
						double eps = 0.125;
						avgspeed = avgspeed * (1-eps) + speed * eps;
					}
				} else if (speed < 0) {
					avgspeed = -1;
				}
			}
			if (avgspeed <= 0) {
				curr = -1;		// force indeterminate display
				avgleft = -1;
				recentSpeeds = null;
				this.left = "";
			} else {
				if (recentSpeeds == null) {
					recentSpeeds = new LinkedList<Double>();
				} else while (recentSpeeds.size() >= RECENT_SPEEDS_SIZE) {
					recentSpeeds.removeFirst();
				}
				recentSpeeds.add(avgspeed);
				double leftmsec = (1 - curr)/ java.util.Collections.min(recentSpeeds);
				if (leftmsec < 366*DAY) {
					if (avgleft < 0)
						avgleft = Math.round(leftmsec);
					else {
						double eps = 0.5 + curr * 0.25; // 0.125 + curr * 0.375;
						avgleft = Math.round(avgleft * (1-eps) + leftmsec * eps);
					}
					if (avgleft < DAY)
						this.left = timeFmt.format(avgleft);
					else
						this.left = String.format("%d day(s)", Math.round(avgleft/(double)DAY));
				} else {
					curr = -1;
					avgleft = -1;
					this.left = "";
				}
			}
		}
		if (listener != null)
			listener.onProgressUpdate(curr * 100, elapsed, left); 
	}

	//---------------------------------------------------------------------
	/** Updates the progress. */
	public synchronized void update() {
		update(current, -1);
	}

	//---------------------------------------------------------------------
	public void setListener(IProgressUpdate observer) {
		listener = observer;
	}
}

