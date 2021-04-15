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

package com.solace.psg.enterprisestats.statspump.tools.semp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;

/**
 * This class is a sortable representation of a SEMP version.
 * It expects the form 'soltr/x_y' or 'soltr/x_y_z' where
 * x|y|z is a positive integer, and the major version (x) is >= 5.<p>
 * This class is immutable.
 */
public class SempVersion implements Comparable<SempVersion> {

    public static final Pattern SOLTR_SCHEMA_VERSION_FORMAT_REGEX = Pattern.compile("soltr/(\\d+)_(\\d+)(?:_(\\d+))?(VMR)?");  // e.g. soltr/7_1_1
    private final String sempVersion;
    private final List<Integer> versionNumbers;

    /**
     * Empty constructor, equivalent to SolOS version 0.0.0
     */
    public SempVersion() {
        sempVersion = PumpConstants.UNINITIALIZED_VALUE;
        List<Integer> tempList = new ArrayList<Integer>();
        tempList.add(0);
        tempList.add(0);
        tempList.add(0);
        versionNumbers = Collections.unmodifiableList(tempList);
    }
    
    public SempVersion(String sempVersion) {
        Matcher m = SOLTR_SCHEMA_VERSION_FORMAT_REGEX.matcher(sempVersion);
        if (m.matches()) {
            versionNumbers = Collections.unmodifiableList(parseMatcher(m));
            this.sempVersion = sempVersion;
        } else {
            throw new IllegalArgumentException(sempVersion+" does not meet valid format definition: "+SOLTR_SCHEMA_VERSION_FORMAT_REGEX.pattern());
        }
    }
    
    private List<Integer> parseMatcher(Matcher m) {
        List<Integer> vers = new ArrayList<Integer>();
        vers.add(Integer.parseInt(m.group(1)));  // should parse correctly as the pattern matching would ensure it's a number
        if (vers.get(0) < 5) {
            throw new IllegalArgumentException(sempVersion+" not valid: must be at least soltr/5_0");
        }
        vers.add(Integer.parseInt(m.group(2)));
        if (m.group(3) == null) {
            vers.add(0);
        } else {
            vers.add(Integer.parseInt(m.group(3)));
        }
        return vers;
    }
    
    public int getMajorVersion() {
        return versionNumbers.get(0);
    }
    
    public int getMinorVersion() {
        return versionNumbers.get(1);
    }
    
    public int getMaintVersion() {
        return versionNumbers.get(2);
    }
    
    @Override
    public int compareTo(SempVersion sempVersion) {
        assert versionNumbers.size() == 3;
        assert sempVersion.versionNumbers.size() == 3;
        List<Integer> a = this.versionNumbers;
        List<Integer> b = sempVersion.versionNumbers;
        for (int i=0;i<3;i++) {
            //System.out.printf("%d: %d vs %d%n",i,a.get(i),b.get(i));
            int res = a.get(i).compareTo(b.get(i));
            if (res != 0) return res;
        }
        return 0;
    }

    public boolean isValid(SempVersion minVersion, SempVersion maxVersion) {
        if (minVersion == null && maxVersion == null) return true;
        if (sempVersion.equals(PumpConstants.UNINITIALIZED_VALUE)) return true;
        boolean meetsMinVer = true;
        boolean meetsMaxVer = true;
        if (minVersion != null) {
            meetsMinVer = this.compareTo(minVersion) >= 0;
        }
        if (maxVersion != null) {
            meetsMaxVer = this.compareTo(maxVersion) <= 0;
        }
        return (meetsMinVer && meetsMaxVer);
    }

    /**
     * Can compare with another SempSolTrVersion object, or a String of
     * the format 'soltr/6_0'.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o instanceof SempVersion) {
            return versionNumbers.equals(((SempVersion)o).versionNumbers);  // equals of ArrayList will make sure they're the same
        } else if (o instanceof String) {
            return sempVersion.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + versionNumbers.get(0);
        result = 37 * result + versionNumbers.get(1);
        result = 39 * result + versionNumbers.get(2);
        return result;
    }
    
    @Override
    public String toString() {
        return sempVersion;
    }
    
 // The code below this point can be used for informal unit testing
    
//    public static void main(String... args) {
//        SempVersion v1 = new SempVersion("soltr/6_0");
//        SempVersion v2 = new SempVersion("soltr/6_0");
//        System.out.println(v1.equals(v2));
//        System.out.println(v1.hashCode());
//        System.out.println(v2.hashCode());
//    }
}
