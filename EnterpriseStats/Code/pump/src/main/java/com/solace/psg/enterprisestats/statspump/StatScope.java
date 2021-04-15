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

package com.solace.psg.enterprisestats.statspump;

/*
 * For VPN-scoped messages, the topic prefix will always be #INFO/<applianceName>/<vpnName/...
 * For per-VPN stats, need to query the appliance once per VPN: e.g. show message-spool message-vpn <blah> rates
 * For Appliance-scoped messages, the topic prefix will be  #INFO/<applinaceName>/...
 */
public enum StatScope {
    VPN,
    PER_VPN,
    SYSTEM,
}
