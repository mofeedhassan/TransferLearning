/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.aksw.saim.util.SparqlUtils;

/**
 * TODO, download xslt, implement classes
 *
 * @author ngonga
 */
public class ConfigReader {

    public String XSLT = "resources/silk2limes2.xslt";

    /**
     * reads a limes config and transforms it into a transfer learner config
     *
     * @param config LIMES config
     * @return Output
     */
    public Configuration readLimesConfig(String config) {
        if (config == null) {
            return new Configuration();
        }
        de.uni_leipzig.simba.io.ConfigReader cr = new de.uni_leipzig.simba.io.ConfigReader();
        cr.validateAndRead(config);
        Configuration cf = new Configuration();
        cf.source = cr.sourceInfo;
        cf.target = cr.targetInfo;
        cf.measure = cr.metricExpression;
        cf.threshold = cr.acceptanceThreshold;
        cf.name= config;
        return cf;
    }

    /**
     * Reads a SILK config by transforming it into a LIMES config
     *
     * @param config SILK Config
     * @return Transfer Learner config
     */
    public Configuration readSilkConfig(String config) {
        String limesConfig = transformToLimes(config);
        return readLimesConfig(limesConfig);
    }

    /**
     * Transforms a silk config into a limes config
     *
     * @param config SILK config
     * @return LIMES config
     */
    private String transformToLimes(String config) {
        String temp = null;
        try {
            temp = File.createTempFile("boo", "woo").getAbsolutePath();
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File(XSLT));
            Transformer transformer = factory.newTransformer(xslt);

            Source xml = new StreamSource(new File(config));
            transformer.transform(xml, new StreamResult(new File(temp)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private void transformToLimes(String config, String output) {
        try {

            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File(XSLT));
            Transformer transformer = factory.newTransformer(xslt);

            Source xml = new StreamSource(new File(config));
            transformer.transform(xml, new StreamResult(new File(output)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads all specs and transforms SILK specs to LIMES specs if they do not
     * yet exist
     *
     * @param inputFolder Folder where the original specs are to be founds
     * @param outputFolder Output folder
     */
    public void convert(String inputFolder, String outputFolder) {
        Set<String> specs = readFolder(inputFolder);
        for (String spec : specs) {
            String output = spec.replaceAll(Pattern.quote(inputFolder), outputFolder);
            String folder = output.substring(0, output.lastIndexOf("\\"));
            System.out.println("Converting " + spec + " to " + output);
            if (!new File(folder).exists()) {
                new File(folder).mkdirs();

                if (!spec.contains("limes")) {
                    transformToLimes(spec, output);
                } else {
                    copyFile(spec, output);
                }

                Configuration c = readLimesConfig(output);
                if (!SparqlUtils.isAlive(c.source.endpoint, null) || !SparqlUtils.isAlive(c.target.endpoint, null)) {
                    System.err.println("One SPARQL endpoint of " + output + " is not alive.");
                    //new File(output).delete();
//                    new File(folder).delete();
                }
            }
        }
        for (String spec : specs) {
            String output = spec.replaceAll(Pattern.quote(inputFolder), outputFolder);
            File folder = new File(output.substring(0, output.lastIndexOf("\\")));
            if (folder.exists()) {
                folder.delete();
            }
        }
    }

    public void copyFile(String fromFile, String toFile) {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytesRead); // write
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                    ;
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
    }

    /**
     * Gets all the specs that are to be read
     *
     * @param inputFolder Folder for reading the specs
     * @return Set of paths
     */
    private static Set<String> readFolder(String inputFolder) {
        Set<String> paths = new TreeSet<String>();
        Set<String> queue = new TreeSet<String>();
        queue.add(inputFolder);
        while (!queue.isEmpty()) {
            Set<String> copy = new TreeSet<String>();
            for (String s : queue) {
                // if file is directory then process everything in it
                File f = new File(s);
                if (f.isDirectory()) {
                    File[] files = new File(s).listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            copy.add(files[i].getAbsolutePath());
                        } else if (files[i].getAbsolutePath().endsWith("spec.xml")) {
                            if (new File(s + "/positive.nt").exists()) {
                                paths.add(files[i].getAbsolutePath());
                            } else {
                                System.err.println("No examples for spec " + f.getAbsolutePath());
                            }
                        }
                    }
                }
                // if it's a proper file then simply add it to the list of path                   
            }
            queue = copy;
        }
        return paths;
    }

    public static void main(String args[]) {
        ConfigReader cr = new ConfigReader();
        cr.convert("specs", "limesSpecsAll");
    }
}
