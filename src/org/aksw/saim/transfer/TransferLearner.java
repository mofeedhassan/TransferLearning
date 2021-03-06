package org.aksw.saim.transfer;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.evaluation.PRFComputer;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import org.aksw.saim.replacement.Replacer;

import org.aksw.saim.transfer.classes.ClassSimilarity;
import org.aksw.saim.transfer.classes.SamplingBasedClassSimilarity;
import org.aksw.saim.transfer.classes.UriBasedClassSimilarity;
import org.aksw.saim.transfer.config.ConfigAccuracy;
import org.aksw.saim.transfer.config.ConfigAccuracyWald95;
import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.transfer.properties.UriBasedPropertySimilarity;
import org.aksw.saim.transfer.properties.PropertySimilarity;
import org.aksw.saim.transfer.properties.SamplingBasedPropertySimilarity;
import org.aksw.saim.util.Execution;
import org.aksw.saim.util.SparqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The transfer learning algorithm core.
 *
 * @author Jens Lehmann
 * @author Axel Ngonga
 *
 */
public class TransferLearner {

    public static double COVERAGE = 0.6;
    public static int SAMPLESIZE = 300;
    private Set<Configuration> configurations;
    private Map<Configuration, String> posExamples;
    private Map<Configuration, String> negExamples;
    private Map<Configuration, Set<String>> sourceProperties;
    private Map<Configuration, Set<String>> targetProperties;
    private Map<String, Set<String>> sourcePropertyCache;
    private Map<String, Set<String>> targetPropertyCache;
    private String resultBuffer;
    Logger logger = LoggerFactory.getLogger(TransferLearner.class);

    
    public static void main(String[] args) 
    {
        /*TransferLearner tl = new TransferLearner("finalSpecs");
        System.out.println(tl.runSimpleMatching(100, 0.6));*/
        TransferLearner tl = new TransferLearner("tesSpecs"/*"limesSpecsAll"*/, true);
        Map<Configuration, List<Configuration>> result = tl.runOrderedMatching();
        for (Configuration c : result.keySet()) {
            System.out.print(c);
            List<Configuration> list = result.get(c);
            for (Configuration l : list) {
                System.out.print("\t" + l);
            }
            System.out.println();
        }
    }
    
    
    
    /**
     * Constructor to assign configurations, positive examples, negative example
     * @param configurations
     * @param posExamples
     * @param negExamples
     */
    
    public TransferLearner(Set<Configuration> configurations,
            Map<Configuration, String> posExamples,
            Map<Configuration, String> negExamples) {
        this.configurations = configurations;
        this.posExamples = posExamples;
        this.negExamples = negExamples;
    }

    /**
     * read the configuration items where properties required are cached or all the properties  related to the 
     * instances of source or target queried from the endpoint
     * @param configFolder
     */
    public TransferLearner(String configFolder) {
        resultBuffer = "";
        System.out.println(new File(configFolder).getAbsolutePath());
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        String[] files = f.list();
        ConfigReader cr = new ConfigReader();
        Configuration config;
        int count = 0;

        //check for writeCache
        File cache = new File(configFolder + "/propertyCache.tsv");//cached properties for files either source or destination
        if (cache.exists()) {
            System.out.println("Found cached data");
            readCache(configFolder);
        }
        for (String file : files) {
            folderName = new File(configFolder + "/" + file);
            if (folderName.isDirectory()) {
                if (!new File(folderName.getAbsoluteFile() + "/fixme.txt").exists()) {
                    config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");
                    count++;
                    System.out.println("Processing " + count + ".\t" + config.getName());
                    //get classes of source and target
                    String sourceClass = config.source.getClassOfendpoint(true);
                    String targetClass = config.target.getClassOfendpoint(true);
                    Set<String> relevantSourceProperties = new HashSet<>();
                    Set<String> relevantTargetProperties = new HashSet<>();
                    
                    //get properties of the source and target either cached or query them from endpoint
                    //check whether data already cached
                    if (sourcePropertyCache.containsKey(config.name)) {
                        relevantSourceProperties = sourcePropertyCache.get(config.name);
                    } else {
                        relevantSourceProperties = SparqlUtils.getRelevantProperties(config.getSource().endpoint, sourceClass, SAMPLESIZE, COVERAGE);
                        if (relevantSourceProperties.size() > 0) {
                            sourcePropertyCache.put(config.name, relevantSourceProperties);
                        }
                    }
                    //same here
                    if (targetPropertyCache.containsKey(config.name)) {
                        relevantTargetProperties = targetPropertyCache.get(config.name);
                    } else {
                        relevantTargetProperties = SparqlUtils.getRelevantProperties(config.getTarget().endpoint, targetClass, SAMPLESIZE, COVERAGE);
                        if (relevantTargetProperties.size() > 0) {
                            targetPropertyCache.put(config.name, relevantTargetProperties);
                        }
                    }
                    //set the configuraion items
                    if (!relevantSourceProperties.isEmpty() && !relevantTargetProperties.isEmpty()) {
                        configurations.add(config);
                        sourceProperties.put(config, relevantSourceProperties);
                        targetProperties.put(config, relevantTargetProperties);
                        posExamples.put(config, folderName.getAbsolutePath() + "/positive.nt");
                        posExamples.put(config, folderName.getAbsolutePath() + "/negative.nt");
                    }
                }
            }
        }
        writeCache(configFolder);//write the read configuration related properties for source and target into property cachefile
    }

