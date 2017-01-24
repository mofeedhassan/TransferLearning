package org.aksw.saim.transfer.classes;

import org.aksw.saim.transfer.config.Configuration;

/**
 * Measures similarity between classes.
 * 
 * @author Jens Lehmann
 * @author Axel Ngonga
 */
public interface ClassSimilarity {

	public double getSimilarity(String class1, String class2, Configuration config);
	
}
