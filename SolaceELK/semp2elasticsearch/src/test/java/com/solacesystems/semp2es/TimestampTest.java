package com.solacesystems.semp2es;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by koverton on 11/27/2014.
 */
public class TimestampTest {
    @Test
    public void createUTCTimestamp() {
        //                                         "2014-11-20T19:13:06.738Z"
        SimpleDateFormat dayfmt = new SimpleDateFormat("yyyy-MM-dd");
        dayfmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat timefmt = new SimpleDateFormat("HH:mm:ss.SSS");
        timefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date d = new Date();
        String ts = dayfmt.format(d) + "T" + timefmt.format(d) + "Z";
        System.out.println(ts);
    }

    @Test
    public void allInOne() {
        SimpleDateFormat tsfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        tsfmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String ts = tsfmt.format(new Date());
        System.out.println(ts);
    }
}
