/**
 * Copyright 2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company.
 * If there are no contracts in place use of this source
 * is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported
 * by Solace unless such support is provided for under
 * an agreement signed between you and Solace.
 */
package com.solace.psg.enterprisestats.receiver.elasticsearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a log file containing http based Elasticsearch statements sent.
 */
public class ElasticsearchLogger {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchLogger.class);
    private String m_filename = null;

    protected static String generateFileName(String base) {
        Date dt = new Date();
        long ticks = dt.getTime();
        return base + "_" + ticks + ".log";
    }

    /**
     * Simple constructor
     *
     * @returns this
     */
    public ElasticsearchLogger() {
        m_filename = generateFileName("ElasticsearchHttp");
        File f = new File(m_filename);
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * Returns the name of the file currently in use.
     *
     * @return the file name
     */
    public String getFileNameUsed() {
        return m_filename;
    }

    /**
     * Low level primitive to write a line into the file
     *
     * @param line
     */
    public void writeLog(String line) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(m_filename, true)); // append
            writer.write(line);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Failure writing Elasticsearch log file:", e);
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failure writing Elasticsearch log:", e);
            }
        }
    }
}
