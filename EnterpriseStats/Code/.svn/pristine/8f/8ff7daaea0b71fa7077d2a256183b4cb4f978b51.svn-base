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
package com.solace.psg.enterprisestats.statspump;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.pollers.VpnDetailPoller;
import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;
import com.solace.psg.enterprisestats.statspump.tools.semp.SempVersion;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

public class PhysicalAppliance {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalAppliance.class);

    enum TopicFormat {
        NONE, SMF, MQTT, BOTH;
    }

    public static final String VALUE_LOCAL_ACTIVE = "Local Active";
    public static final String VALUE_AD_ACTIVE = "AD-Active";
    public static final String VALUE_UP = "Up";

    // provided
    private final LogicalAppliance parent;
    private final String sempHostnameOrIp;
    private final String sempUsername;
    private final String sempPassword;
    private volatile String hostname;
    private final boolean useSecureSession;		// use SEMP over SSL if true

    // TBD --> not final... will populate upon connection
    // private String sempVersion = StatsPumpConstants.UNINITIALIZED_VALUE; //
    // this will be the SEMP version like soltr/7_0 (could also look like
    // 'soltr/7_1_1'
    private SempVersion sempVersion = new SempVersion();
    private String primaryActivity = PumpConstants.UNINITIALIZED_VALUE;
    private String backupActivity = PumpConstants.UNINITIALIZED_VALUE;
    private String adActivity = PumpConstants.UNINITIALIZED_VALUE;
    private String redundancyMode = PumpConstants.UNINITIALIZED_VALUE;
    private final Set<String> activeVpns = new LinkedHashSet<String>();
    private final Map<String, Map<String, String>> vpnStatuses = new LinkedHashMap<String, Map<String, String>>();
    private final Map<String, TopicFormat> vpnTopicFormats = new LinkedHashMap<String, TopicFormat>();
    private AtomicInteger consecutiveMissedPoll = new AtomicInteger(0);
    private AtomicBoolean reachable = new AtomicBoolean(false);

    public PhysicalAppliance(LogicalAppliance parent, String sempHostname, String sempUsername, String sempPassword, boolean useSecureSession) {
        this.parent = parent;
        this.sempHostnameOrIp = sempHostname.toLowerCase();
        this.sempUsername = sempUsername;
        this.sempPassword = sempPassword;
        this.hostname = sempHostname;
        this.useSecureSession = useSecureSession;
        if (this.sempHostnameOrIp == null || this.sempHostnameOrIp.isEmpty())
            throw new IllegalArgumentException("Appliance SEMP host + port cannot be empty");
        if (this.sempUsername == null || this.sempUsername.isEmpty())
            throw new IllegalArgumentException("SEMP username cannot be empty");
        if (this.sempPassword == null)
            throw new IllegalArgumentException("SEMP password cannot be null");
    }

    /**
     * Returns what this PhysicalAppliance was configured with for a hostname,
     * how to connect.
     * 
     * @return
     */
    public String getSempHostnameOrIp() {
        return sempHostnameOrIp;
    }

    public String getSempUsername() {
        return sempUsername;
    }
    public boolean isSecureSession() {
        return useSecureSession;
    }

    public String getSempPassword() {
        return sempPassword;
    }

    @Override
    public String toString() {
        return getHostname();
    }

    public LogicalAppliance getLogical() {
        return parent;
    }

    public boolean isPrimary() {
        return this == parent.getPrimary();
    }

    /**
     * Returns the detected hostname of the PhysicalAppliance. This will be
     * populated once the Pump connects and the 'show hostname' script runs.
     * 
     * @return
     */
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        if (!this.hostname.equals(hostname)) {
            if (hostname == null) {
                logger.warn(String.format("Hostname of '%s' (%s) attempting to change from '%s' to '%s'", this,
                        sempHostnameOrIp, this.hostname, hostname));
            } else {
                logger.info(String.format("Hostname of '%s' changing to '%s'", this, hostname));
                this.hostname = hostname;
            }
        }
    }

    public SempVersion getSempVersion() {
        return sempVersion;
    }

    public void setSempVersion(String sempVersionString) {
        if (!this.sempVersion.equals(sempVersionString)) {
            if (sempVersionString == null) {
                logger.warn(String.format("SEMP version of '%s' attempting to change from '%s' to '%s'", this,
                        sempVersion, sempVersionString));
                this.sempVersion = new SempVersion();
            } else {
                logger.info(String.format("SEMP version of '%s' changing from '%s' to '%s'", this, sempVersion,
                        sempVersionString));
                // this.sempVersion = sempVersion;
                sempVersion = new SempVersion(sempVersionString);
            }
        }
    }

    public boolean isPrimaryActive() {
        return VALUE_LOCAL_ACTIVE.equals(primaryActivity);
    }

    public String getPrimaryActivity() {
        return primaryActivity;
    }

    public boolean isBackupActive() {
        return VALUE_LOCAL_ACTIVE.equals(backupActivity);
    }

    public String getBackupActivity() {
        return backupActivity;
    }

    public void setPrimaryLocalActivity(String primaryActivity) {
        if (!this.primaryActivity.equals(primaryActivity)) {
            if (primaryActivity == null) {
                logger.warn(String.format(
                        "Local Activity for Primary Virtual Router on '%s' attempting to change from '%s' to '%s'",
                        this, this.primaryActivity, primaryActivity));
                this.primaryActivity = PumpConstants.UNINITIALIZED_VALUE;
            } else {
                logger.info(
                        String.format("Local Activity for Primary Virtual Router on '%s' changing from '%s' to '%s'",
                                this, this.primaryActivity, primaryActivity));
                this.primaryActivity = primaryActivity;
            }
        }
    }

    public void setBackupLocalActivity(final String backupActivity) {
        if (!this.backupActivity.equals(backupActivity)) {
            if (backupActivity == null) {
                logger.warn(String.format(
                        "Local Activity for Backup Virtual Router on '%s' attempting to change from '%s' to '%s'", this,
                        this.backupActivity, backupActivity));
                this.backupActivity = PumpConstants.UNINITIALIZED_VALUE;
            } else {
                logger.info(String.format("Local Activity for Backup Virtual Router on '%s' changing from '%s' to '%s'",
                        this, this.backupActivity, backupActivity));
                this.backupActivity = backupActivity;
            }
        }
    }

    public boolean isAdActive() {
        return VALUE_AD_ACTIVE.equals(adActivity);
    }

    public String getAdActivity() {
        return adActivity;
    }

    public void setAdActivity(final String adActivity) {
        if (!this.adActivity.equals(adActivity)) {
            if (adActivity == null) {
                logger.warn(String.format("AD Activity on '%s' attempting to change from '%s' to '%s'", this,
                        this.adActivity, adActivity));
                this.adActivity = PumpConstants.UNINITIALIZED_VALUE;
            } else {
                logger.info(String.format("AD Activity on '%s' changing from '%s' to '%s'", this, this.adActivity,
                        adActivity));
                this.adActivity = adActivity;
            }
        }
    }

    public String getRedundancyMode() {
        return redundancyMode;
    }

    public void setRedundancyMode(final String redundancyMode) {
        if (!this.redundancyMode.equals(redundancyMode)) {
            if (redundancyMode == null) {
                logger.warn(String.format("Detected Redundancy Mode on '%s' attempting to change from '%s' to '%s'",
                        this, this.redundancyMode, redundancyMode));
                this.redundancyMode = PumpConstants.UNINITIALIZED_VALUE;
            } else {
                logger.info(String.format("Detected Redundancy Mode on '%s' changing from '%s' to '%s'", this,
                        this.redundancyMode, redundancyMode));
                this.redundancyMode = redundancyMode;
            }
        }
    }

    public TopicFormat getVpnTopicFormat(final String vpn) {
        return vpnTopicFormats.get(vpn);
    }

    private static TopicFormat getTopicFormat(final Map<String, String> statusMap) {
        // mqtt - must explicitly be 'true', other false (e.g. UNINITIALIZED)
        boolean mqtt = Boolean.parseBoolean(statusMap.get(VpnDetailPoller.TAG_MQTT_TOPIC_FORMAT));
        // smf - only false if map actually says 'false', otherwise true. i.e.
        // if ~UNDEFINED~ it's pre-7.1.1 and we'll call it true
        boolean smf = !statusMap.get(VpnDetailPoller.TAG_SMF_TOPIC_FORMAT).equalsIgnoreCase("false");
        if (mqtt) {
            if (smf) {
                return TopicFormat.BOTH;
            } else { // just MQTT
                return TopicFormat.MQTT;
            }
        } else { // MQTT is either false or ~UNDEFINED~ so just use SMF
            if (smf) {
                return TopicFormat.SMF;
            } else {
                return TopicFormat.NONE;
            }
        }
    }

    private void setVpnStatus(final String vpn, final Map<String, String> statusMap) {
        vpnStatuses.put(vpn, statusMap);
        TopicFormat tf = getTopicFormat(statusMap);
        if (!tf.equals(vpnTopicFormats.get(vpn))) { // get will return false on
                                                    // first run when Map has
                                                    // nulls
            logger.info(String.format("Topic format for VPN '%s' on %s changing from '%s' to '%s'", this, vpn,
                    vpnTopicFormats.get(vpn), tf));
            vpnTopicFormats.put(vpn, tf);
        }
    }

    /**
     * Private method to check the difference in a set of maps... keys have to
     * be the same
     */
    private static List<String> helperMapDiff(Map<String, String> map1, Map<String, String> map2) {
        assert map1.keySet().equals(map2.keySet());
        List<String> returnList = new ArrayList<String>();
        for (String key : map1.keySet()) {
            if (!map1.get(key).equals(map2.get(key))) {
                returnList.add(new StringBuilder(key).append('=').append(map1.get(key)).append("-->")
                        .append(map2.get(key)).toString());
            }
        }
        return returnList;
    }

    public void updateVpnStatus(final String vpn, final Map<String, String> statusMap) {
        assert statusMap != null;
        // is it different than what's in there now?
        if (!statusMap.equals(vpnStatuses.get(vpn))) {
            // different statuses! Either new, or something has changed. First,
            // is this VPN active now?
            boolean currentlyActive = Boolean.parseBoolean(statusMap.get(VpnDetailPoller.TAG_ENABLED))
                    && Boolean.parseBoolean(statusMap.get(VpnDetailPoller.TAG_OPERATIONAL))
                    && Boolean.parseBoolean(statusMap.get(VpnDetailPoller.TAG_LOCALLY_CONFIGURED))
                    && VALUE_UP.equals(statusMap.get(VpnDetailPoller.TAG_LOCAL_STATUS))
                    && getTopicFormat(statusMap) != TopicFormat.NONE;
            // Then, is this a brand new VPN? (i.e. vpnStatuses.get(vpn) would
            // return null)
            if (!vpnStatuses.containsKey(vpn)) {
                logger.info(String.format("Detected %s VPN '%s' on '%s': %s", currentlyActive ? "ACTIVE" : "inactive",
                        vpn, this, statusMap));
                // add it first, so we don't get two INFO logs about the 'change in status'
                vpnTopicFormats.put(vpn, getTopicFormat(statusMap)); 
                setVpnStatus(vpn, statusMap);
                if (currentlyActive)
                    activeVpns.add(vpn);
            } else {
                // else, it's already there, but one of the values has changed
                if (currentlyActive == activeVpns.contains(vpn)) {
                    // the same activity... so that doesn't change
                    logger.info(String.format("Detected changed status for %s VPN '%s' on '%s' : %s",
                            currentlyActive ? "ACTIVE" : "inactive", vpn, this,
                            helperMapDiff(vpnStatuses.get(vpn), statusMap)));
                    setVpnStatus(vpn, statusMap);
                } else {
                    logger.info(String.format("Activity status VPN '%s' on '%s' changing from %s to %s : %s", vpn, this,
                            activeVpns.contains(vpn) ? "ACTIVE" : "inactive", currentlyActive ? "ACTIVE" : "inactive",
                            helperMapDiff(vpnStatuses.get(vpn), statusMap)));
                    setVpnStatus(vpn, statusMap);
                    if (currentlyActive) {
                        activeVpns.add(vpn);
                    } else {
                        activeVpns.remove(vpn);
                    }
                }
            }
        }
    }

    public boolean isActiveVpn(String vpn) {
        return activeVpns.contains(vpn);
    }

    public boolean isReachable() {
        return reachable.get();
    }

    /**
     * The only thing calling this method should be the Reachable Callable. And
     * that No regular Poller should be able to reset the reachable state.
     * However, they can declare it down after 5
     * (StatsPumpConstants.MISSED_SEMP_POLL_LIMIT) missed polls.
     * 
     */
    public void declareReachable() {
        logger.info(String.format("Established SEMP connectivity to %s... changing state to UP", this));
        boolean isReachable = reachable.getAndSet(true);
        assert !isReachable; // there is no way this should be run by the
                             // ReachableCallable unless this appliance is not
                             // reachable!
    }

    /**
     * This method will be run at the successful completion of a Poller inside
     * the PollerRunnable. Or it will be called by the ReachableRunnable if it
     * successfully performs the initial queries. Hence, it should be impossible
     * for this method to get called by a regular PollerRunnable if the
     * appliance is not reachable.
     */
    public void resetMissedPoll() {
        synchronized (consecutiveMissedPoll) {
            consecutiveMissedPoll.set(0);
        }
    }

    public void incMissedPoll() {
        synchronized (consecutiveMissedPoll) {
            if (!isReachable())
                return; // if we've already declared this appliance unreachable,
                        // then fall out... no need to continue counting
            int count = consecutiveMissedPoll.incrementAndGet();
            logger.debug("############# " + count + " MISSED POLLS for " + this);
            if (count >= StatsPumpConstants.MISSED_SEMP_POLL_LIMIT) { // or
                                                                      // could
                                                                      // have
                                                                      // used ==
                                                                      // doesn't
                                                                      // matter
                reachable.set(false); // this will allow the Reachable Callable
                                      // to run
                logger.warn(String.format("Unable to perform %d SEMP requests on %s... changing state to DOWN", count,
                        this));
                setPrimaryLocalActivity(PumpConstants.UNINITIALIZED_VALUE);
                setBackupLocalActivity(PumpConstants.UNINITIALIZED_VALUE);
                setAdActivity(PumpConstants.UNINITIALIZED_VALUE);
                setRedundancyMode(PumpConstants.UNINITIALIZED_VALUE);
            }
        }
    }

}
