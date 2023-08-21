package org.prongpa.Repository.Hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;

@Slf4j
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            File configFile = new File("hibernate.cfg.xml");
            Configuration configuration = new Configuration().configure(configFile);
            if (!configFile.exists()) {
                log.error("El archivo de configuración hibernate.cfg.xml no existe en la ubicación actual.");
                return null;
            }
            //log.info("Conexion exitosa a la base de datos");
            return configuration.buildSessionFactory();
        } catch (Throwable ex) {
            log.error("Error en la Conexion base de datos MENSAJE:"+ex.getMessage());
           return null;
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