    //same as previous constructor but it focuses on properties in configuration only
    public TransferLearner(String configFolder, boolean useSparql) {
        resultBuffer = "";
        configurations = new HashSet<>();
        posExamples = new HashMap<>();
        negExamples = new HashMap<>();
        sourceProperties = new HashMap<>();
        targetProperties = new HashMap<>();
        negExamples = new HashMap<>();
        sourcePropertyCache = new HashMap<>();
        targetPropertyCache = new HashMap<>();

        File f = new File(configFolder), folderName;
        String[] files = f.list();
        ConfigReader cr = new ConfigReader();
        Configuration config;
        int count = 0;

        //check for writeCache        
        for (String file : files) {
            folderName = new File(configFolder + "/" + file);
            if (folderName.isDirectory()) {
                if (!new File(folderName.getAbsoluteFile() + "/fixme.txt").exists()) {
                	//read limes configuration file
                    config = cr.readLimesConfig(folderName.getAbsolutePath() + "/spec.xml");
                    count++;
                    System.out.println("Processing " + count + ".\t" + config.getName());
                    //get the source and target classes
                    String sourceClass = config.source.getClassOfendpoint(true);
                    String targetClass = config.target.getClassOfendpoint(true);
                    Set<String> relevantSourceProperties = new HashSet<>();
                    Set<String> relevantTargetProperties = new HashSet<>();

                    //gets the set of Properties inside the configuration either for source or target
                    //check whether data already cached
                    relevantSourceProperties = getRelevantProperties(config, true);
                    relevantTargetProperties = getRelevantProperties(config, false);

                    if (!relevantSourceProperties.isEmpty() && !relevantTargetProperties.isEmpty()) {
                        configurations.add(config);
                        sourceProperties.put(config, relevantSourceProperties);
                        targetProperties.put(config, relevantTargetProperties);
                        posExamples.put(config, folderName.getAbsolutePath() + "/positive.nt");
                        negExamples.put(config, folderName.getAbsolutePath() + "/negative.nt");
                    }
                }
            }
        }
    }
    
