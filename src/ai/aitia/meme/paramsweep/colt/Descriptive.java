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
package ai.aitia.meme.paramsweep.colt;

import cern.colt.list.DoubleArrayList;

/** Extension of the statistics of the cern.jet.stat.Descriptive class. */
public class Descriptive {

	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Returns the sum of inversions of a data sequence, which is Sum( 1.0 / data[i]).
	 * @param data the data sequence
	 */
	public static double sumOfInversions(final DoubleArrayList data) {
		return cern.jet.stat.Descriptive.sumOfInversions(data,0,data.size()-1);
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the sum of logarithms of a data sequence, which is Sum( Log(data[i]).
	 * @param data the data sequence
	 */
	public static double sumOfLogarithms(final DoubleArrayList data) {
		return cern.jet.stat.Descriptive.sumOfLogarithms(data,0,data.size()-1);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double autoCorrelation(final DoubleArrayList data, final int lag) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
		return cern.jet.stat.Descriptive.autoCorrelation(data,lag,mean,variance);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double correlation(final DoubleArrayList data1, final DoubleArrayList data2) {
		final double sum1 = cern.jet.stat.Descriptive.sum(data1);
		final double sumOfSquares1 = cern.jet.stat.Descriptive.sumOfSquares(data1);
		final double variance1 = cern.jet.stat.Descriptive.variance(data1.size(),sum1,sumOfSquares1);
		final double standardDeviation1 = cern.jet.stat.Descriptive.standardDeviation(variance1);
		
		final double sum2 = cern.jet.stat.Descriptive.sum(data2);
		final double sumOfSquares2 = cern.jet.stat.Descriptive.sumOfSquares(data2);
		final double variance2 = cern.jet.stat.Descriptive.variance(data2.size(),sum2,sumOfSquares2);
		final double standardDeviation2 = cern.jet.stat.Descriptive.standardDeviation(variance2);
		return cern.jet.stat.Descriptive.correlation(data1,standardDeviation1,data2,standardDeviation2);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double harmonicMean(final DoubleArrayList data) {
		final double sumOfInversions = sumOfInversions(data);
		return cern.jet.stat.Descriptive.harmonicMean(data.size(),sumOfInversions);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double kurtosis(final DoubleArrayList data) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
		final double standardDeviation = cern.jet.stat.Descriptive.standardDeviation(variance);
		return cern.jet.stat.Descriptive.kurtosis(data,mean,standardDeviation);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double meanDeviation(final DoubleArrayList data) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		return cern.jet.stat.Descriptive.meanDeviation(data,mean);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double pooledMean(final DoubleArrayList data1, final DoubleArrayList data2) {
		final double mean1 = cern.jet.stat.Descriptive.mean(data1);
		final double mean2 = cern.jet.stat.Descriptive.mean(data2);
		return cern.jet.stat.Descriptive.pooledMean(data1.size(),mean1,data2.size(),mean2);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double pooledVariance(final DoubleArrayList data1, final DoubleArrayList data2) {
		final double sum1 = cern.jet.stat.Descriptive.sum(data1);
		final double sumOfSquares1 = cern.jet.stat.Descriptive.sumOfSquares(data1);
		final double variance1 = cern.jet.stat.Descriptive.variance(data1.size(),sum1,sumOfSquares1);
		
		final double sum2 = cern.jet.stat.Descriptive.sum(data2);
		final double sumOfSquares2 = cern.jet.stat.Descriptive.sumOfSquares(data2);
		final double variance2 = cern.jet.stat.Descriptive.variance(data2.size(),sum2,sumOfSquares2);
		return cern.jet.stat.Descriptive.pooledVariance(data1.size(),variance1,data2.size(),variance2);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double rms(final DoubleArrayList data) {
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		return cern.jet.stat.Descriptive.rms(data.size(),sumOfSquares);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double skew(final DoubleArrayList data) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
		final double standardDeviation = cern.jet.stat.Descriptive.standardDeviation(variance);
		return cern.jet.stat.Descriptive.skew(data,mean,standardDeviation);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double standardDeviation(final DoubleArrayList data) {
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.sampleVariance(data.size(),sum,sumOfSquares);
		return cern.jet.stat.Descriptive.standardDeviation(variance);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double standardError(final DoubleArrayList data) {
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
		return cern.jet.stat.Descriptive.standardError(data.size(),variance);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double kurtosisStandardError(final DoubleArrayList data) {
		return cern.jet.stat.Descriptive.sampleKurtosisStandardError(data.size());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double skewStandardError(final DoubleArrayList data) {
		return cern.jet.stat.Descriptive.sampleSkewStandardError(data.size());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double sumOfSquaredDeviations(final DoubleArrayList data) {
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		final double variance = cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
		return cern.jet.stat.Descriptive.sumOfSquaredDeviations(data.size(),variance);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double trimmedMean(final DoubleArrayList data, final int left, final int right) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		return cern.jet.stat.Descriptive.trimmedMean(data,mean,left,right);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double variance(final DoubleArrayList data) {
		final double sum = cern.jet.stat.Descriptive.sum(data);
		final double sumOfSquares = cern.jet.stat.Descriptive.sumOfSquares(data);
		return cern.jet.stat.Descriptive.variance(data.size(),sum,sumOfSquares);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double weightedRMS(final DoubleArrayList data, final DoubleArrayList weights) {
		double sumOfProducts = 0;
		double sumOfSquaredProducts = 0;
		for (int i = 0;i < data.size();++i) {
			final double d = data.getQuick(i);
			final double w = weights.get(i);
			sumOfProducts += d * w; 
			sumOfSquaredProducts += d * d * w;
		}
		return cern.jet.stat.Descriptive.weightedRMS(sumOfProducts,sumOfSquaredProducts);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static double winsorizedMean(final DoubleArrayList data, final int left, final int right) {
		final double mean = cern.jet.stat.Descriptive.mean(data);
		return cern.jet.stat.Descriptive.winsorizedMean(data,mean,left,right);
	}

	//-------------------------------------------------------------------------------------
	/** Returns the size of a sequence.
	 * @param data the sequence
	 */
	@Deprecated
	public static int size(ObjectList data) { return data.size(); }
	
	//-------------------------------------------------------------------------------------
	/** Returns the <code>value <i>mod</i> modulus</code>. */ 
	public static int mod(final int value, final int modulus) { return value % modulus; }
	
	//----------------------------------------------------------------------------------------------------
	public static double sum2(final double a, final double b) { return a + b; }
	
	//----------------------------------------------------------------------------------------------------
	public static double subtraction2(final double a, final double b) { return a - b; }
	
	//----------------------------------------------------------------------------------------------------
	public static double multiplication2(final double a, final double b) { return a * b; }
	
	//----------------------------------------------------------------------------------------------------
	public static double division2(final double a, final double b) { return a / b; }
}
