package org.aksw.saim.transfer.config;

/**
 * Measures similarity between classes.
 * 
 * @author Jens Lehmann
 * @author Axel Ngonga
 */
public interface ConfigAccuracy {

	public double getAccuracy(Configuration configuration, String posExamplesFile, String negExamplesFile);
	
}
