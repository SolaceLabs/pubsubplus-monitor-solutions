/**
 * Copyright (c) 2016 Dishant Langayan
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Dishant Langayan
 */
package com.solace.psg.enterprisestats.receiver.transport;

import com.solacesystems.jcsmp.BytesXMLMessage;

/**
 * Listener interface to be implemented by Solace subscriber that needs to be
 * notified of events in the {@link SolaceTransport}. All registered listeners
 * are called synchronously by the {@link SolaceTransport} at the occurrence of
 * the event. It expected that implementers of this interface do NOT perform
 * long running tasks in the implementation of this interface.
 */
public interface SolaceTransportListener {
    /**
     * Notifies the listener of the reconnection of an existing disconnected
     * session.
     */
    public void onReconnected();

    /**
     * Notifies the listener that the underlying transport is automatically
     * attempting to reconnect an existing disconnected session.
     * 
     * @return True to continue with the reconnect operation; false to abort the
     *         reconnect operation.
     */
    public boolean onReconnecting();

    /**
     * Notifies the listener that the connection to Solace has been terminated.
     */
    public void onDisconnected();

    /**
     * Notifies the listener that the connection to Solace has been lost.
     */
    public void onConnectionLost();

    /**
     * Notifies the listener that a new message has been received from Solace.
     */
    public void onMessageReceived(BytesXMLMessage message);

    /**
     * Notifies the listener that a message has been confirmed by Solace.
     * 
     * @param token
     *            details of the message that has been confirmed.
     */
    public void onMessageConfirmed(SolaceTransportToken token);
}
