package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.prongpa.Models.ConfigDBModel;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Config.ConfigRepository;
import org.prongpa.Repository.Config.HibernateConfigRepository;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.prongpa.Repository.Hibernate.HibernateUtil.getSessionFactory;

@Slf4j
public class ThreadManagerService implements Runnable {
    private List<TaskModel> tasks;
    private ConfigReader configReader;
    private TaskRepository taskRepository;
    private SessionFactory sessionFactory;
    private TaskService taskService;
    private boolean status=false;
    private ConfigDBModel configDBModel;
    public static AtomicInteger threadCount = new AtomicInteger(0); // Variable de contador
    public ThreadManagerService(ConfigReader configReader) {
        this.configReader = configReader;
    }
    public void start(){
        status=true;
    }
    public void stop(){
        status=false;
    }
    public boolean configure(){
        try{
            log.info("Iniciando y cargando configuracion de Hilo padre");
            //Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            log.info("cargando configuracion de Hibernate BBDD");
            sessionFactory = getSessionFactory();
            // Crear una instancia del repositorio HibernateTaskRepository
            taskRepository = new HibernateTaskRepository(sessionFactory);
            taskService=new TaskService(taskRepository);
            log.info("sesion nueva para configuracion en bbdd");
            //sesion nueva para configuracion en bbdd
            ConfigRepository configRepository=new HibernateConfigRepository(sessionFactory);
            ConfigServices configServices=new ConfigServices(configRepository);
            log.info("Obteniendo Configuracion de bbdd");
            configDBModel=configServices.GetConfig();
            configReader.setTimeout(Integer.parseInt(configDBModel.getAcsRestartTime())* 60 * 1000);
            configReader.setMaxThreads(Integer.parseInt(configDBModel.getMaxThreads()));
            configReader.setMaxRetries(Integer.parseInt(configDBModel.getMaxReintentos()));
            log.info("Configuracion cargada Correctamente de hilo padre");
            start();
            return true;
        }catch (Exception e){
            log.error("Error al Cargar configuracion del Hilo principal MENSAJE: "+e.getMessage());
            log.error("No se Iniciara el servicio");
            return false;
        }
    }
    private void ReloadConfig(){
        ConfigRepository configRepository=new HibernateConfigRepository(sessionFactory);
        ConfigServices configServices=new ConfigServices(configRepository);
        configDBModel=configServices.GetConfig();
        configReader.setTimeout(Integer.parseInt(configDBModel.getAcsRestartTime())* 60 * 1000);
        configReader.setMaxThreads(Integer.parseInt(configDBModel.getMaxThreads()));
        configReader.setMaxRetries(Integer.parseInt(configDBModel.getMaxReintentos()));
    }

    public void run() {
        while (status) {
            try {
                ReloadConfig();
                log.info("Buscando tareas activas");
                if(configReader.getMaxThreads()<=threadCount.get()){
                    log.info("Hilos maximo alcanzado esperando que decremente. HILOS: "+getThreadCount());
                    Thread.sleep(configReader.getTimeout()); // Esperar el intervalo de tiempo especificado
                    continue;
                }
            // Consultar la base de datos para obtener las tareas activas
            List<TaskModel> activeTasks = taskService.findByEstado("I");
            if(activeTasks.isEmpty()){
                log.info("sin tareas activas");
                log.info("Esperando el intervalo de tiempo especificado para que se vuelvan a consultar las tareas, TIEMPO: "+configReader.getTimeout()/(60*1000)+" minutos");
                Thread.sleep(configReader.getTimeout());
            }else {
                // Crear un HashMap para agrupar las tareas por process_id+
                HashMap<String, List<TaskModel>> taskMap = new HashMap<>();

                // Aislar las tareas por process_id
                for (TaskModel task : activeTasks) {
                    String processId = task.getProcess_id();
                    List<TaskModel> taskList = taskMap.getOrDefault(processId, new ArrayList<>());
                    taskList.add(task);
                    taskMap.put(processId, taskList);
                }

                // Ordenar los lotes de tareas por el campo 'order'
                List<List<TaskModel>> taskBatches = new ArrayList<>(taskMap.values());
                for (List<TaskModel> batch : taskBatches) {
                    Collections.sort(batch, Comparator.comparingInt(TaskModel::getOrderTask));
                }

                // Crear un hilo para cada tarea activa4
                for (Map.Entry<String, List<TaskModel>> entry : taskMap.entrySet()) {
                    String processId = entry.getKey();
                    List<TaskModel> taskList = entry.getValue();
                    if(configReader.getMaxThreads()<=threadCount.get()){
                        log.info("Hilos maximo alcanzado esperando que decremente. HILOS: "+getThreadCount());
                        break;
                    }else{
                        Thread taskThread = new Thread(new TaskExecuteService(taskList, configReader, processId));
                        taskThread.start();
                        threadCount.incrementAndGet();
                    }
                }
                log.info("Esperando el intervalo de tiempo especificado para que se vuelvan a consultar las tareas, TIEMPO: "+configReader.getWaitProcess()/(60*1000)+" minutos");
                log.info("Hilos trabajando actualmente: "+getThreadCount());
                Thread.sleep(configReader.getWaitProcess()); // Esperar el intervalo de tiempo especificado para que se vuelvan a consultar las tareas
            }
            } catch (InterruptedException e) {
                log.error("Error mientras se ejecutaba el hilo padre MENSAJE: "+e.getMessage());
                stop();
            }catch (Exception e){
                log.error("Error al obtener informacion BBDD,  : "+e.getMessage());
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    public int getThreadCount() {
        return threadCount.get();
    }
}