package org.prongpa.Models;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ConfigReader {
    private int timeout;
    private int maxThreads;
    private String apiAcsUrl;
    private String apiAcsPort;
    private int maxRetries;
    private int waitProcess;
    private String urlCallBack;
    private String userCallBack;
    private String passwordCallBack;
    private String urlSession;
    private String userSession;
    private String passwordSession;
    private int sessionIdTime;
    public boolean loadConfig() {
        Properties properties = new Properties();
        try {
            log.info("Cargando Configuracion");
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("config.properties");
            properties.load(inputStream);
            inputStream.close();
            // Leer los valores del archivo de configuración
            timeout = Integer.parseInt(properties.getProperty("timeout")) * 60 * 1000;
            maxThreads = Integer.parseInt(properties.getProperty("maxThreads"));
            maxRetries = Integer.parseInt(properties.getProperty("maxRetries"));
            apiAcsUrl = properties.getProperty("APIACS.url");
            apiAcsPort = properties.getProperty("APIACS.port");
            waitProcess = Integer.parseInt(properties.getProperty("waitProcess"))* 60 * 1000;
            urlCallBack=properties.getProperty("URL_CALLBACK");
            userCallBack=properties.getProperty("USER_CALLBACK");
            passwordCallBack=properties.getProperty("PASSWORD_CALLBACK");
            sessionIdTime = Integer.parseInt(properties.getProperty("SESSION_ID_TIME"));
            urlSession = properties.getProperty("URL_SESSION_ID");
            userSession=properties.getProperty("USER_SESSION");
            passwordSession=properties.getProperty("PASSWORD_SESSION");
            return true;
        } catch (IOException e) {
            log.error("Error Al leer Configuracion MENSAJE: "+ e.getMessage());
            return false;
        }
    }
}
