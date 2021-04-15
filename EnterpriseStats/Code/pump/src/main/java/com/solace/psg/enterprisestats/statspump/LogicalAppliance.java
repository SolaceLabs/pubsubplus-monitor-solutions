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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.config.xml.DestinationType;
import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.pollers.Poller;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

public class LogicalAppliance {
    private static final Logger logger = LoggerFactory.getLogger(LogicalAppliance.class);

    public enum Type {

        STANDALONE(StatsPumpConstants.REDUNDANCY_STANDALONE), ACTIVE_STANDBY(
                StatsPumpConstants.REDUNDANCY_ACTIVE_STANDBY), ACTIVE_ACTIVE(
                        StatsPumpConstants.REDUNDANCY_ACTIVE_ACTIVE);

        final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Now for the cluster details /////////////////////////////////////////
    private final String name;
    private Type type = Type.STANDALONE;
    private PhysicalAppliance primary; // primaty/backup vs. active/standby
    private PhysicalAppliance backup;
    private final Map<Poller, Float> pollerIntervalMap = new LinkedHashMap<Poller, Float>();
    private final Map<Poller, ScheduledPollerHandle> pollerHandleMap = new LinkedHashMap<Poller, ScheduledPollerHandle>();
    private final Set<MessageBus> mgmtMsgBusSet = new LinkedHashSet<MessageBus>();
    private final Set<MessageBus> selfMsgBusSet = new LinkedHashSet<MessageBus>();
    private final Set<MessageBus> localMgmtMsgBusSet = new LinkedHashSet<MessageBus>();

    public LogicalAppliance(String name) {
        this.name = name;
    }

    public void addPrimaryPhysicalAppliance(PhysicalAppliance primary) {
        this.primary = primary;
        logger.info("Adding primary physical appliance " + primary + " to " + this);
    }

    public void addBackupPhysicalAppliance(PhysicalAppliance backup, Type type) {
        this.backup = backup;
        if (backup != null) {
            logger.info("Adding backup physical appliance " + backup + " to " + this);
            assert type != Type.STANDALONE;
        } else {
            logger.info("Removing backup physical appliance from " + this);
            assert type == Type.STANDALONE;
        }
        this.type = type;
    }

    public void addMessageBus(MessageBus msgBus) {
        logger.info("Adding " + msgBus + " to " + this);
        if (msgBus.getType() == MessageBus.Type.MGMT) {
            mgmtMsgBusSet.add(msgBus);
        } else if (msgBus.getType() == MessageBus.Type.LOCAL_MGMT) {
            localMgmtMsgBusSet.add(msgBus);
        	
        } else {
            selfMsgBusSet.add(msgBus);
        }
    }

    public void addPoller(Poller poller, float intervalInSec) {
        pollerIntervalMap.put(poller, intervalInSec);
        logger.info(String.format("Adding %s to %s at interval %.1f sec", poller, toString(), intervalInSec));
    }

    public Set<Poller> getPollers() {
        return pollerIntervalMap.keySet();
    }

    public void addPollerHandle(Poller poller, ScheduledPollerHandle pollerHandle) {
        pollerHandleMap.put(poller, pollerHandle);
        // logger.info(String.format("Adding %s to %s at interval %.1f
        // sec",poller,toString(),intervalInSec));
    }

    public Collection<ScheduledPollerHandle> getPollerHandles() {
        return pollerHandleMap.values();
    }

    @Override
    public String toString() {
        if (isStandalone())
            return String.format("Logical (Standalone) Appliance '%s'", name);
        else
            return String.format("Logical (HA-Pair) Appliance '%s'", name);
    }

    public String getName() {
        return name;
    }

    public boolean isStandalone() {
        return backup == null;
        // or return type = Type.STAND_ALONE;
    }

    public Type getType() {
        return type;
    }

    public PhysicalAppliance getPrimary() {
        return primary;
    }

    public PhysicalAppliance getBackup() {
        return backup;
    }

