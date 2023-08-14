package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.prongpa.Models.ConfigDBModel;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;
import org.prongpa.Repository.TaskHistory.HibernateTaskHistoryRepository;
import org.prongpa.Repository.TaskHistory.TaskHistoryRepository;

import java.util.*;

import static org.prongpa.Repository.Hibernate.HibernateUtil.getSessionFactory;

@Slf4j
public class TaskExecuteService implements Runnable{
    private Boolean status;
    private ConfigReader configReader;
    private SessionFactory sessionFactory;
    private List<TaskModel> taskModelList;
    private TaskRepository taskRepository;
    private TaskService taskService;
    private TaskHistoryService taskHistoryService;
    private TaskHistoryRepository taskHistoryRepository;
    private String processId;
    private ConfigDBModel configDBModel;
    CallBackService  callBackService;
    public TaskExecuteService(List<TaskModel> taskModellist, ConfigReader configReader, String processId) {
        this.taskModelList = taskModellist;
        this.configReader = configReader;
        this.processId = processId;
        configure();
    }
    public void configure(){
        try {
            //Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            sessionFactory = getSessionFactory();
            // Crear una instancia del repositorio HibernateTaskRepository
            taskRepository = new HibernateTaskRepository(sessionFactory);
            taskService=new TaskService(taskRepository);
            taskHistoryRepository =new HibernateTaskHistoryRepository(sessionFactory);
            taskHistoryService=new TaskHistoryService(taskHistoryRepository);
            start();
        }catch (Exception e){
            log.error("Error a cargar configuracion del subhilo Mensaje :"+e.getMessage());
            stop();
        }
    }
    public HttpResponse doCurl(String url, String requestBody) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(requestBody, "UTF-8"));

        try {
            HttpResponse response = httpClient.execute(httpPost);
            return response;
        } catch (Exception e) {
            System.err.println("Error al realizar la solicitud HTTP: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public HttpResponse doHttpGet(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Content-Type", "application/json");

        try {
            HttpResponse response = httpClient.execute(httpGet);
            return response;
        } catch (Exception e) {
            System.err.println("Error al realizar la solicitud HTTP: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public boolean update(TaskModel task){
        boolean success = false;
        try{
            String url = "http://" +configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + task.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("name", "download");
            jsonBody.put("file", task.getFilename());
            String requestBody = jsonBody.toString();
            HttpResponse response = doCurl(url, requestBody);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, "UTF-8");

            log.info("["+processId+"]Respuesta del curl: " + responseBody);

            if (responseBody.toLowerCase().contains("device")) {
                // Estado exitoso, realizar las acciones correspondientes
                actualizarEstadoLista(this.taskModelList,task.getId(),"P");
                success = true;
            } else {
                // Error, realizar las acciones correspondientes
                success=false;
                actualizarEstadoLista(this.taskModelList, task.getId(), "E");
                log.error("Error al actualizar el equipo " + task.getId() + " a la versión " + task.getVersion());
            }
        }catch (Exception e){
            actualizarEstadoLista(this.taskModelList, task.getId(), "E");
            log.error("Error en la funcion Update, MENSAJE: "+e.getMessage() );
            success=false;
        }finally {
            return success;
        }
    }
    public boolean checkStatus(TaskModel taskModel){
        boolean sucess=false;
        try{
            if(taskModel.getVersion().equals("xml")||taskModel.getEstado().equals('F')){
                log.info("["+processId+"]Actualizacion completada para equipo ID task :"+taskModel.getId());
                taskModel.setEstado("F");
                sucess=true;
            }else {
                String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/?query=%7B%22_id%22%3A%22" + taskModel.getIdcompuesto() + "%22%7D&projection=InternetGatewayDevice.DeviceInfo.SoftwareVersion._value";
                HttpResponse response = doHttpGet(url);
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, "UTF-8");
                if (response != null && response.getStatusLine().getStatusCode() == 200) {
                    // Evaluar el estado de la respuesta
                    int status = responseBody.toLowerCase().contains("_value") ? 1 : 0;

                    if (status <= 0) {
                        // No se encontró el equipo en genieacs
                        log.error("Error No se pudo encontrar el equipo " + taskModel.getIdcompuesto() + " en genieacs, no se actualizará");
                        log.error("Respuesta: " + responseBody);
                        actualizarEstadoLista(this.taskModelList,taskModel.getId(),"E");
                        sucess=false;
                    } else {
                        // Comparar la versión de ACS con la versión anterior
                        String version_actual =ExtractValue(responseBody);
                        String version_anterior = taskModel.getVersion();
                        sucess=true;
                        if (version_actual.equals(version_anterior)) {
                            // Actualización completa
                            log.info("["+processId+"]Actualización completa de la task " + taskModel.getId() + ", se moverá a la tabla histórica");
                            actualizarEstadoLista(this.taskModelList,taskModel.getId(),"F");
                        }else{
                            actualizarEstadoLista(this.taskModelList,taskModel.getId(),"P");
                            log.info("["+processId+"]Aun esta pendiente la Actualización de la task " + taskModel.getId() + ", esperando intervalo de tiempo.");
                        }
                    }
                }
            }
        }catch (Exception e){
            sucess=false;
            actualizarEstadoLista(this.taskModelList,taskModel.getId(),"E");
        }finally {
            return sucess;
        }
    }
    public boolean TR069(TaskModel taskModel){
        boolean sucess=false;
        try{
            String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + taskModel.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            String requestBody = taskModel.getCommands();

            HttpResponse response =  doCurl(url, requestBody);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, "UTF-8");
            if (response != null && response.getStatusLine().getStatusCode()== 200) {
                log.info("["+processId+"]Respuesta de la solicitud TR069: " + responseBody);
                // Evaluar la respuesta y determinar si fue exitosa
                //PENDIENTE para evaluar SEGUN SU CUERPO
                int result = responseBody.toLowerCase().contains("_value") ? 1 : 0;

                if (result <= 0) {
                    log.error("Error: No se pudo provisionar el equipo en ACS");
                    log.error("Respuesta: " + responseBody);
                    actualizarEstadoLista(this.taskModelList, taskModel.getId(), "E");
                    sucess=false;
                } else {
                    log.info("["+processId+"]Provision completa de la task ID:  " + taskModel.getId() + ", se moverá a la tabla historica");
                    actualizarEstadoLista(this.taskModelList,taskModel.getId(),"F");
                    taskHistoryService.saveByTaskModel(taskModel);
                    taskService.delete(taskModel);
                    sucess=true;
                }
            } else {
                log.error("Error al realizar peticion HTTP para la tarea task: " + taskModel.getId());
                actualizarEstadoLista(this.taskModelList,taskModel.getId(),"E");
                sucess=false;
            }
        }catch (Exception e){
            log.info("["+processId+"]Error en la funcion TR069 MENSAJE: "+e.getMessage());
            sucess=false;
        }finally {
            return sucess;
        }
    }
    public void actualizarEstadoLista(List<TaskModel> taskModelList, int id, String nuevoEstado) {
        for (TaskModel task : taskModelList) {
            if (task.getId() == id) {
                task.setEstado(nuevoEstado);
                task.setReintentos(task.getReintentos()+1);
                break; // Se encontró el objeto, se actualiza y se termina el ciclo
            }
        }
    }
    public void ActualizarEstadoTask(List<TaskModel> taskModelList, int id, String nuevoEstado){
        for (TaskModel task : taskModelList) {
            if (task.getId() == id) {
                task.setEstado(nuevoEstado);
                break; // Se encontró el objeto, se actualiza y se termina el ciclo
            }
        }
    }
    public void actualizarReintentosLista(List<TaskModel> taskModelList, int id, int reintento) {
        for (TaskModel task : taskModelList) {
            if (task.getId() == id) {
                task.setReintentos(reintento);
                break; // Se encontró el objeto, se actualiza y se termina el ciclo
            }
        }
    }
    public void MoveToHistory(){
        log.info("["+processId+"]se procede a mover hacia historico, processId: "+processId);
        taskHistoryService.saveOfTask(this.taskModelList);
        taskService.deleteByProcessId(processId);
        callBackService.ExecuteCallBack(processId);
    }

    @Override
    public void run() {
        try {
            while (status){
                //actualiza a pendiente los estado para
                //que no vuelvan a ser consultados en caso que demore su ejecucion de hilo
                //se trabaja en memoria la lista por hilo para evitar realizar demaciadas consultas.
                taskService.updateByProcessId(taskModelList);
                callBackService=new CallBackService(configReader);
                log.info("["+processId+"]Ejecutando Hilo para process_id: "+processId);
                for (TaskModel taskModel : taskModelList) {
                    if(taskModel.getReintentos()<=configReader.getMaxRetries()){
                        log.info("["+processId+"]actualizando equipo en intento:"+taskModel.getReintentos(),"para el equipo ID Task: "+taskModel.getId());
                        if(taskModel.getEstado().equals("I")){
                            taskModel.setEstado("P");
                            if(taskModel.getTasktype().equals("TR069")){
                                if(!TR069(taskModel)){
                                    MoveToHistory();
                                    stop();
                                    break;
                                };
                            }else{
                                if(!update(taskModel)) {
                                    MoveToHistory();
                                    stop();
                                    break;
                                }
                            }
                            log.info("["+processId+"]Esperando tiempo de actualizacion para task ID: "+taskModel.getId()+", process_id: "+processId);
                            Thread.sleep(configReader.getTimeout());
                            break;
                        }else{
                            if(!checkStatus(taskModel)){
                                MoveToHistory();
                                stop();
                                break;
                            }
                            if(taskModel.getEstado()!="F"){
                                log.info("["+processId+"]Esperando tiempo de actualizacion para task ID: "+taskModel.getId()+", process_id: "+processId);
                                Thread.sleep(configReader.getTimeout());
                                break;
                            }else{
                                log.info("["+processId+"]Tarea completada para task ID: "+taskModel.getId()+", process_id: "+processId);
                                taskHistoryService.saveTask(taskModel);
                                taskModelList.remove(taskModel);
                                if(taskModelList.isEmpty()){
                                    MoveToHistory();
                                    stop();
                                }
                                break;
                            }
                        }
                    }else{
                        log.info("["+processId+"]Reintentos maximo alcanzado para task: "+taskModel.getId()+", process_id: "+processId);
                        ActualizarEstadoTask(this.taskModelList, taskModel.getId(), "E");
                        MoveToHistory();
                        stop();
                        break;
                    }
                }
            }
        }catch (Exception e){
            stop();
            log.error("Error al ejecutar el process_id: "+processId+", MENSAJE: "+e.getMessage());
        }finally {
            log.info("["+processId+"]Proceso Terminado, process_id: "+processId);
            ThreadManagerService.threadCount.decrementAndGet();
        }
    }
    private void whenError(){
        log.info("["+processId+"]se procede a mover hacia historico, processId: "+processId);
        taskHistoryService.saveOfTask(this.taskModelList);
        taskService.deleteByProcessId(processId);
        callBackService.ExecuteCallBack(processId);
        stop();
    }
    public void stop(){
        status=false;
    }
    public void start(){
        status=true;
    }
    private String ExtractValue(String response){
        try {
            JSONArray jsonArray = new JSONArray(response);
            JSONObject jsonObject = jsonArray.getJSONObject(0);

            JSONObject softwareVersion = jsonObject.getJSONObject("InternetGatewayDevice")
                    .getJSONObject("DeviceInfo")
                    .getJSONObject("SoftwareVersion");

            String value = softwareVersion.getString("_value");
            return value;
        } catch (Exception e) {
            System.err.println("Error al procesar la respuesta JSON: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
