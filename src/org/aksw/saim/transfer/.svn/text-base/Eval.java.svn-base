package org.aksw.saim.transfer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.util.SparqlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Implements leave-one-out cross validation on the LATC linkspecs. 
 * 
 * @author Jens Lehmann
 * 
 *
 */
public class Eval {

	Logger logger = LoggerFactory.getLogger(Eval.class);
	
	private File specsDir = new File("specs/");
	private File usableSpecDirsFile = new File(specsDir, "usableSpecDirs.txt");
	private List<String> usableSpecs;
	private String limesSpecName = "spec.limes.xml";
	
	public Eval() {
		
	}
	
	public void run() throws IOException {

		// first check which specs we can actually use (if not done before)
		if(usableSpecDirsFile.exists()) {
			logger.info("Reading usable specs from " + usableSpecDirsFile + ".");
			usableSpecs = FileUtils.readLines(usableSpecDirsFile);
		} else {
			logger.info("Determining usable specs.");
			List<String> usableSpecDirs = getUsableSpecDirs();
			logger.info("Writing usable spec directory list to " + usableSpecDirsFile + ".");
			FileUtils.writeLines(usableSpecDirsFile, usableSpecDirs);	
		}
		
	}
	
	/**
	 * Loops through the spec dirs and checks whether they can be used in the evaluation.
	 * @return The usable spec dirs.
	 */
	public List<String> getUsableSpecDirs() {
		List<String> usableSpecDirs = new LinkedList<String>();
		// inefficiency: returns all directories, which is more than we need
		Collection<File> specDirs = FileUtils.listFilesAndDirs(specsDir, DirectoryFileFilter.INSTANCE, DirectoryFileFilter.INSTANCE);
		ConfigReader cr = new ConfigReader();
		for(File specDir : specDirs) {
			boolean usable = false;
			// check whether a LIMES spec exists
			File limesSpec = new File(specDir, limesSpecName);
			if(limesSpec.exists()) {
				// get endpoints from spec
				// TODO: we also need the graphs as using them is often more efficient and
				// we should check whether the graph in spec is correct
				Configuration cf = cr.readLimesConfig(limesSpec.getAbsolutePath());
				boolean sourceAlive = SparqlUtils.isAlive(cf.getSource().endpoint, null);
				boolean targetAlive = SparqlUtils.isAlive(cf.getTarget().endpoint, null);
				usable = sourceAlive && targetAlive;
			}
			if(usable) {
				logger.info("Spec " + specDir + " is usable.");
				usableSpecDirs.add(specDir.getName());
			} else {
				logger.info("Spec " + specDir + " is not usable.");
			}
		}
		return usableSpecDirs;
	}
	
	public static void main(String args[]) throws IOException {
		Eval eval = new Eval();
		eval.run();
	}
	
}
