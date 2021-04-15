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

package com.solace.psg.enterprisestats.statspump.pollers;

import java.util.Map;

import com.solace.psg.enterprisestats.statspump.PhysicalAppliance;
import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.config.xml.RunCondition;
import com.solace.psg.enterprisestats.statspump.containers.SingleContainerSet;
import com.solace.psg.enterprisestats.statspump.semp.GenericSempSaxHandler;
import com.solace.psg.enterprisestats.statspump.semp.SempSaxProcessingListener;
import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;

public class RedundancyPoller extends GenericPoller {

    private static final String REDUNDANCY_MODE = "REDUNDANCY_MODE";
    private static final String PRIMARY_ACTIVITY = "PRIMARY_ACTIVITY";
    private static final String BACKUP_ACTIVITY = "BACKUP_ACTIVITY";
    private static final String PRIMARY_ACTIVITY_DETAIL = "PRIMARY_ACTIVITY_DETAIL";
    private static final String BACKUP_ACTIVITY_DETAIL = "BACKUP_ACTIVITY_DETAIL";
    
    private static final RedundancyPoller INSTANCE;
    static {
        GenericPoller.Builder b = new Builder();
        b.setName("show redundancy detail");
        b.setScope(Scope.SYSTEM);
        b.setDescription("Reports on the activity states of this router and those reported by the mate.");
        b.setSempRequest("<rpc semp-version='%s'><show><redundancy><detail/></redundancy></show></rpc>");
        b.setRunConditionOnPrimaryWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAS(RunCondition.ALWAYS);
        b.setRunConditionOnPrimaryWhenAA(RunCondition.ALWAYS);
        b.setRunConditionOnBackupWhenAA(RunCondition.ALWAYS);
        b.setDestination(DestinationType.MGMT);
        b.setTopicStringSuffix("REDUNDANCY");
        b.setBaseTag("/rpc-reply/rpc/show/redundancy");
        b.addObjectTag(REDUNDANCY_MODE,"/redundancy-mode");
        b.addObjectTag(PRIMARY_ACTIVITY,"/virtual-routers/primary/status/activity");
        b.addObjectTag(BACKUP_ACTIVITY,"/virtual-routers/backup/status/activity");
        b.addObjectTag(PRIMARY_ACTIVITY_DETAIL,"/virtual-routers/primary/status/detail/activity-status/summary");
        b.addObjectTag(BACKUP_ACTIVITY_DETAIL,"/virtual-routers/backup/status/detail/activity-status/summary");
        INSTANCE = new RedundancyPoller(b);
    }
    
    public static final Poller getInstance() {
        return INSTANCE;
    }

    private RedundancyPoller(Builder b) {
        super(b);
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance) {
        return buildSaxHandler(appliance,new RedundancyPollerListener(appliance));
    }
    
    @Override
    public GenericSempSaxHandler buildSaxHandler(final PhysicalAppliance appliance, final SempSaxProcessingListener listener) {
        return new GenericSempSaxHandler(this,appliance,listener,getObjectTags());
    }
    
    private class RedundancyPollerListener extends GenericSempReplyParserListener {
        
        protected RedundancyPollerListener(final PhysicalAppliance appliance) {
            super(appliance);
        }
        
        @Override
        public void onStatMessage(final SingleContainerSet containerSet, Map<String,String> objectValuesMap) {
            // if configured in an HA pair, it is preferred to look at the detailed version of activity... sometimes the non-detailed one gets stuck at Subscriptions Pending during upgrades
            if (PumpConstants.UNINITIALIZED_VALUE.equals(objectValuesMap.get(PRIMARY_ACTIVITY_DETAIL))) {
                physical.setPrimaryLocalActivity(objectValuesMap.get(PRIMARY_ACTIVITY));
            } else {
                physical.setPrimaryLocalActivity(objectValuesMap.get(PRIMARY_ACTIVITY_DETAIL));
            }
            if (PumpConstants.UNINITIALIZED_VALUE.equals(objectValuesMap.get(BACKUP_ACTIVITY_DETAIL))) {
                physical.setBackupLocalActivity(objectValuesMap.get(BACKUP_ACTIVITY));
            } else {
                physical.setBackupLocalActivity(objectValuesMap.get(BACKUP_ACTIVITY_DETAIL));
            }
            physical.setRedundancyMode(objectValuesMap.get(REDUNDANCY_MODE));
            super.onStatMessage(containerSet,objectValuesMap);
        }
    }
}


