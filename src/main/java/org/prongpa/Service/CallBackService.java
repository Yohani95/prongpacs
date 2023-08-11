package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.prongpa.Models.ConfigReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
@Slf4j
public class CallBackService {
    private Map<String, LocalDateTime> sessionIdMap = new HashMap<>();
    private ConfigReader config;
    public CallBackService(ConfigReader configReader){
        this.config=configReader;
    }
    public  void ExecuteCallBack( String processId){
        try{
            LocalDateTime currentTime = LocalDateTime.now();
            String sessionId = getSessionIdOrGenerate(currentTime);
            if (sessionId != null) {
                sendGenieCallBack(sessionId, processId, "0");
            }
        }catch (Exception e){
            log.info("Error al Ejecutar ExecuteCallBack :"+e.getMessage());
        }
    }
    private String getSessionIdOrGenerate(LocalDateTime currentTime) {
        String existingSessionId = null;
        for (Map.Entry<String, LocalDateTime> entry : sessionIdMap.entrySet()) {
            existingSessionId = entry.getKey();
            LocalDateTime sessionTime = entry.getValue();

            LocalDateTime thirtyMinutesAgo = currentTime.minusMinutes(30);
            if (sessionTime.isAfter(thirtyMinutesAgo)) {
                log.info("Aun Es valido el SessionID");
                // Si el tiempo de la sesión actual es menor a 30 minutos atrás, se reutiliza la sesión
                return existingSessionId;
            }
        }

        // Si no existe una sesión válida o ha pasado más de 30 minutos, se crea una nueva
        String newSessionId = createSessionId();
        sessionIdMap.put(newSessionId, currentTime);
        return newSessionId;
    }
    public HttpResponse<String> doSoapRequest(String url, String soapBody) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                    .build();

            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Error al realizar la solicitud SOAP: " + e.getMessage());
            return null;
        }
    }
    public String createSessionId() {
        log.info("Creando SessionId", "managerSession");
        try {
            // Define el cuerpo de la solicitud SOAP
            String soapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cai3=\"http://schemas.ericsson.com/cai3g1.2/\">\n"
                    + "  <soapenv:Header/>\n"
                    + "  <soapenv:Body>\n"
                    + "    <cai3:Login>\n"
                    + "      <cai3:userId>" + this.config.getUserSession() + "</cai3:userId>\n" // agregar user
                    + "      <cai3:pwd>" + this.config.getPasswordSession() + "</cai3:pwd>\n"
                    + "    </cai3:Login>\n"
                    + "  </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            // Realiza la solicitud SOAP utilizando el método doSoapRequest
            HttpResponse<String> response = doSoapRequest(this.config.getUrlSession(), soapBody);

            // Verifica la respuesta y maneja el sessionId si es válido
            if (response != null && response.statusCode() == 200) {
                String responseBody = response.body();
                String sessionId = parseSessionIdFromResponse(responseBody); // Método para extraer el sessionId de la respuesta SOAP
                if (sessionId != null) {
                    storeSessionId(sessionId); // Método para almacenar el sessionId en el HashMap
                    return sessionId;
                }
            }
        }catch (Exception e){
            log.error("Error al crear sessionID "+e.getMessage());
        }
        return null;
    }
    public void storeSessionId(String sessionId) {
        LocalDateTime currentTime = LocalDateTime.now();
        sessionIdMap.put(sessionId, currentTime);
    }

    public String validateSessionId(String sessionId) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime sessionTime = sessionIdMap.get(sessionId);

        if (sessionTime != null) {
            long minutesElapsed = ChronoUnit.MINUTES.between(sessionTime, currentTime);

            if (minutesElapsed < config.getSessionIdTime()) {
                log.info("SessionId aún válida");
                return sessionId;
            } else {
                log.info("SessionId caducada");
                sessionId=createSessionId();
                sessionIdMap.remove(sessionId);
                return sessionId;
            }
        } else {
            log.info("SessionId no encontrado");
        }

        return null;
    }
    public Map<String, Object> sendGenieCallBack(String sessionId, String processId, String codigoError) {
        Map<String, Object> response = new HashMap<>();
        //TEST para probar las funciones
        response.put("status", 200);
        response.put("msje", "sendgeniecallback");

        try {
            LocalDateTime timestamp = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

            Map<String, Object> params = new HashMap<>();
            params.put("SessionId", sessionId);
            params.put("actionId", "actionId");
            params.put("PROCESS_ID", processId);
            params.put("CODIGO_ERROR", codigoError);
            params.put("DESCRIPCION_ERROR", "error");
            params.put("DETALLE_ERROR", "detalle del error");
            params.put("TIMESTAMP", timestamp.format(formatter));

            // Construye el cuerpo de la solicitud SOAP
            String soapBody = buildSoapRequestBody(params);

            // Realiza la solicitud SOAP utilizando la función genérica doSoapRequest
            HttpResponse<String> soapResponse = doSoapRequest(this.config.getUrlCallBack(), soapBody);
            String code=extractTagValue(soapResponse.body(),"errorcode");
            if(code.equals("1097")){
                log.info("["+processId+"]Error al enviar Notificacion, codeRequest: "+soapResponse.statusCode()+", codeError: "+extractTagValue(soapResponse.body(),"errorcode")+", Detalles: " +extractTagValue(soapResponse.body(),"errormessage"));
            } else if (code.equals("1010")) {
                log.info("["+processId+"]Error al enviar Notificacion, codeRequest: "+soapResponse.statusCode()+", codeError: "+extractTagValue(soapResponse.body(),"errorcode") +", Detalles: " +extractTagValue(soapResponse.body(),"errormessage"));
            }else{
                log.info("["+processId+"]Notificacion enviada con exito codeRequest: " +soapResponse.statusCode());
            }
        } catch (Exception e) {
            log.info("["+processId+"]Error al en la funcion Callback, mensaje: "+e.getMessage());
        }
        return response;
    }

    // Método para extraer el sessionId de la respuesta SOAP
    public String parseSessionIdFromResponse(String responseXml) {
        // Define el patrón de expresión regular para buscar el sessionId
        String pattern = "<sessionId>(.*?)</sessionId>";

        // Crea el objeto Pattern y Matcher para buscar el patrón en la respuesta XML
        Pattern sessionIdPattern = Pattern.compile(pattern);
        Matcher matcher = sessionIdPattern.matcher(responseXml);

        // Encuentra la primera coincidencia y extrae el sessionId
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    private String buildSoapRequestBody(Map<String, Object> params) {
        String soapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:cai3=\"http://schemas.ericsson.com/cai3g1.2/\" xmlns:ent=\"http://schemas.ericsson.com/ma/CA/EntelB2CFTTHMultiRed/\">\n" +
                "   <soapenv:Header>\n" +
                "      <cai3:SessionId>"+params.get("SessionId")+"</cai3:SessionId>\n" +
                "   </soapenv:Header>\n" +
                "   <soapenv:Body>\n" +
                "      <cai3:Set>\n" +
                "         <cai3:MOType>EntelB2CFTTHMultiRed@http://schemas.ericsson.com/ma/CA/EntelB2CFTTHMultiRed/</cai3:MOType>\n" +
                "         <cai3:MOAttributes>\n" +
                "            <ent:SetFTTHSubscriber>\n" +
                "               <ent:actionId>?</ent:actionId>\n" +
                "               <!--Optional:-->\n" +
                "               <ent:GenieCallBack>\n" +
                "                  <ent:PROCESS_ID>"+ params.get("PROCESS_ID") +"</ent:PROCESS_ID>\n" +
                "                  <ent:CODIGO_ERROR>"+params.get("CODIGO_ERROR")+"</ent:CODIGO_ERROR>\n" +
                "                  <ent:DESCRIPCION_ERROR>"+params.get("DESCRIPCION_ERROR")+"</ent:DESCRIPCION_ERROR>\n" +
                "                  <ent:DETALLE_ERROR>"+params.get("DETALLE_ERROR")+"</ent:DETALLE_ERROR>\n" +
                "                  <ent:TIMESTAMP>" + params.get("TIMESTAMP") + "</ent:TIMESTAMP>\n" +
                "               </ent:GenieCallBack>\n" +
                "            </ent:SetFTTHSubscriber>\n" +
                "         </cai3:MOAttributes>\n" +
                "      </cai3:Set>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        return soapBody;
    }
    public String extractTagValue(String soapResponse, String tagName) {
        // Define el patrón de expresión regular para buscar el tag especificado
        String pattern = "<" + tagName + ">(.*?)</" + tagName + ">";

        // Crea el objeto Pattern y Matcher para buscar el patrón en la respuesta XML
        Pattern tagPattern = Pattern.compile(pattern);
        Matcher matcher = tagPattern.matcher(soapResponse);

        // Encuentra la primera coincidencia y extrae el valor del tag
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