    //write the read configuration related properties for source and target into property cachefile
    public void writeCache(String folder) {
        try {
            new File(folder + "/propertyCache.tsv").delete();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(folder + "/propertyCache.tsv")));
            for (Configuration c : configurations) {
                Set<String> sourceProps = sourceProperties.get(c);
                for (String property : sourceProps) {
                    writer.println(c.name + "\tSourceProperty\t" + property);
                }
                Set<String> targetProps = targetProperties.get(c);

                for (String property : targetProps) {
                    writer.println(c.name + "\tTargetProperty\t" + property);
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //read the cached properties in propertyCache file as part of configuration reading
    public void readCache(String folder) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(folder + "/propertyCache.tsv"));
            String s = reader.readLine();
            sourcePropertyCache = new HashMap<>();
            targetPropertyCache = new HashMap<>();
            while (s != null) {
                String[] split = s.split("\t");
                if (split[1].equals("SourceProperty")) {
                    if (!sourcePropertyCache.containsKey(split[0])) {
                        sourcePropertyCache.put(split[0], new HashSet<String>());
                    }
                    sourcePropertyCache.get(split[0]).add(split[2]);
                } else {
                    if (!targetPropertyCache.containsKey(split[0])) {
                        targetPropertyCache.put(split[0], new HashSet<String>());
                    }
                    targetPropertyCache.get(split[0]).add(split[2]);
                }
                s = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the whole transfer learning experiment for all specs using the URI
     * matching approach
     *
     * @param instanceSampleSize Sampling size
     * @param coverage Minimal converage for
     * @return
     */
    public Map<Configuration, List<Configuration>> runOrderedMatching() {
        Map<Configuration, List<Configuration>> results = new HashMap<>();
        List<Configuration> result;
        for (Configuration config : configurations) {
        	//Returns the class contained in the restriction
            List<String> sourceClasses = getRestrictionList(config.source.restrictions);
            List<String> targetClasses = getRestrictionList(config.target.restrictions);
            //get properties related to this configuration
            Set<String> relevantSourceProperties = sourceProperties.get(config);
            Set<String> relevantTargetProperties = targetProperties.get(config);
            
            result = runLeaveOneOutOrdered(config, sourceClasses, targetClasses, relevantSourceProperties, relevantTargetProperties);
            results.put(config, result);//store config:list of config
        }
        return results;
    }

    /**
     * Runs the whole transfer learning experiment for all specs using the URI
     * matching approach
     *
     * @param instanceSampleSize Sampling size
     * @param coverage Minimal converage for
     * @return
     */
    public Map<Configuration, Double> runSimpleMatching(int instanceSampleSize, double coverage) {
        Map<Configuration, Double> results = new HashMap<Configuration, Double>();
        Configuration result;
        String output = "Specification \t Precision \t Recall \t F-Measure\n";
        PRFComputer prf = new PRFComputer();
        Execution exec = new Execution();
        for (Configuration config : configurations) {
            //System.out.println("Processing " + config.name + ":" + config.source.getClassOfendpoint(true) + " -> " + config.target.getClassOfendpoint(true));
            String sourceClass = config.source.getClassOfendpoint(true);
            String targetClass = config.target.getClassOfendpoint(true);

            System.out.println("Getting source properties ... ");
            Set<String> relevantSourceProperties = sourceProperties.get(config);
            System.out.println("Got " + relevantSourceProperties.size() + " source properties.\nGetting target properties ... ");
            Set<String> relevantTargetProperties = targetProperties.get(config);
            System.out.println("Got " + relevantTargetProperties.size() + " target properties.\n");
            System.out.println("Running leave one out ... ");
            // gets the best spec
            result = runLeaveOneOut(config, sourceClass, targetClass, relevantSourceProperties, relevantTargetProperties);
            // now run best spec and get precision, recall and f-measure
            System.out.println("Getting reference mapping ...");
            Mapping reference = new Mapping().readNtFile(config.name.replaceAll(Pattern.quote("spec.xml"), "accept.nt"));
            System.out.println("Running new spec derived from " + result.name + " ...");
            Mapping transferResult = exec.executeComplex(result);//get mapping based on the best spec

//            System.out.println("Transfermapping\n");
//            System.out.println(transferResult);
//            System.out.println("Reference\n");
//            System.out.println(reference);
//            
            //measure the performance and accuracy against the reference mapping
            output = output + new File(config.name).getParent() + "\t" + prf.computePrecision(transferResult, reference) + "\t" + prf.computeRecall(transferResult, reference) + "\t" + prf.computeFScore(transferResult, reference) + "\n";
            ConfigAccuracy ca = new ConfigAccuracyWald95();
            double acc = ca.getAccuracy(result, posExamples.get(config), negExamples.get(config));
            System.out.println(output);
            results.put(result, acc);//store config:accuracy
            //System.exit(1);
        }
        System.out.println("\n\n=== FINAL RESULTS ===\n" + output + "\n\n");
        System.out.println("\n\n" + resultBuffer + "\n\n");
        return results;
    }
//    /**
//     * Gets an input source and target class and return the best possible config
//     *
//     * @param inputSourceClass Input source class
//     * @param inputTargetClass Input target class
//     * @param relevantSourceProperties Source properties
//     * @param relevantTargetProperties Target properties
//     */
//    public Configuration run(String inputSourceClass, String inputTargetClass, Set<String> relevantSourceProperties, Set<String> relevantTargetProperties) {
//
//        // setup
//        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
//        ClassSimilarity classSim = new UriBasedClassSimilarity();
//        PropertySimilarity propSim = new UriBasedPropertySimilarity();
//        double bestF = 0;
//        Configuration bestConfig = configurations.iterator().next();
//        for (Configuration configuration : configurations) {
//            // accuracy of link specification; // TODO: where to get the positive and negative examples?
//            double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));
//
//            String sourceClass = configuration.getSource().getClassOfendpoint();
//            String targetClass = configuration.getTarget().getClassOfendpoint();
//
//            double cSSim = classSim.getSimilarity(sourceClass, inputSourceClass, configuration);
//            double cTSim = classSim.getSimilarity(targetClass, inputTargetClass, configuration);
//
//            double factor = alpha * cSSim * cTSim;
//            if (bestF < factor) {
//                bestF = factor;
//                bestConfig = configuration;
//            }
//
//        }
//        // figure out best mapping properties
//        //first for source
//
//        String sourceClass = bestConfig.getSource().getClassOfendpoint();
//        String targetClass = bestConfig.getTarget().getClassOfendpoint();
//        Map<String, String> sourcePropertyMapping = getPropertyMap(relevantSourceProperties,
//                sourceClass, inputSourceClass, propSim, bestConfig, true);
//        Map<String, String> targetPropertyMapping = getPropertyMap(relevantTargetProperties,
//                targetClass, inputTargetClass, propSim, bestConfig, true);
//
//        //important: clone the config
//        bestConfig = new ConfigReader().readLimesConfig(bestConfig.name);
//        for (String property : sourcePropertyMapping.keySet()) {
//            bestConfig = Replacer.replace(bestConfig, property, sourcePropertyMapping.get(property), true);
//        }
//        for (String property : targetPropertyMapping.keySet()) {
//            bestConfig = Replacer.replace(bestConfig, property, targetPropertyMapping.get(property), false);
//        }
//
//        System.out.println(bestConfig.source.getClassOfendpoint());
//        System.out.println(bestConfig.target.getClassOfendpoint());
//        System.out.println(bestConfig.measure);
//        //System.out.println(bestConfig.source.getClassOfendpoint());
//
//        return bestConfig;
//    }

    /**
     * Gets a configuration with its input source and target class and return the best possible configuration similar
     * to it with replacing all the best config.'s data with the given configuration data except the metric in the best 
     * configuration 
     *
     * @param inputSourceClass Input source class
     * @param inputTargetClass Input target class
     * @param relevantSourceProperties Source properties
     * @param relevantTargetProperties Target properties
     */
    public Configuration runLeaveOneOut(Configuration config, String inputSourceClass, String inputTargetClass, Set<String> relevantSourceProperties, Set<String> relevantTargetProperties) {

        // setup
        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
        ClassSimilarity classSim = new SamplingBasedClassSimilarity();//get similarities based on Class
        PropertySimilarity propSim = new SamplingBasedPropertySimilarity();
        double bestF = 0;
        Configuration bestConfig = configurations.iterator().next();
        double similarity = -1;
        //get the best configuration in accuracy that is similar in terms of classes to the given config
        for (Configuration configuration : configurations) {
            String name1 = configuration.name.trim();
            String name2 = config.name.trim();
            if (!name1.equalsIgnoreCase(name2)) {//not to b compared to itself
                System.out.println("Processing " + configuration.name);
                // accuracy of link specification's positive and negative examples; 
                // TODO: where to get the positive and negative examples?
                double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));

                String sourceClass = configuration.getSource().getClassOfendpoint();
                String targetClass = configuration.getTarget().getClassOfendpoint();
                if (sourceClass == null) {
                    System.err.println(configuration.name + " leads to sourceClass == null");
                }
                if (targetClass == null) {
                    System.err.println(configuration.name + " leads to targetClass == null");
                }
                //cacl. class similarities beteen the config and each configuration
                double cSSim = classSim.getSimilarity(sourceClass, inputSourceClass, configuration);
                double cTSim = classSim.getSimilarity(targetClass, inputTargetClass, configuration);

                double factor = alpha * cSSim * cTSim;
                //double factor = cSSim * cTSim;
                if (bestF < factor) {
                    bestF = factor;
                    bestConfig = configuration;
                }
            }
        }
        System.out.println("Mapping " + config.name + " and " + bestConfig.name + " is " + bestF);
        //clone the best config
        bestConfig = new ConfigReader().readLimesConfig(bestConfig.name);

        // figure out best mapping properties
        //first for source        
        String sourceClass = bestConfig.getSource().getClassOfendpoint();
        String targetClass = bestConfig.getTarget().getClassOfendpoint();
        //get the best related properties from the bestConfig to the properties of config in terms of similarity
        Map<String, String> sourcePropertyMapping = getPropertyMap(relevantSourceProperties,
                sourceClass, inputSourceClass, propSim, bestConfig, true);
        Map<String, String> targetPropertyMapping = getPropertyMap(relevantTargetProperties,
                targetClass, inputTargetClass, propSim, bestConfig, false);

        //replace endpoints
        bestConfig.source.endpoint = config.source.endpoint;
        bestConfig.target.endpoint = config.target.endpoint;
        //replace graphs
        bestConfig.source.graph = config.source.graph;
        bestConfig.target.graph = config.target.graph;
        //fill in prefixes
        for (String entry : config.source.prefixes.keySet()) {
            bestConfig.source.prefixes.put(entry, config.source.prefixes.get(entry));
            bestConfig.target.prefixes.put(entry, config.source.prefixes.get(entry));
        }
        for (String entry : config.target.prefixes.keySet()) {
            bestConfig.source.prefixes.put(entry, config.target.prefixes.get(entry));
            bestConfig.target.prefixes.put(entry, config.target.prefixes.get(entry));
        }
        //replace properties
        // this replacement based on replacing the best config properties with the most similar property of config, that are
        //got before from method getPropertyMap()
        for (String property : sourcePropertyMapping.keySet()) {

            bestConfig = Replacer.replace(bestConfig, property, config, sourcePropertyMapping.get(property), true);
        }
        for (String property : targetPropertyMapping.keySet()) {
            bestConfig = Replacer.replace(bestConfig, property, config, targetPropertyMapping.get(property), false);
        }


        //replace class
        String r = bestConfig.source.restrictions.get(0);
        r = r.replaceAll(Pattern.quote(bestConfig.source.getClassOfendpoint()), "<" + inputSourceClass + ">");
        bestConfig.source.restrictions.set(0, r);

        r = bestConfig.target.restrictions.get(0);
        r = r.replaceAll(Pattern.quote(bestConfig.target.getClassOfendpoint()), "<" + inputTargetClass + ">");
        bestConfig.target.restrictions.set(0, r);
        resultBuffer = resultBuffer + config.name + "\t" + bestConfig.name + "\t"
                + bestF + "\t" + bestConfig.source.getClassOfendpoint()
                + "\t" + bestConfig.target.getClassOfendpoint() + "\t" + bestConfig.measure + "\n";

        return bestConfig;//return the bestConfig after replacing all its items with the given config. except its metric which will b
                          //used with it
    }

