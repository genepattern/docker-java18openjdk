package org.genepattern.server.executor.lsf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.genepattern.webservice.JobInfo;
import org.hibernate.cfg.Environment;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Configure LSF default and custom properties by reading a JSON properties file, 'lsf_properties.json'.
 * @author pcarr
 */
public class LsfConfigurationJson {
    private static Logger log = Logger.getLogger(LsfConfigurationJson.class);

    private LsfProperties defaultLsfProperties = null;
    private JSONObject lsfJson = null;
    
    public static LsfConfigurationJson loadLsfProperties() {
        LsfConfigurationJson config = new LsfConfigurationJson();
        try {
            config.reloadPropertiesFromFile();
        }
        catch (Exception e) {
            //ignoring, it is logged in the reloadPropertiesFromFile ...
            return new LsfConfigurationJson();            
        }
        return config;
    }
    
    private LsfConfigurationJson() {   
    }

    private LsfConfigurationJson(JSONObject lsfProperties) {
        reloadPropertiesFromJsonObject(lsfProperties);
    }
    
    public void reloadPropertiesFromFile() throws Exception {
        File lsfPropertiesFile = new File(System.getProperty("genepattern.properties"), "lsf_properties.json");
        reloadPropertiesFromFile(lsfPropertiesFile);
    }
    
    public void reloadPropertiesFromFile(File lsfPropertiesFile) throws Exception {
        if (!lsfPropertiesFile.canRead()) {
            throw new Exception("Can't read properties file: "+lsfPropertiesFile.getAbsolutePath());
        }

        JSONObject props = null;
        Reader reader = null;
        try {
            log.info("loading properties file: "+lsfPropertiesFile.getAbsolutePath());
            reader = new FileReader(lsfPropertiesFile);
            props = (JSONObject) JSONValue.parseWithException(reader);
            reloadPropertiesFromJsonObject(props);
        }
        catch (Throwable t) {
            throw new Exception("Error reading properties from file: "+lsfPropertiesFile.getAbsolutePath(), t);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    log.error("Error closing reader for properties file: "+lsfPropertiesFile.getAbsolutePath(), e);
                }
            }
        }
    }

    public void reloadPropertiesFromJsonObject(JSONObject lsfJson) {
        this.lsfJson = lsfJson;
        JSONObject lsfJsonDefaultProps = (JSONObject) lsfJson.get("default.props");
        this.defaultLsfProperties = new LsfProperties();
        updateLsfPropertiesFromJson(defaultLsfProperties, lsfJsonDefaultProps);
    }
    
    private static void updateLsfPropertiesFromJson(LsfProperties lsfProperties, JSONObject jsonObject) {
        for(LsfProperties.Key propertyName : LsfProperties.Key.values()) {
            String key = propertyName.getKey();
            lsfProperties.put(propertyName, (String) jsonObject.get(key));
        }
        lsfProperties.validate();
    }
    
    public Properties getHibernateOptions() {
        Properties hibernateOptions = new Properties();
        addProperty(hibernateOptions, "hibernate.connection.datasource", "java:comp/env/jdbc/db1");
        addProperty(hibernateOptions, Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        addProperty(hibernateOptions, Environment.TRANSACTION_STRATEGY, JDBCTransactionFactory.class.getName());
        addProperty(hibernateOptions, Environment.DEFAULT_SCHEMA, "GENEPATTERN_DEV_01");
        addProperty(hibernateOptions, Environment.DIALECT, "org.genepattern.server.database.PlatformOracle9Dialect");
        return hibernateOptions;
    }
    
    /**
     * Helper method for adding a property defined in the JSON object loaded from the properties file to
     * the given java.util.Properties object.
     * 
     * @param customProps, the properties object to which to put the property
     * @param key
     * @param defaultValue, if non-null, use this value if there is no value set in this#props
     */
    private void addProperty(Properties customProps, String key, String defaultValue) {
        if (lsfJson == null) {
            log.error("props not initialized, ignoring addProperty( "+key+" )");
            if (defaultValue != null) {
                customProps.put(key, defaultValue);
            }
            return;
        }
        Object obj = lsfJson.get(key);
        if (obj != null && obj instanceof String) {
            customProps.put(key, (String) obj);
        }
        else if (defaultValue != null) {
            customProps.put(key, defaultValue);
        }
    }
    
    public String getProperty(String key) {
        return getProperty(key, null);
    }
    
    public String getProperty(String key, String defaultValue) {
        if (lsfJson == null) {
            return defaultValue;
        }
        Object obj = lsfJson.get(key);
        if (obj != null && obj instanceof String) {
            return (String) obj;
        }
        return defaultValue;
    }
    
    /**
     * Get the lsf command line args to use based on the given job.
     * @param jobInfo
     * @return
     */
    public LsfProperties getLsfProperties(JobInfo jobInfo) {
        LsfProperties lsfProperties = initFromDefault();
        
        //customize ...
        JSONObject customProps = (JSONObject) lsfJson.get("custom.props");
        JSONObject extraProps = null;

        String taskName = jobInfo.getTaskName();
        Lsid lsid = new Lsid(jobInfo.getTaskLSID());
        if (customProps.containsKey(jobInfo.getTaskName())) {
            extraProps = (JSONObject) customProps.get(taskName);
            updateLsfPropertiesFromJson(lsfProperties, extraProps);
        }
        else if (customProps.containsKey(lsid.getLsidNoVersion())) {
            extraProps = (JSONObject) customProps.get(lsid.getLsidNoVersion());
            updateLsfPropertiesFromJson(lsfProperties, extraProps);
        }
        else if (customProps.containsKey(lsid.getLsid())) {
            extraProps = (JSONObject) customProps.get(lsid.getLsid());
            updateLsfPropertiesFromJson(lsfProperties, extraProps);
        } 
        return lsfProperties;
    }
    
    private LsfProperties initFromDefault() {
        LsfProperties p = new LsfProperties();
        for(LsfProperties.Key key : LsfProperties.Key.values()) {
            p.put(key, defaultLsfProperties.get(key));
        }
        p.validate();
        return p;
    }

}