    private Set<ContainerFactory> getContainerFactories(Set<MessageBus> buses) {
        Set<ContainerFactory> factories = new LinkedHashSet<ContainerFactory>();
        for (MessageBus msgbus : buses) {
            factories.add(msgbus.getContainerFactory());
        }
        return factories;
    }
    private Set<ContainerFactory> getMgmtContainerFactories() {
        return getContainerFactories(mgmtMsgBusSet);
    }
    private Set<ContainerFactory> getLocalContainerFactories() {
        return getContainerFactories(localMgmtMsgBusSet);
    }
    private Set<ContainerFactory> getSelfContainerFactories() {
        return getContainerFactories(selfMsgBusSet);
    }

    private Set<MessageBus> getMgmtMsgBuses() {
    	Set<MessageBus> allMsgBuses = new LinkedHashSet<MessageBus>(mgmtMsgBusSet);
        allMsgBuses.addAll(localMgmtMsgBusSet);
        return Collections.unmodifiableSet(allMsgBuses);
    }

    private Set<MessageBus> getSelfMsgBuses() {
        return Collections.unmodifiableSet(selfMsgBusSet);
    }

    private Set<ContainerFactory> getAllContainerFactories() {
        Set<ContainerFactory> allFactories = getMgmtContainerFactories();
        allFactories.addAll(getSelfContainerFactories());
        allFactories.addAll(getLocalContainerFactories());
        return allFactories;
    }

    private Set<MessageBus> getAllMsgBuses() {
        Set<MessageBus> allMsgBuses = new LinkedHashSet<MessageBus>(mgmtMsgBusSet);
        allMsgBuses.addAll(selfMsgBusSet);
        allMsgBuses.addAll(localMgmtMsgBusSet);
        return Collections.unmodifiableSet(allMsgBuses);
    }

    public Set<ContainerFactory> getContainerFactories(DestinationType destination) {
        switch (destination) {
        case MGMT:
        	// The logic of the local bus is such that it is, in fact, a management bus.. hence we have to add
        	// in the local factories here.
        	Set<ContainerFactory> retSet = getMgmtContainerFactories();
        	retSet.addAll(getLocalContainerFactories());
            return retSet; 
            
        case SELF:
            return getSelfContainerFactories();
        case BOTH:
            //return getBothContainerFactories();
            return getAllContainerFactories();
        default:
            logger.warn(
                    "A destination type has been passed to getContainerFactories that doesn't exist! " + destination);
            return getAllContainerFactories();
        }
    }

    public Set<MessageBus> getMsgBuses(DestinationType destination) {
        switch (destination) {
        case MGMT:
            return getMgmtMsgBuses();
        case SELF:
            return getSelfMsgBuses();
        case BOTH:
        	// "both" has been stretched. Now that we have a local bus type, there are 3 types
        	// and hence both now means ALL
            return getAllMsgBuses();
        default:
            logger.warn("A destination type has been passed to getMsgBuses that doesn't exist! " + destination);
            return getAllMsgBuses();
        }
    }

    // TODO finish -- the message bus should decide that it wants to be grouped.
    public boolean hasGroupedBuses() {
        return false;
    }

    /**
     * Returns true if this LogicalAppliance has a configured management
     * MessageBus with the same virtual router name, and the same container
     * factory.
     * 
     * @param selfMsgBus
     * @return
     */
    public boolean hasMatchingManagementMsgBus(MessageBus selfMsgBus) {
        assert selfMsgBus.isSelfMsgBus();
        for (MessageBus mgmtMsgBus : getMgmtMsgBuses()) {
            if (selfMsgBus.getVirtualRouterName().equals(mgmtMsgBus.getVirtualRouterName())
                    && selfMsgBus.getContainerFactory().equals(mgmtMsgBus.getContainerFactory())) {
                return true;
            }
        }
        return false;
    }

    public float getPollerInterval(Poller poller) {
        return pollerIntervalMap.get(poller);
    }

}