    /**
     * for each property in the list of properties of the given config, get the best related property (most similar) from the given
     * configuration (bestConfig)
     * @param inputProperties Properties from class whose mapping is to be
     * learned (config)
     * @param className Class name whose mapping is to be learned
     * @param inputClassName Known class from config
     * @param propSim Property similarity computation algorithm
     * @param configuration Known configuration (best configuration)
     * @return Mapping from properties of known config to config to be learned
     */
    private Map<String, String> getPropertyMap(Set<String> inputProperties, String className, String inputClassName, PropertySimilarity propSim, Configuration configuration, boolean source) {
        Map<String, String> propertyMapping = new HashMap<>();
        List<String> properties;
        if (source) {
            properties = configuration.source.properties;
        } else {
            properties = configuration.target.properties;
        }
        for (String knownProperty : properties) {
            double maxSim = -1, sim;
            String bestProperty = "";
            for (String inputProperty : inputProperties) // ... call replacement function using propSim, relevantSourceProperties, configuration as input ...
            {
                // get rid of rdf:type
                if (!inputProperty.endsWith("ype") && !inputProperty.endsWith("sameAs")) {
                    sim = propSim.getSimilarity(knownProperty, inputProperty, className, inputClassName, configuration);
                    if (sim > maxSim) {
                        bestProperty = inputProperty;
                        maxSim = sim;
                    }
                }
            }
            propertyMapping.put(knownProperty, bestProperty); 
        }
        return propertyMapping;
    }

