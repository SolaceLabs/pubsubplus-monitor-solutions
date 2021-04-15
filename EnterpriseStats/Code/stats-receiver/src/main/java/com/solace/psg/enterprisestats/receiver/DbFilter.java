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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbFilter {
    private static final Logger logger = LoggerFactory.getLogger(DbFilter.class);

    Node root;

    class Node {
        String name;
        HashMap<String, Node> children;
        boolean hasWildcard = false;
        boolean hasDescendantWildcard = false;
        boolean isMatchingNode = false;

        private Node(String name) {
            this.name = name;
            children = new HashMap<String, Node>();
        }

        private Node getChild(String name) {
            return children.get(name);
        }

        private Node addChild(String name) {
            Node node = getChild(name);
            if (node == null) {
                if (name.equals(">")) {
                    this.hasDescendantWildcard = true;
                    return this;
                }
                if (name.equals("*")) {
                    this.hasWildcard = true;
                }
                node = new Node(name);
                this.children.put(name, node);
            }
            return node;
        }

    }

    public DbFilter() {
        this.root = new Node("ROOT");
    }

    public void addSubscription(String sub) throws IllegalArgumentException {
        validateSubscription(sub);
        Node node = root;
        List<String> levels = Arrays.asList(sub.split("/"));

        for (String level : levels) {
            node = node.addChild(level);
        }
        node.isMatchingNode = true;
    }

    private void validateSubscription(String sub) throws IllegalArgumentException {
        boolean hasDescendantWildcard = false;
        List<String> levels = Arrays.asList(sub.split("/"));
        for (String level : levels) {
            // Descendant wildcards are only valid at last level
            if (hasDescendantWildcard) {
                throw new IllegalArgumentException("Invalid Subscription: Invalid location for descendant wildcard");
            }

            if (level.isEmpty()) {
                throw new IllegalArgumentException("Invalid Subscription: Empty level");
            }
            if (level != level.trim()) {
                throw new IllegalArgumentException("Invalid Subscription: Contains unexpected whitespace");
            }

            if (level.equals(">")) {
                hasDescendantWildcard = true;
            }
        }
    }

    public boolean lookup(String topic) {
        List<String> levels = Arrays.asList(topic.split("/"));
        return lookup(root, levels);
    }

    private boolean lookup(Node curNode, List<String> topic) {
        if (topic.isEmpty()) {
            logger.error("Topic is unexpectedly empty");
            return false;
        }

        String level = topic.get(0);
        List<String> subTopic = null;
        if (topic.size() > 1) {
            subTopic = topic.subList(1, topic.size());
        }

        if (curNode.hasDescendantWildcard) {
            return true;
        }

        if (curNode.hasWildcard) {
            // Search down wildcard path of the tree
            curNode = curNode.getChild("*");
            if (curNode == null) {
                // Should always exist
                logger.error("Tree is missing wildcard branch.");
                return false;
            }

            if (subTopic == null) {
                // we are at leaf node of topic, we are done
                return curNode.isMatchingNode;
            } else {
                if (lookup(curNode, subTopic))
                    return true;
                // wildcard path doesn't match, continue on looking for
                // non-wildcard match
            }
        }

        curNode = curNode.getChild(level);
        if (curNode == null)
            return false;
        if (subTopic == null) {
            // we are at leaf node of topic, we are done
            return curNode.isMatchingNode;
        } else {
            return lookup(curNode, subTopic);
        }
    }
}
