package org.apache.hadoop.metrics2.sink.influxdb;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.configuration.SubsetConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.MetricsTag;
import org.apache.hadoop.metrics2.sink.influxdb.utils.InfluxDBService;

/**
 * Hadoop Metrics Sink for writing to influxdb
 *
 * example configuration:
 *
 * *.sink.influxdb.class=org.apache.hadoop.metrics2.sink.influxdb.InfluxdbSink
 * *.sink.influxdb.url=http://influx.example.com:8086
 * *.sink.influxdb.database=influxdbPassword
 * *.sink.influxdb.username=influxdbUsername
 * *.sink.influxdb.password=influxdbPassword
 * *.sink.influxdb.cluster=hadoop-clustername-identifier
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class InfluxdbSink implements MetricsSink, Closeable {

    private static final Log LOG = LogFactory.getLog(InfluxdbSink.class);

    private SubsetConfiguration conf;
    private String influxdbUsername;
    private String influxdbPassword;
    private String influxdbUrl;
    private String influxdbDatabase;
    private String clusterName;
    private String source;
    
    private boolean validConnection = false;

    private InfluxDBService influxDBService;

    /*
     * (non-Javadoc)
     * @see java.io.Closeable.close()
     */
    @Override
    public void close() throws IOException {
        // Do Nothing
    }

    /*
     * (non-Javadoc)
     * @see org.apache.hadoop.metrics2.MetricsSink.putMetrics(final MetricsRecord metricsRecord)
     */
    @Override
    public void putMetrics(final MetricsRecord metricsRecord) {
        
        if(!validConnection) return;
        
        StringBuilder lines = new StringBuilder();
        LOG.debug("########## Start Put metric ##########");
        // Configure the hierarchical place to display the graph.
        LOG.debug("Going to put metricsRecord context: " + metricsRecord.context() + " with name: " + metricsRecord.name());
        StringBuilder tags = new StringBuilder();

        for (MetricsTag tag : metricsRecord.tags()) {
            String val = tag.value();
            if (val != null && val.length() > 0) {
                val = val.replace(',','|');
                tags.append(tag.name())
                    .append("=")
                    .append(val)
                    .append(",");
            }
        }

        // Add the clustername to the tags String if defined
        tags.append("cluster=").append(this.clusterName);
        
        if(source != null && source.length() > 0)
            tags.append(",source=").append(this.source);

        for (AbstractMetric metric : metricsRecord.metrics()) {

            // Because influx cannot handle -0 floats we are checking and converting them
            // to 0
            float f = metric.value().intValue();
            if (f < 0) {
                f = 0;
            }

            lines.append(metric.name().replace(" ", "_"))
                .append(",")
                .append(tags.toString().trim())
                .append(" ")
                .append("value=")
                .append(f)
                .append(" ")
                .append("\n");
        }
        try {
            if (lines.toString() != null) {
                influxDBService.write(lines.toString());
            }
        }
        catch (IOException e) {
            LOG.error("Exception saving metrics to InfluxdbSink", e);
        }
    }

    /*
    * (non-Javadoc)
    * @see org.apache.hadoop.metrics2.MetricsSink.flush()
    */
    @Override
    public void flush() {
        //DO Nothing
    }

    /*
    * (non-Javadoc)
    * @see org.apache.hadoop.metrics2.MetricsSink.init()
    */
    @Override
    public void init(SubsetConfiguration conf) {
        LOG.info("Initializing InfluxDB connection");
        this.conf = conf;

        try {
            parseConfiguration();
            connect();
        }
        catch (Exception e) {
            LOG.error("Exception init InfluxdbSink", e);
        }
    }

    /**
     * Initiate connection to influxDB.
     *
     * @throws IllegalStateException
     * @throws IOException
     */
    private void connect() throws IllegalStateException, IOException {
        LOG.info("Using URL: " + influxdbUrl);
        influxDBService = new InfluxDBService(influxdbUrl, influxdbDatabase, influxdbUsername, influxdbPassword);
        
        validConnection = influxDBService.testConnection();
    }

    /**
     * Parse the configuration from hadoop-metrics2.properties.
     */
    private void parseConfiguration() {

        // Get InfluxDB host configurations
        influxdbUrl = conf.getString("url");
        LOG.info("InFluxDB influxdb url: " + this.influxdbUrl);

        // Get InFluxDB metrics database
        influxdbDatabase = conf.getString("database");
        LOG.info("InFluxDB database: " + this.influxdbDatabase);

        influxdbUsername = conf.getString("username");
        LOG.info("InFluxDB username: " + this.influxdbUsername);

        this.influxdbPassword = conf.getString("password");
        LOG.info("InfluxDB password is set");

        this.clusterName = conf.getString("cluster", "hadoop");
        LOG.info("Clustername set to: " + this.clusterName);
        
        this.source = conf.getString("source", "unknown");
        LOG.info("Source set to: " + this.source);
    }
}