    /**
     * An example transfer learning task.
     *
     * @param args
     */
    public void test() {
        // input (example from https://github.com/LATC/24-7-platform/blob/master/link-specifications/dbpedia-diseasome-disease/spec.xml)
//		String inputSourceClass = "http://dbpedia.org/ontology/Disease";
//		String inputTargetClass = "http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases";

        // LIMES example: we learn inactive ingredients from other similar specs
        String inputSourceClass = "http://www4.wiwiss.fu-berlin.de/dailymed/resource/dailymed/drugs";
        String sourceEndpoint = "http://lgd.aksw.org:5678/sparql";
        String inputTargetClass = "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs";
        String targetEndpoint = "http://lgd.aksw.org:5678/sparql";
        Set<Configuration> configurations = new HashSet<Configuration>();

        Map<Configuration, String> posExamples = new HashMap<Configuration, String>();
        Map<Configuration, String> negExamples = new HashMap<Configuration, String>();

        boolean limesOnly = true;

        if (limesOnly) {
            ConfigReader cr = new ConfigReader();
            Configuration cf;
            cf = cr.readLimesConfig("specs/drugbank-dailymed-activeIngredients/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-activeIngredients/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-activeIngredients/negative.ttl");
            cf = cr.readLimesConfig("specs/drugbank-dailymed-activeMoiety/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-activeMoiety/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-activeMoiety/negative.ttl");
            cf = cr.readLimesConfig("specs/drugbank-dailymed-ingredients/spec.limes.xml");
            configurations.add(cf);
            posExamples.put(cf, "specs/drugbank-dailymed-ingredients/positive.ttl");
            negExamples.put(cf, "specs/drugbank-dailymed-ingredients/negative.ttl");
            // we learn drugbank-dailymed-inactiveIngredients from 
            // drugbank-dailymed-activeIngredients
            // drugbank-dailymed-activeMoiety
            // drugbank-dailymed-ingredients


        } else {
            // TODO: read configurations from LATC specs	
        }

        // determine relevant properties for current link Task with min cov 0.8
        Set<String> relevantSourceProperties = SparqlUtils.getRelevantProperties(sourceEndpoint, inputSourceClass, 100, 0.8);
        Set<String> relevantTargetProperties = SparqlUtils.getRelevantProperties(targetEndpoint, inputTargetClass, 100, 0.8);;

        TransferLearner tl = new TransferLearner(configurations, posExamples, negExamples);
        //tl.run(inputSourceClass, inputTargetClass, relevantSourceProperties, relevantTargetProperties);

    }
    /**
     * gets the set of Properties inside the configuration either for source or target
     * @param c
     * @param source
     * @return
     */
    public Set<String> getRelevantProperties(Configuration c, boolean source) {
        Set<String> properties = new HashSet<>();
        if (source) {
            for (String p : c.source.properties) {
                properties.add(p);
            }
        } else {
            for (String p : c.target.properties) {
                properties.add(p);
            }
        }
        return properties;
    }

