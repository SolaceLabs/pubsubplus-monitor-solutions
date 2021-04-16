package com.solacesystems.solgeneos.sample.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class MetricFilter<T> {
	
	Node<T> root;
	
	static class Node<T> {
		String name;
		HashMap<String, Node<T>> children;
		boolean hasWildcard = false;
		boolean hasDescendantWildcard = false;
		boolean isMatchingNode = false;
		MatchList<T> matchList = new MatchList<T>();
		
		private Node(String name) {
			this.name = name;
			children = new HashMap<String, Node<T>>();
		}
		
		private Node<T> getChild(String name) {
			return children.get(name);
		}
		
		private Node<T> addChild(String name) {
			Node<T> node = getChild(name);
			if(node == null) {
				if(name.equals(">")) {
					this.hasDescendantWildcard = true;
					return this;
				}
				if(name.equals("*")) {
					this.hasWildcard = true;
				}
				node = new Node<T>(name);
				this.children.put(name, node);
			}
			return node;
		}
	}
	
	public static class MatchList<T> {
		private LinkedList<T> m_list = new LinkedList<T>();
		
		public MatchList() {
		}
		
		public MatchList(MatchList<T> matchList) {
			m_list = matchList.getList();
		}
		
		public MatchList(Collection<T> list) {
			m_list.addAll(list);
		}
		
		public boolean isMatch() {
			return !m_list.isEmpty();
		}
		
		public LinkedList<T> getList() {
			return new LinkedList<T>(m_list);
		}
		
		public int size() {
			return m_list.size();
		}
		
		public void add(Collection<T> list) {
			m_list.addAll(list);
		}
		
		public void add(T data) {
			m_list.addLast(data);
		}
		
		public T remove() {
			return m_list.removeFirst();
		}
	}
	
	public MetricFilter() {
		this.root = new Node<T>("ROOT");
	}
	
	public void addSubscription(String sub, T matchData) throws IOException {
		LinkedList<T> matchList = new LinkedList<T>();
		matchList.add(matchData);
		addSubscription(sub, matchList);
	}
	
	public void addSubscription(String sub, Collection<T> matchData) throws IOException {
		validateSubscription(sub);
		if(matchData.isEmpty()) {
			throw new IOException("Match data is empty");
		}
		
		Node<T> node = root;
		List<String> levels = Arrays.asList(sub.split("/"));
		
		for (String level : levels) {
			node = node.addChild(level);
		}
		node.isMatchingNode = true;
		if(node.matchList.isMatch()) {
			node.matchList.add(matchData);
		} else {
			node.matchList = new MatchList<T>(matchData);
		}
	}

	public static void validateSubscription(String sub) throws IOException {
		boolean hasDescendantWildcard = false;
		List<String> levels = Arrays.asList(sub.split("/"));
		for (String level : levels) {
			// Descendant wildcards are only valid at last level
			if(hasDescendantWildcard) {
				throw new IOException("Invalid Subscription: " + sub + " - Invalid location for descendant wildcard");
			}
			
			if(level.isEmpty()) {
				throw new IOException("Invalid Subscription: " + sub + " - Empty level");
			}
			if(level != level.trim()) {
				throw new IOException("Invalid Subscription: " + sub + " - Contains unexpected whitespace");
			}
			
			if(level.equals(">")) {
				hasDescendantWildcard = true;
			}
		}
	}
	
	public MatchList<T> lookup(String topic) {
		List<String> levels = Arrays.asList(topic.split("/"));
		return lookup(root, levels);
	}
	
	private MatchList<T> lookup(Node curNode, List<String> topic) {
		if(topic.isEmpty()) {
			return new MatchList<T>();
		}

		String level = topic.get(0);
		List<String> subTopic = null;
		if(topic.size() > 1) {
			subTopic = topic.subList(1, topic.size());
		}
		
		if(curNode.hasDescendantWildcard) {
			return new MatchList<T>(curNode.matchList);
		}
		
		if(curNode.hasWildcard) {
			// Search down wildcard path of the tree
			curNode = curNode.getChild("*");
			if(curNode == null) {
				// Should always exist
				System.err.println("Tree is missing wildcard branch.");
				return new MatchList<T>();
			}
			
			if(subTopic == null) {
				// we are at leaf node of topic, we are done
				return new MatchList<T>(curNode.matchList);
			} else {
				MatchList<T> matchList = lookup(curNode, subTopic);
				if(matchList.isMatch()) return new MatchList<T>(matchList);
				// wildcard path doesn't match, continue on looking for non-wildcard match
			}
		}
		
		curNode = curNode.getChild(level);
		if(curNode == null) return new MatchList<T>();
		if(subTopic == null) {
			// we are at leaf node of topic, we are done
			return new MatchList<T>(curNode.matchList);
		} else {
			return lookup(curNode, subTopic);
		}
	}
	
	// main exists for testing purposes only
	public static void main(String... args)  {
        boolean failures = false;
        try {
            MetricFilter<Integer> filter = new MetricFilter<Integer>();
            Integer matchId = 0;
            filter.addSubscription("stat/l1_a/l2_a/l3_a", matchId++);
            filter.addSubscription("stat/l1_b/l2_b/l3_b", matchId++);
            filter.addSubscription("stat/l1_b/l2_bb/l3_bb", matchId++);
            filter.addSubscription("stat/l1_c/*", matchId++);
            filter.addSubscription("stat/l1_c/*/l3_c", matchId++);
            filter.addSubscription("stat/l1_d/>", matchId++);
            
            // Test matching behavior
            if(!filter.lookup("stat/l1_a/l2_a/l3_a").isMatch()) {
            	System.out.println("Test 1 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_b/l2_b/l3_b").isMatch()) {
            	System.out.println("Test 1 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_b/l2_bb/l3_bb").isMatch()) {
            	System.out.println("Test 1 failed");
            	failures = true;
            }
            if(filter.lookup("stat/l1_a/l2_a").isMatch()) {
            	System.out.println("Test 2 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_c/XXXXXX").isMatch()) {
            	System.out.println("Test 3 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_c/XXXXXX/l3_c").isMatch()) {
            	System.out.println("Test 4 failed");
            	failures = true;
            }
            if(filter.lookup("stat/l1_c/XXXXXX/YYYY").isMatch()) {
            	System.out.println("Test 5 failed");
            	failures = true;
            }
            if(filter.lookup("stat/l1_c/XXXXXX/l3_c/l4_c").isMatch()) {
            	System.out.println("Test 6 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_d/XXXXXX").isMatch()) {
            	System.out.println("Test 7 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/l1_d/XXXXXX/YYYYY").isMatch()) {
            	System.out.println("Test 8 failed");
            	failures = true;
            }
            
            filter = new MetricFilter();
            filter.addSubscription(">", matchId++);
            
            if(!filter.lookup("stat").isMatch()) {
            	System.out.println("Test 9 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/x").isMatch()) {
            	System.out.println("Test 10 failed");
            	failures = true;
            }
            if(!filter.lookup("stat/x/y").isMatch()) {
            	System.out.println("Test 11 failed");
            	failures = true;
            }
            
            
            // Test invalid subscriptions
            try {
            	filter.addSubscription(">/a", matchId++);
            	System.out.println("Invalid subscription '>/a' was not rejected");
            	failures = true;
            } catch (Exception subE) {
            }
            try {
            	filter.addSubscription("stat/ x", matchId++);
            	System.out.println("Invalid subscription 'stat/ x' was not rejected");
            	failures = true;
            } catch (Exception subE) {
            }
            try {
            	filter.addSubscription("stat /x", matchId++);
            	System.out.println("Invalid subscription 'stat /x' was not rejected");
            	failures = true;
            } catch (Exception subE) {
            }
            try {
            	filter.addSubscription("stat//x", matchId++);
            	System.out.println("Invalid subscription 'stat//x' was not rejected");
            	failures = true;
            } catch (Exception subE) {
            }
            try {
            	filter.addSubscription("", matchId++);
            	System.out.println("Invalid subscription '' was not rejected");
            	failures = true;
            } catch (Exception subE) {
            }
            
            
            if(!failures) {
            	System.out.println("All tests passed.");
            }
        }
        catch (Exception e) {
        	System.out.println("Exception received: " + e);
        	e.printStackTrace();
        }
        
        MatchList<String> match = new MatchList<String>();
        match.add("a");
        match.add("b");
        match.add("c");
        
        LinkedList<String> list = match.getList();
        list.removeFirst();
        
        System.out.println("matchList size: " + match.getList().size() + " list size: " + list.size());
        
    }
}
