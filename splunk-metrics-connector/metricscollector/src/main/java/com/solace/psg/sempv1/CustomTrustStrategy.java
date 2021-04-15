package com.solace.psg.sempv1;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.TrustStrategy;


public class CustomTrustStrategy implements TrustStrategy 
{
	    public static final CustomTrustStrategy INSTANCE = new CustomTrustStrategy();

	    @Override
	    public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException 
	    {
	        return false;
	    }
}
