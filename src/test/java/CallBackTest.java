import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Service.CallBackService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallBackTest {

    private SessionFactory sessionFactory;
    private CallBackService callBackService;
    private Map<String, LocalDateTime> sessionIdMap = new HashMap<>();
    private ConfigReader configReader =new ConfigReader();
    @BeforeEach
    public void setup() {
        // Configurar la sesión de Hibernate utilizando el archivo de configuración
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
        callBackService=new CallBackService(configReader);
        // Crear una instancia del repositorio HibernateTaskRepository
        //repository = new HibernateTaskRepository(sessionFactory);
    }

    @Test
    @Order(1)
    public void parseSessionIdFromResponseTest() {
        String xml="<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "       <S:Body>\n" +
                "      <LoginResponse xmlns=\"http://schemas.ericsson.com/cai3g1.2/\">\n" +
                "       <sessionId>1</sessionId>\n" +
                "      <baseSequenceId>1</baseSequenceId>\n" +
                "     </LoginResponse>\n" +
                "      </S:Body>\n" +
                "      </S:Envelope>";
            String result= callBackService.parseSessionIdFromResponse(xml);
            assertEquals("1",result);
    }
    @Test
    @Order(2)
    public void validateSessionId() {
        callBackService.storeSessionId("123");
        String result=callBackService.validateSessionId("123");
        assertEquals("123",result);
    }

    @AfterEach
    public void cleanUp() {
        sessionFactory.close();
    }

}
