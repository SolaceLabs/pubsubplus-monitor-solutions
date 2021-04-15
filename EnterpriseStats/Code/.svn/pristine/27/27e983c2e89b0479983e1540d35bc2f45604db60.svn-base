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
package com.solace.psg.enterprisestats.receiver;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.solace.psg.enterprisestats.receiver.DbFilter;

/**
 * @author dlangayan
 *
 */
public class DbFilterTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    DbFilter m_filter = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        m_filter = new DbFilter();
        try {
            m_filter.addSubscription("stat/l1_a/l2_a/l3_a");
            m_filter.addSubscription("stat/l1_b/l2_b/l3_b");
            m_filter.addSubscription("stat/l1_b/l2_bb/l3_bb");
            m_filter.addSubscription("stat/l1_c/*");
            m_filter.addSubscription("stat/l1_c/*/l3_c");
            m_filter.addSubscription("stat/l1_d/>");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        m_filter = null;
    }

    /**
     * Test method for
     * {@link com.solace.psg.enterprisestats.receiver.DbFilter#addSubscription(java.lang.String)}.
     */
    @Test
    public void testAddSubscription1() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        DbFilter filter = new DbFilter();
        filter.addSubscription(">/a");
    }

    @Test
    public void testAddSubscription2() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        DbFilter filter = new DbFilter();
        filter.addSubscription("stat/ x");
    }

    @Test
    public void testAddSubscription3() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        DbFilter filter = new DbFilter();
        filter.addSubscription("stat /x");
    }

    @Test
    public void testAddSubscription4() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        DbFilter filter = new DbFilter();
        filter.addSubscription("stat//x");
    }

    @Test
    public void testAddSubscription5() throws IllegalArgumentException {
        thrown.expect(IllegalArgumentException.class);
        DbFilter filter = new DbFilter();
        filter.addSubscription("");
    }

    /**
     * Test method for
     * {@link com.solace.psg.enterprisestats.receiver.DbFilter#lookup(java.lang.String)}.
     */
    @Test
    public void testLookup1() {
        assertTrue(m_filter.lookup("stat/l1_a/l2_a/l3_a"));
    }

    @Test
    public void testLookup2() {
        assertTrue(m_filter.lookup("stat/l1_b/l2_b/l3_b"));
    }

    @Test
    public void testLookup3() {
        assertTrue(m_filter.lookup("stat/l1_b/l2_bb/l3_bb"));
    }

    @Test
    public void testLookup4() {
        // Topic filter does not exist
        assertFalse(m_filter.lookup("stat/l1_a/l2_a"));
    }

    @Test
    public void testLookup5() {
        assertTrue(m_filter.lookup("stat/l1_c/XXXXXX"));
    }

    @Test
    public void testLookup6() {
        assertTrue(m_filter.lookup("stat/l1_c/XXXXXX/l3_c"));
    }

    @Test
    public void testLookup7() {
        assertFalse(m_filter.lookup("stat/l1_c/XXXXXX/YYYY"));
    }

    @Test
    public void testLookup8() {
        assertFalse(m_filter.lookup("stat/l1_c/XXXXXX/l3_c/l4_c"));
    }

    @Test
    public void testLookup9() {
        assertTrue(m_filter.lookup("stat/l1_d/XXXXXX"));
    }

    @Test
    public void testLookup10() {
        assertTrue(m_filter.lookup("stat/l1_d/XXXXXX/YYYYY"));
    }

    @Test
    public void testLookup11() {
        DbFilter filter = new DbFilter();
        try {
            filter.addSubscription(">");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(filter.lookup("stat"));
    }

    @Test
    public void testLookup12() {
        DbFilter filter = new DbFilter();
        try {
            filter.addSubscription(">");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(filter.lookup("stat/x"));
    }

    @Test
    public void testLookup13() {
        DbFilter filter = new DbFilter();
        try {
            filter.addSubscription(">");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        assertTrue(filter.lookup("stat/x/y"));
    }
}
