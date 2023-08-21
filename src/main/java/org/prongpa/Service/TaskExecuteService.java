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
import org.json.JSONArray;
import org.json.JSONObject;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;
import org.prongpa.Repository.TaskHistory.HibernateTaskHistoryRepository;
import org.prongpa.Repository.TaskHistory.TaskHistoryRepository;

import java.util.*;

import static org.prongpa.Repository.Hibernate.HibernateUtil.getSessionFactory;

@Slf4j
public class TaskExecuteService implements Runnable {
    private Boolean status;
    private ConfigReader configReader;
    private SessionFactory sessionFactory;
    private TaskRepository taskRepository;
    private TaskService taskService;
    private TaskHistoryService taskHistoryService;
    private TaskHistoryRepository taskHistoryRepository;
    private String processId;
    private TaskModel taskModel;
    CallBackService callBackService;

    public TaskExecuteService(ConfigReader configReader) {
        this.configReader = configReader;
        configure();
    }

    public void configure() {
        try {
            //Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            sessionFactory = getSessionFactory();
            // Crear una instancia del repositorio HibernateTaskRepository
            taskRepository = new HibernateTaskRepository(sessionFactory);
            taskService = new TaskService(taskRepository);
            taskHistoryRepository = new HibernateTaskHistoryRepository(sessionFactory);
            taskHistoryService = new TaskHistoryService(taskHistoryRepository);
            start();
        } catch (Exception e) {
            log.error("Error a cargar configuracion del subhilo Mensaje :" + e.getMessage());
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

    public boolean update(TaskModel task) {
        boolean success = false;
        try {
            String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + task.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("name", "download");
            jsonBody.put("file", task.getFilename());
            String requestBody = jsonBody.toString();
            HttpResponse response = doCurl(url, requestBody);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, "UTF-8");

            log.info("[" + processId + "]Respuesta del curl: " + responseBody);

            if (responseBody.toLowerCase().contains("device")) {
                // Estado exitoso, realizar las acciones correspondientes
                actualizarEstadoLista("P");
                success = true;
            } else {
                // Error, realizar las acciones correspondientes
                success = false;
                actualizarEstadoLista( "E");
                log.error("Error al actualizar el equipo " + task.getId() + " a la versión " + task.getVersion());
            }
        } catch (Exception e) {
            actualizarEstadoLista("E");
            log.error("Error en la funcion Update, MENSAJE: " + e.getMessage());
            success = false;
        } finally {
            return success;
        }
    }

    public boolean checkStatus(TaskModel taskModel) {
        boolean sucess = false;
        try {
            if (taskModel.getVersion().equals("xml") || taskModel.getEstado().equals('F')) {
                log.info("[" + processId + "]Actualizacion completada para equipo ID task :" + taskModel.getId());
                taskModel.setEstado("F");
                sucess = true;
            } else {
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
                        actualizarEstadoLista( "E");
                        sucess = false;
                    } else {
                        // Comparar la versión de ACS con la versión anterior
                        String version_actual = ExtractValue(responseBody);
                        String version_anterior = taskModel.getVersion();
                        sucess = true;
                        if (version_actual.equals(version_anterior)) {
                            // Actualización completa
                            log.info("[" + processId + "]Actualización completa de la task " + taskModel.getId() + ", se moverá a la tabla histórica");
                            actualizarEstadoLista( "F");
                        } else {
                            actualizarEstadoLista( "P");
                            log.info("[" + processId + "]Aun esta pendiente la Actualización de la task " + taskModel.getId() + ", esperando intervalo de tiempo.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            sucess = false;
            actualizarEstadoLista( "E");
        } finally {
            return sucess;
        }
    }

    public boolean TR069(TaskModel taskModel) {
        boolean sucess = false;
        try {
            String url = "http://" + configReader.getApiAcsUrl() + ":" + configReader.getApiAcsPort() + "/devices/" + taskModel.getIdcompuesto() + "/tasks?timeout=3000&connection_request";
            String requestBody = taskModel.getCommands();

            HttpResponse response = doCurl(url, requestBody);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity, "UTF-8");
            log.info("Respuesta de HTTP, TR069: " + responseBody + "HEADER: " + response.getStatusLine().getStatusCode());
            if (response != null && response.getStatusLine().getStatusCode() == 200) {
                log.info("[" + processId + "]Respuesta de la solicitud TR069: " + responseBody);
                // Evaluar la respuesta y determinar si fue exitosa
                //PENDIENTE para evaluar SEGUN SU CUERPO
                int result = responseBody.toLowerCase().contains("_value") ? 1 : 0;

                if (result <= 0) {
                    log.error("Error: No se pudo provisionar el equipo en ACS");
                    log.error("Respuesta: " + responseBody);
                    actualizarEstadoLista( "E");
                    sucess = false;
                } else {
                    log.info("[" + processId + "]Provision completa de la task ID:  " + taskModel.getId() + ", se moverá a la tabla historica");
                    actualizarEstadoLista( "F");
                    taskHistoryService.saveByTaskModel(taskModel);
                    taskService.delete(taskModel);
                    sucess = true;
                }
            } else {
                log.error("Error al realizar peticion HTTP para la tarea task: " + taskModel.getId());
                actualizarEstadoLista("E");
                sucess = false;
            }
        } catch (Exception e) {
            log.info("[" + processId + "]Error en la funcion TR069 MENSAJE: " + e.getMessage());
            sucess = false;
        } finally {
            return sucess;
        }
    }

    public void actualizarEstadoLista(String nuevoEstado) {
        long currentTime = System.currentTimeMillis();
        taskModel.setLastDate(new Date(currentTime));
        taskModel.setEstado(nuevoEstado);
        taskRepository.update(taskModel);
    }

    public void MoveToHistory() {
        log.info("[" + processId + "]se procede a mover hacia historico, processId: " + processId);
        taskHistoryService.saveOfTask(taskModel);
        taskService.deleteByProcessId(taskModel.getProcess_id());
        callBackService.ExecuteCallBack(processId);
    }

    private void handleMaxRetries() {
        log.info("[" + processId + "] Reintentos máximo alcanzado para task: " + taskModel.getId() + ", process_id: " + processId);
        taskModel.setEstado("E");
        MoveToHistory();
    }

    private void processInProgressTask() {
        log.info("[" + processId + "] Actualizando equipo en intento: " + taskModel.getReintentos() + " para el equipo ID Task: " + taskModel.getId());
        actualizarEstadoLista("P");
        if (taskModel.getTasktype().equals("TR069")) {
            if (TR069(taskModel)) {
                log.info("[" + processId + "] Esperando tiempo de actualización para task ID: " + taskModel.getId() + ", process_id: " + processId);
            } else {
                logFailedTask(taskModel);
            }
        } else {
            if (update(taskModel)) {
                log.info("[" + processId + "] Esperando tiempo de actualización para task ID: " + taskModel.getId() + ", process_id: " + processId);
            } else {
                logFailedTask(taskModel);
            }
        }
    }

    private void logFailedTask(TaskModel taskModel) {
        log.info("[" + processId + "] La tarea ha fallado para task ID: " + taskModel.getId() + ", process_id: " + processId);
        MoveToHistory();
    }

    private void processCompletedTask() {
        log.info("[" + processId + "] Tarea completada para task ID: " + taskModel.getId() + ", process_id: " + processId);
        taskHistoryService.saveTask(taskModel);
        taskService.remove(taskModel);
    }

    private void CheckState(TaskModel taskModel) {
        log.info("[" + processId + "]Check Estado para task ID: " + taskModel.getId() + ", process_id: " + processId);
        if(!checkStatus(taskModel)){
            MoveToHistory();
        }
        if(taskModel.getEstado()!="F"){
            log.info("["+processId+"]Esperando tiempo de actualizacion para task ID: "+taskModel.getId()+", process_id: "+processId);
        }else{
            processCompletedTask();
        }
    }

    private TaskModel getNextTask() {
        if (!ThreadManagerService.taskMap.isEmpty()) {
            Map.Entry<String, TaskModel> entry = ThreadManagerService.taskMap.entrySet().iterator().next();
            TaskModel taskModel = entry.getValue();
            ThreadManagerService.taskMap.remove(entry.getKey());
            return taskModel;
        }
        return null;
    }

    private String ExtractValue(String response) {
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
    public void stop() {
        status = false;
    }

    public void start() {
        status = true;
    }

    @Override
    public void run() {
        try {
            while (status) {
                synchronized (ThreadManagerService.taskMap) {
                    taskModel = getNextTask();
                    if (taskModel == null) {
                        continue;
                    }
                }
                configReader = ThreadManagerService.configReader;
                processId = taskModel.getProcess_id();
                callBackService = new CallBackService(configReader);
                log.info("[" + processId + "]Ejecutando tarea para process_id: " + taskModel.getProcess_id());
                if (taskModel.getReintentos() > configReader.getMaxRetries()) {
                    handleMaxRetries();
                } else if (taskModel.getEstado().equals("I")) {
                    processInProgressTask();
                } else if (taskModel.getEstado().equals("F")) {
                    processCompletedTask();
                } else {
                    CheckState(taskModel);
                }
                taskModel.setReintentos(taskModel.getReintentos() + 1);
            }
        } catch (Exception e) {
            log.error("Error al ejecutar el process_id: " + processId + ", MENSAJE: " + e.getMessage());
        } finally {
            ThreadManagerService.threadCount.decrementAndGet();
        }
    }


}
