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
package ai.aitia.meme.processing.model;

public class VariableInfo {
	
	protected String name = null;
	protected int order = 1;
	
	protected double min = 0;
	protected double max = 0;
	protected double center = 0;
	protected double intervalStart = 0;
	protected double intervalEnd = 0;
	protected double intervalCenter = 0;
	
	public VariableInfo(String name, int order) {
		if (order < 1) throw new IllegalArgumentException("The order of the variable "+name+" must be positive");
		this.name = name;
		this.order = order;
	}
	
	@Override
	public String toString(){
		if (order == 1) return "["+name+"]";
		else return "["+name+"^"+order+"]";
	}
	
	public void increaseOrder(){
		order++;
	}

	public void decreaseOrder(){
		if(order > 1) order--;
	}

	public String getName() {
		return name;
	}
	
	public int getOrder(){
		return order;
	}

	public double getCenter() {
		return center;
	}

	public void setCenter(double center) {
		this.center = center;
	}

	public double getIntervalCenter() {
		return intervalCenter;
	}

	public void setIntervalCenter(double intervalCenter) {
		this.intervalCenter = intervalCenter;
	}

	public double getIntervalEnd() {
		return intervalEnd;
	}

	public void setIntervalEnd(double intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

	public double getIntervalStart() {
		return intervalStart;
	}

	public void setIntervalStart(double intervalStart) {
		this.intervalStart = intervalStart;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}
}