    /**
     * returns list of similar configurations in terms of class similarity sorted by similarit degree descending
     * good for n most similar configuration
     * @param config
     * @param inputSourceClasses
     * @param inputTargetClasses
     * @param relevantSourceProperties
     * @param relevantTargetProperties
     * @return
     */
    private List<Configuration> runLeaveOneOutOrdered(Configuration config,
            List<String> inputSourceClasses, List<String> inputTargetClasses,
            Set<String> relevantSourceProperties, Set<String> relevantTargetProperties) {

        // setup
        ConfigAccuracy confAcc = new ConfigAccuracyWald95();
        ClassSimilarity classSim = new SamplingBasedClassSimilarity();
        PropertySimilarity propSim = new SamplingBasedPropertySimilarity();
        List<Configuration> result = new ArrayList<Configuration>();

        for (Configuration configuration : configurations) {
       	
        	/////////////////////
            String name1 = configuration.name.trim();
            String name2 = config.name.trim();
            if (!name1.equalsIgnoreCase(name2)) {
                //System.out.println("Processing " + configuration.name);
                // accuracy of link specification; // TODO: where to get the positive and negative examples?
                double alpha = confAcc.getAccuracy(configuration, posExamples.get(configuration), negExamples.get(configuration));

                List<String> sourceClasses = getRestrictionList(configuration.getSource().restrictions);
                List<String> targetClasses = getRestrictionList(configuration.getTarget().restrictions);
                if (sourceClasses.isEmpty()) {
                    System.err.println(configuration.name + " leads to sourceClass == null");
                }
                if (targetClasses.isEmpty()) {
                    System.err.println(configuration.name + " leads to targetClass == null");
                }

                
                double cSSim = 0, cTSim = 0;
                for (String sourceClass : sourceClasses) {
                    double max = -1, sim;
                    for (String inputSourceClass : inputSourceClasses) {
                        sim = classSim.getSimilarity(sourceClass, inputSourceClass, configuration);
                        if (sim > max) {
                            max = sim;
                        }
                    }
                    cSSim = cSSim + max;
                }
                cSSim = cSSim / (double) sourceClasses.size();

                for (String targetClass : targetClasses) {
                    double max = -1, sim;
                    for (String inputTargetClass : inputTargetClasses) {
                        sim = classSim.getSimilarity(targetClass, inputTargetClass, configuration);
                        if (sim > max) {
                            max = sim;
                        }
                    }
                    cTSim = cTSim + max;
                }
                cTSim = cTSim / (double) targetClasses.size();

                double factor = alpha * cSSim * cTSim;
                //double factor = cSSim * cTSim;
                Configuration c = new ConfigReader().readLimesConfig(configuration.name);
                c.similarity = factor;
                result.add(c);
            }
        }
        Collections.sort(result);
        result = reverse(result);
        return result;
    }

