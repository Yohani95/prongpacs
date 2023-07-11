package org.prongpa.Service;

import com.mysql.cj.xdevapi.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;
import org.prongpa.Repository.TaskHistory.HibernateTaskHistoryRepository;
import org.prongpa.Repository.TaskHistory.TaskHistoryRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

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
    CallBackService  callBackService;
    public TaskExecuteService(List<TaskModel> taskModellist, ConfigReader configReader, String processId) {
        this.taskModelList = taskModellist;
        this.configReader = configReader;
        this.processId = processId;
        configure();
    }
    public void configure(){
        try {
            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            sessionFactory = configuration.buildSessionFactory();
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
    public HttpResponse<String> doCurl(String url, String requestBody) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Error al realizar la solicitud HTTP: " + e.getMessage());
            return null;
        }
    }
    public boolean update(TaskModel task){
        boolean success = false;
        try{
            String url = "http://" +configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + task.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            String requestBody = "{\"name\": \"download\", \"file\": \"" + task.getFilename() + "\"}";
            HttpResponse<String> response = doCurl(url, requestBody);
            String responseBody = response.body();
            log.info("Respuesta del curl: " + responseBody);

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
            if(taskModel.getVersion().equals("xml")){
                log.info("Actualizacion completada para equipo ID task :"+taskModel.getId());
                taskModel.setEstado("F");
                sucess=true;
            }else {
                String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/?query=%7B%22_id%22%3A%22" + taskModel.getIdcompuesto() + "%22%7D&projection=InternetGatewayDevice.DeviceInfo.SoftwareVersion._value";
                HttpResponse<String> response = doCurl(url, null);
                if (response != null && response.statusCode() == 200) {
                    String responseBody = response.body();

                    // Evaluar el estado de la respuesta
                    int status = 0 ;//evaluarEstadoRespuesta(responseBody);

                    if (status <= 0) {
                        // No se encontró el equipo en genieacs
                        log.error("Error No se pudo encontrar el equipo " + taskModel.getIdcompuesto() + " en genieacs, no se actualizará");
                        log.error("Respuesta: " + responseBody);
                        actualizarEstadoLista(this.taskModelList,taskModel.getId(),"E");
                        sucess=false;
                    } else {
                        // Comparar la versión de ACS con la versión anterior
                        String version_actual =""; //obtenerVersionACS(responseBody);
                        String version_anterior = taskModel.getVersion();

                        if (version_actual.equals(version_anterior)) {
                            // Actualización completa
                            //log.info("Actualización completa de la task " + taskModel.getId() + ", se moverá a la tabla histórica");
                            actualizarEstadoLista(this.taskModelList,taskModel.getId(),"F");
                        } else {
                            actualizarEstadoLista(this.taskModelList,taskModel.getId(),"E");
                        }
                        sucess=true;
                    }
                }
            }
        }catch (Exception e){

        }finally {
            return sucess;
        }
    }
    public boolean TR069(TaskModel taskModel){
        boolean sucess=false;
        try{
            String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + taskModel.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            String requestBody = taskModel.getCommands();

            HttpResponse<String> response =  doCurl(url, requestBody);
            if (response != null && response.statusCode() == 200) {
                String responseBody = response.body();
                log.info("Respuesta de la solicitud TR069: " + responseBody);
                // Evaluar la respuesta y determinar si fue exitosa
                int result = responseBody.toLowerCase().contains("_value") ? 1 : 0;

                if (result <= 0) {
                    log.error("Error: No se pudo provisionar el equipo en ACS");
                    log.error("Respuesta: " + responseBody);
                    actualizarEstadoLista(this.taskModelList, taskModel.getId(), "E");
                    sucess=false;
                } else {
                    log.info("Provision completa de la task ID:  " + taskModel.getId() + ", se moverá a la tabla historica");
                    actualizarEstadoLista(this.taskModelList,taskModel.getId(),"F");
                    taskHistoryService.saveByTaskModel(taskModel);
                    taskService.delete(taskModel);
                    sucess=true;
                }
            } else {
                log.error("Error al realizar peticion HTTP para la tarea task: " + taskModel.getId());
                actualizarEstadoLista(this.taskModelList,taskModel.getId(),"I");
                sucess=false;
            }
        }catch (Exception e){
            log.info("Error en la funcion TR069 MENSAJE: "+e.getMessage());
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
    public void actualizarReintentosLista(List<TaskModel> taskModelList, int id, int reintento) {
        for (TaskModel task : taskModelList) {
            if (task.getId() == id) {
                task.setReintentos(reintento);
                break; // Se encontró el objeto, se actualiza y se termina el ciclo
            }
        }
    }
    public void MoveToHistory(){
        log.info("se procede a mover hacia historico, processId: "+processId);
        taskHistoryService.saveOfTask(this.taskModelList);
        //taskService.deleteByProcessId(processId);
    }

    @Override
    public void run() {
        try {
            while (status){
                callBackService=new CallBackService(configReader);
                log.info("Ejecutando Hilo para process_id: "+processId);
                //actualiza a pendiente los estado para
                //que no vuelvan a ser consultados en caso que demore su ejecucion de hilo
                //se trabaja en memoria la lista por hilo para evitar realizar demaciadas consultas.
                taskService.updateByProcessId(taskModelList);
                for (TaskModel taskModel : taskModelList) {

                    if(taskModel.getReintentos()<=configReader.getMaxRetries()){
                        log.info("actualizando equipo en intento:"+taskModel.getReintentos(),"para el equipo ID Task: "+taskModel.getId());
                        if(taskModel.getEstado().equals("I")){
                            taskModel.setEstado("P");
                            if(taskModel.getTasktype().equals("TR069")){
                                if(!TR069(taskModel)){MoveToHistory(); stop(); break;};
                            }else{
                                if(!update(taskModel)){MoveToHistory(); stop(); break;}
                            }
                            Thread.sleep(configReader.getTimeout());
                            break;
                        }else{
                            if(!checkStatus(taskModel)){
                                MoveToHistory();
                                stop();
                                break;
                            }
                        }
                    }else{
                        log.info("Reintentos maximo alcanzado para task: "+taskModel.getId()+", process_id: "+processId);
                        MoveToHistory();
                        //callBackService.ExecuteCallBack(processId);
                        stop();
                        break;
                    }
                }
                log.info("Proceso Terminado, process_id: "+processId);
                MoveToHistory();
                //callBackService.ExecuteCallBack(processId);
                stop();
            }
        }catch (Exception e){
            stop();
            ThreadManagerService.threadCount.decrementAndGet();
            log.error("Error al ejecutar el process_id: "+processId+", MENSAJE: "+e.getMessage());
        }
    }
    private void whenError(){
        log.info("se procede a mover hacia historico, processId: "+processId);
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
}
