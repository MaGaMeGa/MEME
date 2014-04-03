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
package ai.aitia.meme.paramsweep.gui.info;

public class MasonIntervalParameterInfo extends ParameterInfo {
	
	private static final long serialVersionUID = -3988441684721097877L;
	protected double minD;
	protected double maxD;
	protected long minL;
	protected long maxL;
	protected Number intervalMin;
	protected Number intervalMax;
	protected boolean isDoubleInterval = false;

	public MasonIntervalParameterInfo(String name, String type, Class<?> javaType, Number min, Number max, boolean isDoubleInterval) {
		this(name, null, type, javaType, min, max, isDoubleInterval);
	}
	
	public MasonIntervalParameterInfo(String name, String description, String type, Class<?> javaType, Number min, Number max, boolean isDoubleInterval) {
		super(name, description, type, javaType);
		this.intervalMin = min;
		this.intervalMax = max;
		this.isDoubleInterval = isDoubleInterval;
		if (isDoubleInterval) {
			minD = min.doubleValue();
			maxD = max.doubleValue();
		} else {
			minL = min.longValue();
			maxL = max.longValue();
		}
	}
	
	public boolean isValidValue(String value) {
		if (isDoubleInterval) {
			double num = Double.parseDouble(value);
			if (num>=minD && num<=maxD) return true;
			else return false;
		} else {
			long num = Long.parseLong(value);
			if (num>=minL && num<=maxL) return true;
			else return false;
		}
	}

	public boolean isValidValues(String[] newValues) {
		for (int i = 0; i < newValues.length; i++) {
			if (!isValidValue(newValues[i])) return false;
		}
		return true;
	}

	public boolean isValidValues(String start, String end, String step) {
		double startNum = Double.parseDouble(start);
		double endNum = Double.parseDouble(end);
		if (startNum > endNum) {
			double swap = endNum;
			endNum = startNum;
			startNum = swap;
		}
		if (isDoubleInterval) {
			if (startNum>=minD && endNum<=maxD) return true;
			else return false;
		} else {
			if (startNum>=minL && endNum<=maxL) return true;
			else return false;
		}
	}
	
	protected MasonIntervalParameterInfo(MasonIntervalParameterInfo p) {
		super(p);
		this.isDoubleInterval = p.isDoubleInterval;
		if (isDoubleInterval) {
			minD = p.minD;
			maxD = p.maxD;
		} else {
			minL = p.minL;
			maxL = p.maxL;
		}
	}

	public Number getIntervalMin() {
		return intervalMin;
	}

	public void setIntervalMin(Number intervalMin) {
		this.intervalMin = intervalMin;
	}

	public Number getIntervalMax() {
		return intervalMax;
	}

	public void setIntervalMax(Number intervalMax) {
		this.intervalMax = intervalMax;
	}

}
