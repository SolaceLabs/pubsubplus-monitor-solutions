/*
 * Copyright 2014-2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company. If there are no 
 * contracts in place use of this source is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless 
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported by Solace 
 * unless such support is provided for under an agreement 
 * signed between you and Solace.
 */

package com.solace.psg.enterprisestats.statspump.util;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.config.ConfigLoader;

/**
 * list resources available from the classpath @ *
 */
public class ResourceList{
    private static final Logger logger = LoggerFactory.getLogger(ResourceList.class);
    
    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     * 
     * @param pattern
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public static Set<URL> getResources(final Pattern pattern) {
        final Set<URL> retval = new LinkedHashSet<URL>();
        final String classPath = System.getProperty("java.class.path",".");
        final String[] classPathElements = classPath.split(System.getProperty("path.separator"));
        for (final String element : classPathElements){
            retval.addAll(getResources(element,pattern));
        }
        return retval;
    }

    private static Set<URL> getResources(final String element, final Pattern pattern) {
        final Set<URL> retval = new LinkedHashSet<URL>();
        final File file = new File(element);
        if (file.isDirectory()){
            retval.addAll(getResourcesFromDirectory(file,pattern,""));
        }
        return retval;
    }

    private static Set<URL> getResourcesFromDirectory(final File directory, final Pattern pattern, String relPath){
        final Set<URL> retval = new LinkedHashSet<URL>();
        final File[] fileList = directory.listFiles();
        for (final File file : fileList){
            if (file.isDirectory()){
                retval.addAll(getResourcesFromDirectory(file, pattern,relPath+file.getName()+System.getProperty("file.separator")));
            } else {
                final String fileName = file.getName();
                final boolean accept = pattern.matcher(fileName).matches();
                if (accept){
                    retval.add(ConfigLoader.class.getClassLoader().getResource(relPath+fileName));  // must be relative
                }
            }
        }
        return retval;
    }

    /**
     * list the resources that match args[0]
     * 
     * @param args
     *            args[0] is the pattern to match, or list all resources if
     *            there are no args
     */
    public static void main(final String[] args){
        Pattern pattern;
        if(args.length < 1){
            pattern = Pattern.compile(".*");
        } else{
            pattern = Pattern.compile(args[0]);
        }
        pattern = Pattern.compile(".*semp.+soltr.+xsd");
        final Set<URL> list = ResourceList.getResources(pattern);
        for(final URL name : list){
            logger.info("Found resource: "+name);
        }
    }
}  
