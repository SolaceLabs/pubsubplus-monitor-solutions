/**
 * Copyright 2016-2019 Solace Corporation. All rights reserved.
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
package com.solace.psg.enterprisestats.receiver;

import java.util.HashMap;


public class DbFieldListNonHierarchial {
	
	private HashMap<String, String> m_fieldList;
	
	public DbFieldListNonHierarchial() {
		m_fieldList = new HashMap<String, String>();
	}
	
	public void add(String fieldName) {
		if (m_fieldList.containsKey(fieldName) == false) {
			m_fieldList.put(fieldName, fieldName);
		}
	}
	public boolean contains(String fieldName) {
		return m_fieldList.containsKey(fieldName);
	}
}
