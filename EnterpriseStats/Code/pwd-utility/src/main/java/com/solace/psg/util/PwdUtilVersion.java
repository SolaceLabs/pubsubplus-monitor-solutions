/**
 * Copyright 2016-2017 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */
package com.solace.psg.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * A helper utility class to retrieve the version number at runtime.
 *
 */
public class PwdUtilVersion {
    private final String version;
    private static final PwdUtilVersion CURRENT;
    private static final String VER_RESOURCE_NAME = "version.properties";

    static {
        File resource = new File(VER_RESOURCE_NAME);
        if (resource == null || !resource.exists()) {
            // Ideally we should throw a exception, but for now just set version
            // to 0.0.0 
            // throw new RuntimeException("Resource '" + VER_RESOURCE_NAME +
            // "'not found.");
            CURRENT = new PwdUtilVersion("0.0.0");
        } else {

            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(resource);
                Properties properties = new Properties();
                properties.load(inputStream);

                String major = properties.get("major").toString();
                String minor = properties.get("minor").toString();
                String patch = properties.get("patch").toString();
                String version = String.format("%s.%s.%s", major, minor, patch);
                CURRENT = new PwdUtilVersion(version);
            } catch (Exception e) {
                throw new RuntimeException("Could not load version details from resource '" + resource + "'.", e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static PwdUtilVersion current() {
        return CURRENT;
    }

    private PwdUtilVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Solace Password Utility version: " + version;
    }

    public String getVersion() {
        return version;
    }
}
