/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.replacement;

import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.aksw.saim.transfer.config.ConfigReader;
import org.aksw.saim.transfer.config.Configuration;

/**
 *
 * @author ngonga
 */
public class Replacer {

    /**
     * Replaces the measure in a configuration
     *
     * @param goalConfig Configuration
     * @param goalConfigProperty Property in the initial config
     * @param property Property in the novel config
     * @return New configuration
     */
    public static int COUNT = 0;
    public static Configuration replace(Configuration goalConfig, String goalConfigProperty, Configuration config, String property, boolean source) {
        // check which variable is to replace and assign it to var variable e.g ?a => var =a
        String var;
        String help = property;
        if (source) {
            var = goalConfig.source.var;
        } else {
            var = goalConfig.target.var;
        }
        if (var.startsWith("?")) {
            var = var.substring(1);
        }
        //Extract the namespace of the property
        String nameSpace = "";
        if (property.contains("#")) {
            nameSpace = property.substring(0, property.indexOf("#") + 1);
            property = property.substring(property.indexOf("#") + 1);
        } else {
            nameSpace = property.substring(0, property.lastIndexOf("/") + 1);
            property = property.substring(property.lastIndexOf("/") + 1);
        }
        //get the namespace prefix name (shortname)
        property = getShortName(config, goalConfig, nameSpace) + ":" + property;
        if(property.startsWith("null"))
        {
            property = "error";
        }
        
        String replacement = var + "." + property;
        String toReplace = var + "." + goalConfigProperty;
        //replace in the configuration the old property with the new property
        goalConfig.measure = goalConfig.measure.replaceAll(Pattern.quote(toReplace), replacement);

        if (source) {
            int index = goalConfig.source.properties.indexOf(goalConfigProperty);
            if (index == -1) {
                System.err.println("Goal property " + goalConfigProperty + " no found ");
            } else {//change in the hashtags using the key to change the old value of the property with the new one and prefixes too
                goalConfig.source.properties.set(index, property);// set new property value
                goalConfig.source.functions.put(property,"lowercase");//add/change the function to lowercase 
                goalConfig.source.prefixes.put(config.source.prefixes.get(nameSpace), nameSpace);//change the prefix of the property in the source
                goalConfig.target.prefixes.put(config.source.prefixes.get(nameSpace), nameSpace);//add the prefix of the property in the target
            }
        } else {
            int index = goalConfig.target.properties.indexOf(goalConfigProperty);
            if (index == -1) {
                System.err.println("Goal property " + goalConfigProperty + " no found ");
            } else {
                goalConfig.target.properties.set(index, property);
                goalConfig.target.functions.put(property,"lowercase");
                goalConfig.target.prefixes.put(config.target.prefixes.get(nameSpace), nameSpace);
                goalConfig.source.prefixes.put(config.target.prefixes.get(nameSpace), nameSpace);
            }
        }

        //get reduced from of property

        return goalConfig;
    }
    
/**
 * returns the shortname of a name space either used in source or target and if not found add it with generated shortname to both prefixes
 * @param c
 * @param goalConfig
 * @param nameSpace
 * @return
 */
    public static String getShortName(Configuration c, Configuration goalConfig, String nameSpace) {
        for (Entry<String, String> e : c.source.prefixes.entrySet()) {//check in used source prefixes and return when prefix is found
            if (e.getValue().equals(nameSpace)) {
                return e.getKey();
            }
        }
        for (Entry<String, String> e : c.target.prefixes.entrySet()) {//check in used target prefixes and return when prefix is found
            if (e.getValue().equals(nameSpace)) {
                return e.getKey();
            }
        }
//           for (Entry<String, String> e : goalConfig.source.prefixes.entrySet()) {
//            if (e.getValue().equals(nameSpace)) {
//                c.getSource().prefixes.put(e.getKey(),e.getValue());
//                goalConfig.getTarget().prefixes.put(e.getKey(),e.getValue());
//                return e.getKey();
//            }
//        }
//        for (Entry<String, String> e : goalConfig.target.prefixes.entrySet()) {
//            if (e.getValue().equals(nameSpace)) {
//                c.getSource().prefixes.put(e.getKey(),e.getValue());
//                goalConfig.getTarget().prefixes.put(e.getKey(),e.getValue());
//                return e.getKey();
//            }
//        }
//        // if namespace is not found then add it to the source and target prefixes with a generated shorname 
        goalConfig.getSource().prefixes.put("ns"+COUNT, nameSpace);
        goalConfig.getTarget().prefixes.put("ns"+COUNT, nameSpace);        
        COUNT++;
        return "ns"+(COUNT-1);
    }

    public static void main(String args[]) {
        ConfigReader cr = new ConfigReader();
        Configuration c = cr.readLimesConfig("resources/dailymed-dbpedia.limes.xml");
        //c = Replacer.replace(c, "rdfs:label", "foaf:label", true);
        System.out.println(c.measure);
    }
}
