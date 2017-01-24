/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.classes;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import org.aksw.saim.transfer.config.Configuration;
import org.aksw.saim.transfer.properties.SamplingBasedPropertySimilarity;
import org.aksw.saim.util.Execution;

/**
 *
 * @author ngonga
 */
public class SamplingBasedClassSimilarity implements ClassSimilarity {

    public int SAMPLING_RATE = 100;
    public double THRESHOLD = 0.25;

/*    private static void getSerlializedDataset(Cache cache, Configuration configuration)
    {
    	
    }*/
    /**
     * get a cache of instances of certain size from a given class containing all their object values
     * @param c
     * @param endpoint
     * @param size
     * @return
     */
    private static Cache getPropertyValues(String c, String endpoint, int size) {
        Cache cache = new HybridCache();
        
        
        String query;
        //get 'size' number of instances from the c class and all object values of these instances
        if(c.startsWith("http")) query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + c + ">. "
                + "?a ?p ?x .} LIMIT " + size;
        else query = "SELECT ?x ?a "
                + "WHERE { ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + c + ". "
                + "?a ?p ?x .} LIMIT " + size;
        Query sparqlQuery = QueryFactory.create(query);

        QueryExecution qexec;
        qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
        ResultSet results = qexec.execSelect();

        String x, a, b;
        QuerySolution soln;
        while (results.hasNext()) {
            soln = results.nextSolution();
            if (soln.get("x").isLiteral()) {
                cache.addTriple(soln.get("a").toString(), "p", ((Literal) soln.get("x")).getLexicalForm());
            }
        }
        return cache;// triples as: ?a "p" ?x
    }

    /**
     * return the average similarity value between certain number of mapping instances between
     * two datasets each from specific class
     */
    @Override
    public double getSimilarity(String class1, String class2, Configuration config) {
    	//expand the class into its fullname http:....(property or class)
        String c1 = SamplingBasedPropertySimilarity.expand(class1, config);
        String c2 = SamplingBasedPropertySimilarity.expand(class2, config);
        //get the cache instances of the given class focusing on subjects' and objects' values
        Cache source = getPropertyValues(c1, config.source.endpoint, SAMPLING_RATE);
        Cache target = getPropertyValues(c2, config.target.endpoint, SAMPLING_RATE);

        //System.out.println(source+"\n"+target+"\n"+m);
        double counter = 0.0;
        double total = 0.0;
        
        if(source.size() !=0 && target.size()!= 0) // the condition block is added by Mofeed to avoid mapping when source/target is empty
        {
        	Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
            
            //we could use max, min instead
            //could also
            for (String s : m.map.keySet()) {
                for (String t : m.map.get(s).keySet()) {
                    counter++;
                    total = total + m.getSimilarity(s, t);
                }
            }
        }
        
        if (counter == 0) {
            return counter;
        }
        return total/(Math.min((double)source.getAllUris().size(),(double)target.getAllUris().size()));//return average similarity value
    }
    
    /** More for testing than anything else
     * 
     * @param class1
     * @param class2
     * @param endpoint1
     * @param endpoint2
     * @return 
     */
    public double getSimilarity(String class1, String class2, String endpoint1, String endpoint2) {
        Cache source = getPropertyValues(class1, endpoint1, SAMPLING_RATE);
        Cache target = getPropertyValues(class2, endpoint2, SAMPLING_RATE);
        
        Mapping m = Execution.execute(source, target, "trigrams", THRESHOLD);
        System.out.println(source+"\n"+target+"\n"+m);
        double counter = 0.0;
        double total = 0.0;
        //we could use max, min instead
        //could also
        for (String s : m.map.keySet()) {
            for (String t : m.map.get(s).keySet()) {
                counter++;
                total = total + m.getSimilarity(s, t);
            }
        }
        if (counter == 0) {
            return counter;
        }
        return total/(Math.min((double)source.getAllUris().size(),(double)target.getAllUris().size()));
    }

    public static void test() {
        SamplingBasedClassSimilarity sbc = new SamplingBasedClassSimilarity();
        double value = sbc.getSimilarity("http://dbpedia.org/ontology/Place", "http://dbpedia.org/ontology/Town", "http://live.dbpedia.org/sparql", "http://live.dbpedia.org/sparql");
        System.out.println(value);

    }

    public static void main(String args[]) {
        test();
    }
}
