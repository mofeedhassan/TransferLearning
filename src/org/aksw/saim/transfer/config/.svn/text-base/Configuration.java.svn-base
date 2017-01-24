/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.saim.transfer.config;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.io.KBInfo;
import org.aksw.saim.util.Execution;

/**
 *
 * @author ngonga
 */
public class Configuration implements Comparable {

    public KBInfo source;
    public KBInfo target;
    public String measure;
    public double threshold;
    public String name;
    public double similarity;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Configuration() {
    }

    public KBInfo getSource() {
        return source;
    }

    public void setSource(KBInfo source) {
        this.source = source;
    }

    public KBInfo getTarget() {
        return target;
    }

    public void setTarget(KBInfo target) {
        this.target = target;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Configuration) {
            Configuration c = (Configuration) o;
            if (similarity > c.similarity) {
                return 1;
            }
            if (similarity < c.similarity) {
                return -1;
            }
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public String toString()
    {
        return "("+name+", "+similarity+")";
    }
    
    public Mapping run() {
        HybridCache sourceCache = HybridCache.getData(source);
        HybridCache targetCache = HybridCache.getData(target);
        return Execution.execute(sourceCache, targetCache, measure, threshold);
    }
}
