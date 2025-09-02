package org.example;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    // Load properties from classpath config.properties
    public static Properties load() throws Exception {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) props.load(in);
        }
        return props;
    }
}
