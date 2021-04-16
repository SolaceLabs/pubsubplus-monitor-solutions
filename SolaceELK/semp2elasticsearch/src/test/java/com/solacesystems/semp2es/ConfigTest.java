package com.solacesystems.semp2es;

import com.solacesystems.semp2es.Semp2ESConfig;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Created by koverton on 11/29/2014.
 */
public class ConfigTest {
    @Test
    public void alwaysProvideADefaultTest() {
        Properties defaults = Semp2ESConfig.loadConfig("none");

        Field[] fields = Semp2ESConfig.class.getDeclaredFields();
        for ( Field field : fields ) {
            field.setAccessible(true);
            try {
                Assert.assertTrue("Must contain all defaults", defaults.containsKey(field.get(defaults)));
            }
            catch(IllegalAccessException iaex) {
                iaex.printStackTrace();
            }
        }
        defaults.list(System.out);
    }
}
