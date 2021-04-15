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

package com.solace.psg.enterprisestats.statspump.tools.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * list resources available from the classpath
 */
public class ResourceList{
    private static final Logger logger = LoggerFactory.getLogger(ResourceList.class);
    
    /**
     * Searches for resource files matching the supplied pattern. Searches both the jar and folders in the classpath 
     * if nothing found in the jar (running in eclipse).
     * 
     * @param pattern the pattern to match
     * @return the resources in the order they are found
     */
    public static Set<URL> getResources(final Pattern pattern) {
    	Set<URL> rc = getResourcesInThisJar(pattern);

    	// First, lets load the resources from the application's own JAR file
    	Set<URL> rcFromFiles = getResourcesFromFileSystem(pattern);

    	// now lets check and see if there are extra resources found in the applications distribution folder 
    	// currently using the gradle build and auto-generated launch scripts, a "resources" sub folder
    	// under the application's main installation folder will be included in the classpath and hence picked up
    	// by these next calls to find files on the file system. There are details in the runbook. 
    	int nResourcesFoundOnTheFileSystem = rcFromFiles.size(); 
    	if (nResourcesFoundOnTheFileSystem > 0) {
            logger.info("StatsPump found " + nResourcesFoundOnTheFileSystem +  " extra resource files outside of the jar in the file system. Overlaying these resources on top of the jar based ones.");
    		rc.addAll(rcFromFiles);
    	}
    	
    	// if we are at full debug, spit all of the loaded resources out
    	if (logger.isDebugEnabled()) {
			logger.debug("-Begin all resources loaded dump---------");
    		Iterator<URL> iter = rc.iterator();
    		while (iter.hasNext()) {
    			URL oneUrl = iter.next();
    			logger.debug(oneUrl.toString());
    		}
			logger.debug("-End all resources loaded dump---------");
    	}
    	
    	return rc;
    }

    /**
     * Searches via the resources subfolder inside of the jar for pattern matching files
     * 
     * Note: if running from eclipse without producing a jar, this will fail to find anything.
     * The other method 
     * 
     * @param pattern the pattern to match
     * @return the resources in the order they are found
     */
    public static Set<URL> getResourcesInThisJar(final Pattern pattern) {
        logger.info("StatsPump is searching for resources matching regex pattern '" + pattern + "' inside its own jar..");
        final Set<URL> retval = new LinkedHashSet<URL>();

        try {
			CodeSource src = ResourceList.class.getProtectionDomain().getCodeSource();
			if( src != null ) {
			    URL jar = src.getLocation();
			    logger.debug("This class jarfile URL is = '" + jar.getPath() + "'.");
			    ZipInputStream zip = new ZipInputStream( jar.openStream());
			    ZipEntry ze = null;
	
			    while( ( ze = zip.getNextEntry() ) != null ) {
			        String entryName = ze.getName();
			        if (entryName.startsWith("resources/")) {
			        	
		                final boolean accept = pattern.matcher(entryName).matches();
		                if (accept){
		                	logger.debug("one entryName in jar =" + entryName);
				        	URL oneresourceURL = ResourceList.class.getClassLoader().getResource(entryName);
				        	logger.debug("oneresourceURL=" + oneresourceURL.toString());
				        	
				        	retval.add(oneresourceURL);
		                }
		                else {
		                	logger.debug(entryName + " doesn't match regex pattern.");
		                }
			        }
			    }
			}
		}
		catch (IOException e) {
		}
		
        return retval;
    }
    
    /**
     * Searches the local file system. 
     * 
     * Make sure that you put this applications resources in your class path. Since
     * using Gradle, we now put all of the resources in pump/src/main/resources/resources. The
     * double resources/resource simply keep the jar file a little neater, so that there is a 
     * top level resources folder inside the jar.
     * 
     * Searches for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     * 
     * @param pattern the pattern to match
     * @return the resources in the order they are found
     */
    public static Set<URL> getResourcesFromFileSystem(final Pattern pattern) {
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
                	String resourceFile = relPath + fileName;
                	logger.debug("Found resource '" + resourceFile + "'.");
                    retval.add(ResourceList.class.getClassLoader().getResource(resourceFile));  // must be relative
                }
            }
        }
        return retval;
    }
}  
