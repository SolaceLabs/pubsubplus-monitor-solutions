package com.solacesystems.solgeneos.sample.util;


public class ConditionalTopic {
	public enum Operator {
		STR_EQUAL,
		STR_NOT_EQUAL,
		INT_EQUAL,
		INT_NOT_EQUAL,
		INT_GREATER_THAN,
		INT_LESS_THAN,
		INVALID;
		
		public static Operator getOperator(String operator) {
			if(operator == null)
				return INVALID;
			else if(operator.equals("STR_EQUAL"))
				return STR_EQUAL;
			else if(operator.equals("STR_NOT_EQUAL"))
				return STR_NOT_EQUAL;
			else if(operator.equals("INT_EQUAL"))
				return INT_EQUAL;
			else if(operator.equals("INT_NOT_EQUAL"))
				return INT_NOT_EQUAL;
			else if(operator.equals("INT_GREATER_THAN"))
				return INT_GREATER_THAN;
			else if(operator.equals("INT_LESS_THAN"))
				return INT_LESS_THAN;
			else {
				System.out.println("unexpected operator: " + operator);
				return INVALID;
			}
		}
		
		public boolean isIntegerOperand() {
			switch (this) {
			case INT_EQUAL:
			case INT_NOT_EQUAL:
			case INT_GREATER_THAN:
			case INT_LESS_THAN:
				return true;
			default:
				return false;
			}
		}
		
		public boolean isStringOperand() {
			switch (this) {
			case STR_EQUAL:
			case STR_NOT_EQUAL:
				return true;
			default:
				return false;
			}
		}
	};
	
	// Topic which will resolve to be the left operand
	public String m_topic = null;
	
	public Operator m_operator = Operator.INVALID;
	
	// Right operand
	public int m_integerOperand = 0;
	public String m_stringOperand = "";
	//public double m_doubleOperand = 0;
	
	
		
	private ConditionalTopic(String topic, Operator operator, String stringOperand) {
		m_topic = topic;
		m_operator = operator;
		m_stringOperand = stringOperand;
	}
	
	private ConditionalTopic(String topic, Operator operator, int intOperand)  {
		m_topic = topic;
		m_operator = operator;
		m_integerOperand = intOperand;
	}
	
	public static ConditionalTopic createConditionalTopic(
						String topic, Operator operator, String rightOperand) throws Exception {
		
		if(operator == Operator.INVALID) {
			throw new Exception("Invalid operator: " + operator);
		}
		
		if(operator.isIntegerOperand()) {
			return new ConditionalTopic(topic, operator, Integer.parseInt(rightOperand));
		} else if(operator.isStringOperand()) {
			return new ConditionalTopic(topic, operator, rightOperand);
		} else {
			// Shouldn't reach here
			throw new Exception("Unexpected operator: " + operator);
		}
	}
	
	public boolean execute(int topicValue) throws Exception {
		if(!m_operator.isIntegerOperand()) {
			throw new Exception("Type mismatch on conditional");
		}
		if(m_operator == Operator.INT_EQUAL) {
			return topicValue == m_integerOperand;
		} else if(m_operator == Operator.INT_NOT_EQUAL) {
			return topicValue != m_integerOperand;
		} else if(m_operator == Operator.INT_GREATER_THAN) {
			return topicValue > m_integerOperand;
		} else if(m_operator == Operator.INT_LESS_THAN) {
			return topicValue < m_integerOperand;
		} else {
			// should never get here
			throw new Exception("Unexpected operator: " + m_operator);
		}
	}
	
	public boolean execute(String topicValue) throws Exception {
		if(!m_operator.isStringOperand()) {
			throw new Exception("Type mismatch on conditional");
		}
		if(m_operator == Operator.STR_EQUAL) {
			return topicValue.equals(m_stringOperand);
		} else if(m_operator == Operator.STR_NOT_EQUAL) {
			return !topicValue.equals(m_stringOperand);
		} else {
			// should never get here
			throw new Exception("Unexpected operator: " + m_operator);
		}
	}
	
};