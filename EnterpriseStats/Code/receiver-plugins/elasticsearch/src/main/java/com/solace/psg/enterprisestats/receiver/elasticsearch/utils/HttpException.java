package com.solace.psg.enterprisestats.receiver.elasticsearch.utils;

public class HttpException extends Exception {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public HttpException() { super(); }
	  public HttpException(String message) { super(message); }
	  public HttpException(String message, Throwable cause) { super(message, cause); }
	  public HttpException(Throwable cause) { super(cause); }
}