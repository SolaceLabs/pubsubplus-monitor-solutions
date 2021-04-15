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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.containers.ContainerFactory;
import com.solace.psg.enterprisestats.statspump.tools.PumpConstants;
import com.solace.psg.enterprisestats.statspump.util.MonitoredLinkBlockingQueue;
import com.solacesystems.jcsmp.Context;
import com.solacesystems.jcsmp.ContextProperties;
import com.solacesystems.jcsmp.JCSMPFactory;

public class MessageBus {
    private static final Logger logger = LoggerFactory.getLogger(MessageBus.class);

    public enum Type {
        MGMT, SELF, LOCAL_MGMT,
    }

    public enum Mode {
        DIRECT, PERSISTENT,
    }

    public static class Builder {
        private LogicalAppliance logical;
        private Type type;
        private Mode mode = Mode.DIRECT;
        private final String hostnameOrIp;
        private String vpn = null;
        private String username = null;
        private String password = null;
        private boolean isCompressed = false;
        private ContainerFactory containerFactory = null;
        private LocalMgmtBusListener localMgmtBusListener = null;
        private Map<String, Object> localMgmtBusListenerConfig = null;
        private Map<MessageBus, MessageBus> constructedMessageBuses = new HashMap<MessageBus, MessageBus>();

        public Builder(LogicalAppliance logical, String hostname) {
            this.logical = logical;
            this.hostnameOrIp = hostname.toLowerCase();
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setVpn(String vpn) {
            this.vpn = vpn;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setCompressed(boolean compressed) {
            this.isCompressed = compressed;
            return this;
        }

        public Builder setContainerFactoryClass(ContainerFactory containerFactory) {
            this.containerFactory = containerFactory;
            return this;
        }

        public Builder setLocalListenerClass(LocalMgmtBusListener localMgmtBusListener) {
            this.localMgmtBusListener = localMgmtBusListener;
            return this;
        }

        public Builder setLocalListenerConfig(Map<String, Object> properties) {
            this.localMgmtBusListenerConfig = properties;
            return this;
        }

        public MessageBus build() {
            MessageBus newGuy = new MessageBus(this);
            // do checks here to see if he's the same as others..!
            if (constructedMessageBuses.keySet().contains(newGuy)) {
                logger.debug(String.format(
                        "This message bus has already been created... returning a reference to that: %s (existing) vs. %s (new)",
                        constructedMessageBuses.get(newGuy), newGuy.toString()));
                return constructedMessageBuses.get(newGuy);
            } else { // first time building it
                constructedMessageBuses.put(newGuy, newGuy);
                return newGuy;
            }
        }
    }

    private final LogicalAppliance logical;
    private final Type type;
    private final String hostnameOrIp;
    private final String vpn;
    private final String username;
    private final String password;
    private boolean isCompressed;
    private Set<Pattern> vpnExceptions = new HashSet<Pattern>();
    private boolean exceptionDefaultAction = true;
    private final ContainerFactory containerFactory;
    private final LocalMgmtBusListener localMgmtBusListener;
    private final Map<String, Object> localMgmtBusListenerConfig;
    private final Mode mode;
    private final boolean isGrouped = false;

    private final MonitoredLinkBlockingQueue<StatsPumpMessage> pumpQueue;
    private final VpnConnectionManager connManager;
    private final Context context;
    private String virtualRouterName = PumpConstants.UNINITIALIZED_VALUE;

    private MessageBus(Builder b) {
        // TODO proper error checking, null checking
        this.logical = b.logical;
        this.type = b.type;
        this.hostnameOrIp = b.hostnameOrIp.toLowerCase();
        if (this.hostnameOrIp == null)
            throw new NullPointerException("Name cannot be null");
        this.vpn = b.vpn;
        this.username = b.username;
        this.password = b.password;
        this.isCompressed = b.isCompressed;
        this.containerFactory = b.containerFactory;
        this.localMgmtBusListener = b.localMgmtBusListener;
        this.localMgmtBusListenerConfig = b.localMgmtBusListenerConfig;
        this.mode = b.mode;
        this.pumpQueue = new MonitoredLinkBlockingQueue<StatsPumpMessage>(getQueueName());
        this.connManager = new VpnConnectionManager(this); // kinda risky
                                                           // referring to
                                                           // 'this' while still
                                                           // in the constructor
        ContextProperties cProps = new ContextProperties();
        cProps.setName(this.hostnameOrIp + " Context");
        context = JCSMPFactory.onlyInstance().createContext(cProps);
    }

    private String getQueueName() {
        return "PumpQueue for " + this.toString();
    }

    public boolean isGrouped() {
        return isGrouped;
    }

    public LogicalAppliance getAppliance() {
        return logical;
    }

    public Context getContext() {
        return context;
    }

    public VpnConnectionManager getConnManager() {
        return connManager;
    }

    public boolean isMgmtMsgBus() {
        return type == Type.MGMT;
    }

    public boolean isSelfMsgBus() {
        return type == Type.SELF;
    }
    
    public boolean isLocalMgmtMsgBus() {
        return type == Type.LOCAL_MGMT;
    }

    public Type getType() {
        return type;
    }

    public String getHostnameOrIp() {
        return hostnameOrIp;
    }

    public String getVpn() {
        return vpn;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public ContainerFactory getContainerFactory() {
        return containerFactory;
    }

    public LocalMgmtBusListener getLocalMgmtBusListener() {
        return localMgmtBusListener;
    }

    public Map<String, Object> getLocalMgmtBusListenerConfig() {
        return localMgmtBusListenerConfig;
    }

    public Mode getMode() {
        return mode;
    }

    public void setExceptionDefaultAction(boolean defaultAction) {
        if (exceptionDefaultAction != defaultAction) { // only change if they're
                                                       // different
            synchronized (this) {
                if (exceptionDefaultAction != defaultAction) { // only change if
                                                               // they're
                                                               // different
                    logger.info(String.format("Changing default action for VPN exceptions from '%s' to '%s' on %s",
                            exceptionDefaultAction ? "ALLOW" : "DISALLOW", defaultAction ? "ALLOW" : "DISALLOW",
                            this.toString()));
                    exceptionDefaultAction = defaultAction;
                }
            }
        }
    }

    public boolean getExceptionDefaultAction() {
        return exceptionDefaultAction;
    }

    public void addVpnException(String vpnPattern) {
        vpnExceptions.add(Pattern.compile(vpnPattern.replaceAll("\\Q*\\E", ".*")));
        logger.info(String.format("Adding VPN exception '%s' on %s", vpnPattern, toString()));
    }

    public boolean matchesException(String vpn) {
        for (Pattern exception : vpnExceptions) {
            if (exception.matcher(vpn).matches()) {
                return true;
            }
        }
        return false;
    }

    public void setVirtualRouterName(String virtualRouterName) {
        if (!this.virtualRouterName.equals(virtualRouterName)) {
            synchronized (this.virtualRouterName) {
                if (!this.virtualRouterName.equals(virtualRouterName)) {
                    logger.info(String.format("Setting virtual router name on %s MessageBus for %s from '%s' to '%s'",
                            type, hostnameOrIp, this.virtualRouterName, virtualRouterName));
                    this.virtualRouterName = virtualRouterName;
                    pumpQueue.setName(getQueueName());
                }
            }
        }
    }

    public String getVirtualRouterName() {
        return virtualRouterName;
    }

    // uses XOR to see whether, given this message buses configuration, a
    // msesage should be published
    // on the VPN that it is intended for
    public boolean isAllowedToPublish(StatsPumpMessage statsMsg) {
        assert statsMsg.getVpn() != null;
        return !(getExceptionDefaultAction() ^ !matchesException(statsMsg.getVpn()));
    }

    public boolean enqueueStatMessage(StatsPumpMessage message) {
        return pumpQueue.offer(message);
    }

    public StatsPumpMessage takeMessage() throws InterruptedException {
        return pumpQueue.take();
    }

    /**
     * Returns true if the hostname/IP and VPN name are the same. Doesn't check
     * any of the other values
     */
    public boolean sameDestination(MessageBus bus) {
        return hostnameOrIp.equals(bus.hostnameOrIp) && vpn.equals(bus.vpn);
    }

    /**
     * This equals() overrides based on the hsotname/IP being text equals, the
     * VPN being the same (either not specified, or specified and equal), the
     * Type being the same (MGMT/SELF), and the connection factory being the
     * same. ** Need to have the VPN exceptions as well!!
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MessageBus))
            return false;
        MessageBus bus = (MessageBus) o;
        return (hostnameOrIp.equals(bus.hostnameOrIp) && vpn == null
                ? (vpnExceptions.equals(bus.vpnExceptions) && exceptionDefaultAction == bus.exceptionDefaultAction)
                : vpn.equals(bus.vpn) &&
                // type.equals(bus.type) && // don't check the type, b/c we need
                // to check if mgmt buses match to self buses
                        containerFactory.getClass().getName().equals(bus.containerFactory.getClass().getName()));
    }

    // TODO make better!
    @Override
    public int hashCode() {
        return (hostnameOrIp + vpn + type + containerFactory.getClass().getName()).hashCode();
    }

    @Override
    public String toString() {
        String msgBusName;
        if (PumpConstants.UNINITIALIZED_VALUE.equals(virtualRouterName)) {
            msgBusName = hostnameOrIp;
        } else {
            msgBusName = virtualRouterName;
        }
        return String.format("%s Message Bus to '%s', VPN='%s', factory='%s'%s", type, // mgmt
                                                                                       // or
                                                                                       // self
                msgBusName, vpn == null ? "<SELF>*" : vpn, containerFactory.getClass().getSimpleName(),
                logical == null ? "" : ", for " + logical);
    }

    public String getShortName() {
        if (!PumpConstants.UNINITIALIZED_VALUE.equals(virtualRouterName)) {
            return virtualRouterName;
        } else {
            return hostnameOrIp;
        }
    }
}
