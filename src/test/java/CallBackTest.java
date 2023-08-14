import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Service.CallBackService;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.time.LocalDateTime;
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
    @Test
    public void sendcurl(){
        String url = "http://localhost:30055/devices/00259E-HG8145X6-485754436015A8A7/tasks?timeout=3000&connection_request";
        String requestBody = "{\"name\": \"download\", \"file\": \"XML_HG8145X6v4.0.xml\"}";

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(requestBody, "UTF-8"));

        try {
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, "UTF-8");

            System.out.println("Response Code: " + response.getStatusLine().getStatusCode());
            System.out.println("Response Body: " + responseBody);

        } catch (IOException e) {
            System.err.println("Error al realizar la solicitud HTTP: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void cleanUp() {
        sessionFactory.close();
    }

}
