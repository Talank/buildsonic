package util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class SessionUtil {
    private static volatile SessionUtil sessionUtil;
    private final SessionFactory factory;
    private SessionUtil() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        LogManager logManager = LogManager.getLogManager();
        Logger logger = logManager.getLogger("");
        logger.setLevel(Level.SEVERE); //could be Level.OFF
        factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
    }

    public static SessionUtil getInstance() {
        if(sessionUtil == null) {
            synchronized (SessionUtil.class) {
                if(sessionUtil == null) {
                    sessionUtil = new SessionUtil();
                }
            }
        }
        return sessionUtil;
    }

    public SessionFactory getSessionFactory() {
        return factory;
    }

    public static Session getSession() {
        return getInstance().factory.openSession();
    }

}

