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

package com.solace.psg.enterprisestats.statspump.containers.factories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.containers.SingleContainer;
import com.solace.psg.enterprisestats.statspump.tools.semp.XsdDataType;

public abstract class AbstractSingleContainer implements SingleContainer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleContainer.class);
    

    /**
     * This bad boy is called at the beginning of the two put() methods.
     * It's also sometimes called by startElement() in the Containers.
     * @param element
     */
    protected abstract void startMultiple(String element);
    
    @Override
    public void put(XsdDataType type, String element, Object value, boolean isMultiple) {
    	
    	// testing the fix:
    	if (element.strip().toLowerCase().equals("total-egress-discards")) {
    		value = "18446744073693113511";
            logger.warn("Element '" + element + "' is being forced to an out of bounds value for testing purposes.");
    	}
    	
        if (isMultiple) {
            startMultiple(element);
        }
        try {
            switch (type) {
            case STRING:
                putString(element,value.toString(),isMultiple);
                break;
            case LONG:
            case UNSIGNEDLONG:
            case UNSIGNEDINT:
            case INTEGER:             // unbounded integer type, could be larger than Long, so fingers crossed
            case NONNEGATIVEINTEGER:  // unbounded integer type, could be larger than Long, so fingers crossed
                putLong(element,(Long)value,isMultiple);
                break;
            case INT:
            case UNSIGNEDSHORT:
                putInt(element,(Integer)value,isMultiple);
                break;
            case SHORT:
            case UNSIGNEDBYTE:
                putShort(element,(Short)value,isMultiple);
                break;
            case BYTE:
                putByte(element,(Byte)value,isMultiple);
                break;
            case BOOLEAN:
                putBoolean(element,(Boolean)value,isMultiple);
                break;
            case FLOAT:
                putFloat(element,(Float)value,isMultiple);
                break;
            case DOUBLE:
            case DECIMAL:   // unbounded decimal type, could be larger than Double, so fingers crossed
                putDouble(element,(Double)value,isMultiple);
                break;
            case DATE:
            case TIME:
            case DATETIME:
                putString(element,value.toString(),isMultiple);
                break;
            case UNKNOWN:
            default:
                String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
                logger.info("Unknown element type in schema parsing! "+currentState);
                putString(element,value.toString(),isMultiple);
                break;
            }
        } catch (NumberFormatException e) {
            logger.warn("Element '" + element + "' cannot be converted to a long integer from it's value of '" + value + "'. Returning 0 as a placeholder");
            try {
				putLong(element,0L,isMultiple);
			} catch (Exception e1) {
	            logger.error("Unable to save 0ed item: element '" + element + "' into map.");
			}
        } 
		catch (ClassCastException e) {
            String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
            logger.error(currentState,e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
            logger.error(currentState,e);
        }
    }

    @Override
    public void putString(XsdDataType type, String element, String value, boolean isMultiple) {
        if (isMultiple) {
            startMultiple(element);
        }
        try {
            switch (type) {
            case STRING:
                putString(element,value,isMultiple);
                break;
            case LONG:
            case UNSIGNEDLONG:
            case UNSIGNEDINT:
            case INTEGER:             // unbounded integer type, could be larger than Long, so fingers crossed
            case NONNEGATIVEINTEGER:  // unbounded integer type, could be larger than Long, so fingers crossed
                putLong(element,Long.parseLong(value),isMultiple);
                break;
            case INT:
            case UNSIGNEDSHORT:
                putInt(element,Integer.parseInt(value),isMultiple);
                break;
            case SHORT:
            case UNSIGNEDBYTE:
                putShort(element,Short.parseShort(value),isMultiple);
                break;
            case BYTE:
                putByte(element,Byte.parseByte(value),isMultiple);
                break;
            case BOOLEAN:
                putBoolean(element,Boolean.parseBoolean(value),isMultiple);
                break;
            case FLOAT:
                putFloat(element,Float.parseFloat(value),isMultiple);
                break;
            case DOUBLE:
            case DECIMAL:   // unbounded decimal type, could be larger than Double, so fingers crossed
                putDouble(element,Double.parseDouble(value),isMultiple);
                break;
            case DATE:
            case TIME:
            case DATETIME:
                putString(element,value,isMultiple);
                break;
            case UNKNOWN:
            default:
                String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
                logger.info("Unknown element type in schema parsing! "+currentState);
                putString(element,value,isMultiple);
                break;
            }
        } catch (NumberFormatException e) {
            String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
            logger.error(currentState,e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            String currentState = String.format("Element='%s', Type='%s', Value='%s'%n%s",element,type,value,this.toString());
            logger.error(currentState,e);
        }
    }

    protected abstract void putLong(String element, Long value, boolean isMultiple) throws Exception;
    protected abstract void putInt(String element, Integer value, boolean isMultiple) throws Exception;
    protected abstract void putShort(String element, Short value, boolean isMultiple) throws Exception;
    protected abstract void putByte(String element, Byte value, boolean isMultiple) throws Exception;
    protected abstract void putBoolean(String element, Boolean value, boolean isMultiple) throws Exception;
    protected abstract void putFloat(String element, Float value, boolean isMultiple) throws Exception;
    protected abstract void putDouble(String element, Double value, boolean isMultiple) throws Exception;
    protected abstract void putString(String element, String value, boolean isMultiple) throws Exception;
}