    public List reverse(List x) {
        List result = new ArrayList();
        for (int i = x.size() - 1; i >= 0; i--) {
            result.add(x.get(i));
        }
        return result;
    }

    /**
     * Returns the class contained in the restriction
     *
     * @return Class label
     */
    public List<String> getRestrictionList(List<String> restrictions) {
        List<String> result = new ArrayList<String>();
        List<String> buffer = new ArrayList<String>();
        for (String rest : restrictions) {
            if (rest.contains("UNION")) {
                String split[] = rest.split("UNION");
                for (int i = 0; i < split.length; i++) {
                    String s = split[i];
                    s = s.replaceAll("<", "").replaceAll(">", "").replaceAll(Pattern.quote("."), "");
                    s = s.replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("}"), "");
                    s = s.split(" ")[2];
                    //s = s.substring(0, s.length() - 1);
                    result.add(s.trim());
                }
            } else {
                String s = rest;
                s = s.replaceAll("<", "").replaceAll(">", "").replaceAll(Pattern.quote("."), "");
                s = s.replaceAll(Pattern.quote("{"), "").replaceAll(Pattern.quote("}"), "");
                s = s.split(" ")[2];
                //s = s.substring(0, s.length() - 1);
                result.add(s.trim());

            }
        }
        return result;
    }

    //can the set of similar specs provide a set of atomic measures to work on and provide an advanced complex of metric
}