/*
NON-detail version:

<rpc-reply semp-version="soltr/7_1">
  <rpc>
    <show>
      <redundancy>
        <config-status>Enabled</config-status>
        <redundancy-status>Up</redundancy-status>
        <auto-revert>false</auto-revert>
        <redundancy-mode>Active/Active</redundancy-mode>
        <mate-router-name>emea2</mate-router-name>
        <oper-status>
          <adb-link-up>true</adb-link-up>
          <adb-hello-up>true</adb-hello-up>
        </oper-status>
        <virtual-routers>
          <primary>
            <config>
              <routing-interface>1/1/lag1:1</routing-interface>
              <vrrp-vrid>60</vrrp-vrid>
            </config>
            <status>
              <activity>Local Active</activity>
              <vrrp>Master</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>250</vrrp-priority>
            </status>
          </primary>
          <backup>
            <config>
              <routing-interface>1/1/lag1:2</routing-interface>
              <vrrp-vrid>90</vrrp-vrid>
            </config>
            <status>
              <activity>Mate Active</activity>
              <vrrp>Backup</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>100</vrrp-priority>
            </status>
          </backup>
        </virtual-routers>
      </redundancy>
    </show>
  </rpc>
<execute-result code="ok"/>
</rpc-reply>





(PRIMARY)
<rpc-reply semp-version="soltr/7_0">
  <rpc>
    <show>
      <redundancy>
        <config-status>Enabled</config-status>
        <redundancy-status>Up</redundancy-status>
        <auto-revert>false</auto-revert>
        <redundancy-mode>Active/Active</redundancy-mode>
        <mate-router-name>emea2</mate-router-name>
        <oper-status>
          <adb-link-up>true</adb-link-up>
          <adb-hello-up>true</adb-hello-up>
        </oper-status>
        <virtual-routers>
          <primary>
            <config>
              <routing-interface>1/1/lag1:1</routing-interface>
              <vrrp-vrid>60</vrrp-vrid>
            </config>
            <status>
              <activity>Local Active</activity>   <---------------------------------------
              <vrrp>Master</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>250</vrrp-priority>
              <detail>
                <priority-reported-by-mate>
                  <summary>Standby</summary>
                  <cspf>Standby</cspf>
                  <adb-hello>Active</adb-hello>
                  <vrrp>None (-1)</vrrp>
                </priority-reported-by-mate>
                <activity-status>
                  <summary>Local Active</summary>   <---------------------------------
                  <operational-status>
                    <summary>Ready</summary>
                    <redundancy-config>Enabled</redundancy-config>
                    <message-spool>Ready</message-spool>
                  </operational-status>
                  <smrp-status>
                    <summary>Ready</summary>
                    <db-build>Ready</db-build>
                    <db-build-percentage>100</db-build-percentage>
                    <db-sync>Ready</db-sync>
                  </smrp-status>
                  <internal>
                    <priority>Active</priority>
                    <activity>Local Active</activity>       <--------------------------------
                    <redundancy>Pri-Active</redundancy>
                  </internal>
                </activity-status>
                <message-spool-status>
                  <summary>Ready</summary>
                  <message-spool-config>Enabled</message-spool-config>
                  <status-if-enabled>
                    <cvrid-config-ready>true</cvrid-config-ready>
                    <adm-card-ready>true</adm-card-ready>
                    <flash-module-ready>true</flash-module-ready>
                    <power-module-ready>true</power-module-ready>
                    <adm-contents-ready>true</adm-contents-ready>
                    <local-contents-key>192.168.2.60:35,28</local-contents-key>
                    <mate-contents-key>192.168.2.60:35,28</mate-contents-key>
                    <router-schema-match>true</router-schema-match>
                    <disk-ready>true</disk-ready>
                    <disk-contents>Ready</disk-contents>
                    <disk-key-primary>192.168.2.60:35,28</disk-key-primary>
                    <disk-key-backup>192.168.2.60:34,28</disk-key-backup>
                    <adm-datapath-ready>true</adm-datapath-ready>
                  </status-if-enabled>
                  <internal>
                    <redundancy>AD-Active</redundancy>     <---------------------------------------
                  </internal>
                </message-spool-status>
                <last-update-status>
                  <code>Ok</code>
                  <subscriber></subscriber>
                  <reason></reason>
                </last-update-status>
              </detail>
            </status>
          </primary>
          <backup>
            <config>
              <routing-interface>1/1/lag1:2</routing-interface>
              <vrrp-vrid>90</vrrp-vrid>
            </config>
            <status>
              <activity>Mate Active</activity>
              <vrrp>Backup</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>100</vrrp-priority>
              <detail>
                <priority-reported-by-mate>
                  <summary>Active</summary>
                  <cspf>Active</cspf>
                  <adb-hello>Standby</adb-hello>
                  <vrrp>Active (250)</vrrp>
                </priority-reported-by-mate>
                <activity-status>
                  <summary>Mate Active</summary>
                  <operational-status>
                    <summary>Ready</summary>
                    <redundancy-config>Enabled</redundancy-config>
                    <message-spool>Ready</message-spool>
                  </operational-status>
                  <smrp-status>
                    <summary>Ready</summary>
                    <db-build>Ready</db-build>
                    <db-build-percentage>100</db-build-percentage>
                    <db-sync>Ready</db-sync>
                  </smrp-status>
                  <internal>
                    <priority>Standby</priority>
                    <activity>Mate Active</activity>
                    <redundancy>Bkup-Standby</redundancy>
                  </internal>
                </activity-status>
                <message-spool-status>
                  <summary>Ready</summary>
                  <message-spool-config>Shutdown</message-spool-config>
                  <internal>
                    <redundancy>AD-Disabled</redundancy>
                  </internal>
                </message-spool-status>
                <last-update-status>
                  <code>Ok</code>
                  <subscriber></subscriber>
                  <reason></reason>
                </last-update-status>
              </detail>
            </status>
          </backup>
        </virtual-routers>
        <mismatch-info>
          <local>
            <primary-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </primary-cvrid>
            <backup-cvrid>
              <ip-address>192.168.2.90</ip-address>
            </backup-cvrid>
            <mate-address>
            </mate-address>
            <ad-enabled-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </ad-enabled-cvrid>
            <disk-wwn>23533656665383964</disk-wwn>
          </local>
          <received-from-mate>
            <primary-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </primary-cvrid>
            <backup-cvrid>
              <ip-address>192.168.2.90</ip-address>
            </backup-cvrid>
            <mate-address>
              <ip-address>192.168.2.91</ip-address>
            </mate-address>
            <ad-enabled-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </ad-enabled-cvrid>
            <disk-wwn>23533656665383964</disk-wwn>
          </received-from-mate>
        </mismatch-info>
      </redundancy>
    </show>
  </rpc>
<execute-result code="ok"/>
</rpc-reply>





(BACKUP)
<rpc-reply semp-version="soltr/7_0">
  <rpc>
    <show>
      <redundancy>
        <config-status>Enabled</config-status>
        <redundancy-status>Up</redundancy-status>
        <auto-revert>false</auto-revert>
        <redundancy-mode>Active/Active</redundancy-mode>
        <mate-router-name>emea1</mate-router-name>
        <oper-status>
          <adb-link-up>true</adb-link-up>
          <adb-hello-up>true</adb-hello-up>
        </oper-status>
        <virtual-routers>
          <primary>
            <config>
              <routing-interface>1/1/lag1:1</routing-interface>
              <vrrp-vrid>90</vrrp-vrid>
            </config>
            <status>
              <activity>Local Active</activity>   <-----------------------------------
              <vrrp>Master</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>250</vrrp-priority>
              <detail>
                <priority-reported-by-mate>
                  <summary>Standby</summary>
                  <cspf>Standby</cspf>
                  <adb-hello>Active</adb-hello>
                  <vrrp>None (-1)</vrrp>
                </priority-reported-by-mate>
                <activity-status>
                  <summary>Local Active</summary>
                  <operational-status>
                    <summary>Ready</summary>
                    <redundancy-config>Enabled</redundancy-config>
                    <message-spool>Ready</message-spool>
                  </operational-status>
                  <smrp-status>
                    <summary>Ready</summary>
                    <db-build>Ready</db-build>
                    <db-build-percentage>100</db-build-percentage>
                    <db-sync>Ready</db-sync>
                  </smrp-status>
                  <internal>
                    <priority>Active</priority>
                    <activity>Local Active</activity>
                    <redundancy>Pri-Active</redundancy>
                  </internal>
                </activity-status>
                <message-spool-status>
                  <summary>Ready</summary>
                  <message-spool-config>Shutdown</message-spool-config>   <--------------------??????
                  <internal>
                    <redundancy>AD-Disabled</redundancy>    <---------------------------------
                  </internal>
                </message-spool-status>
                <last-update-status>
                  <code>Ok</code>
                  <subscriber></subscriber>
                  <reason></reason>
                </last-update-status>
              </detail>
            </status>
          </primary>
          <backup>
            <config>
              <routing-interface>1/1/lag1:2</routing-interface>
              <vrrp-vrid>60</vrrp-vrid>
            </config>
            <status>
              <activity>Mate Active</activity>
              <vrrp>Backup</vrrp>
              <routing-interface>Up</routing-interface>
              <vrrp-priority>100</vrrp-priority>
              <detail>
                <priority-reported-by-mate>
                  <summary>Active</summary>
                  <cspf>Active</cspf>
                  <adb-hello>Standby</adb-hello>
                  <vrrp>Active (250)</vrrp>
                </priority-reported-by-mate>
                <activity-status>
                  <summary>Mate Active</summary>
                  <operational-status>
                    <summary>Ready</summary>
                    <redundancy-config>Enabled</redundancy-config>
                    <message-spool>Ready</message-spool>
                  </operational-status>
                  <smrp-status>
                    <summary>Ready</summary>
                    <db-build>Ready</db-build>
                    <db-build-percentage>100</db-build-percentage>
                    <db-sync>Ready</db-sync>
                  </smrp-status>
                  <internal>
                    <priority>Standby</priority>
                    <activity>Mate Active</activity>
                    <redundancy>Bkup-Standby</redundancy>
                  </internal>
                </activity-status>
                <message-spool-status>
                  <summary>Ready</summary>
                  <message-spool-config>Enabled</message-spool-config>
                  <status-if-enabled>
                    <cvrid-config-ready>true</cvrid-config-ready>
                    <adm-card-ready>true</adm-card-ready>
                    <flash-module-ready>true</flash-module-ready>
                    <power-module-ready>true</power-module-ready>
                    <adm-contents-ready>true</adm-contents-ready>
                    <local-contents-key>192.168.2.60:35,28</local-contents-key>
                    <mate-contents-key>192.168.2.60:35,28</mate-contents-key>
                    <router-schema-match>true</router-schema-match>
                    <disk-ready>true</disk-ready>
                    <disk-contents>Ready</disk-contents>
                    <disk-key-primary>Unknown</disk-key-primary>
                    <disk-key-backup>192.168.2.60:34,28</disk-key-backup>
                    <adm-datapath-ready>true</adm-datapath-ready>
                  </status-if-enabled>
                  <internal>
                    <redundancy>AD-Standby</redundancy>
                  </internal>
                </message-spool-status>
                <last-update-status>
                  <code>Ok</code>
                  <subscriber></subscriber>
                  <reason></reason>
                </last-update-status>
              </detail>
            </status>
          </backup>
        </virtual-routers>
        <mismatch-info>
          <local>
            <primary-cvrid>
              <ip-address>192.168.2.90</ip-address>
            </primary-cvrid>
            <backup-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </backup-cvrid>
            <mate-address>
            </mate-address>
            <ad-enabled-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </ad-enabled-cvrid>
            <disk-wwn>23533656665383964</disk-wwn>
          </local>
          <received-from-mate>
            <primary-cvrid>
              <ip-address>192.168.2.90</ip-address>
            </primary-cvrid>
            <backup-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </backup-cvrid>
            <mate-address>
              <ip-address>192.168.2.61</ip-address>
            </mate-address>
            <ad-enabled-cvrid>
              <ip-address>192.168.2.60</ip-address>
            </ad-enabled-cvrid>
            <disk-wwn>23533656665383964</disk-wwn>
          </received-from-mate>
        </mismatch-info>
      </redundancy>
    </show>
  </rpc>
<execute-result code="ok"/>
</rpc-reply>
*/


