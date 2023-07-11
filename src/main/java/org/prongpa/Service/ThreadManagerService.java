package org.prongpa.Service;

import com.mysql.cj.log.Log;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.HibernateTaskRepository;
import org.prongpa.Repository.Task.TaskRepository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ThreadManagerService implements Runnable {
    private List<TaskModel> tasks;
    private ConfigReader configReader;
    private TaskRepository taskRepository;
    private SessionFactory sessionFactory;
    private TaskService taskService;
    private boolean status=false;
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
            Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            sessionFactory = configuration.buildSessionFactory();
            // Crear una instancia del repositorio HibernateTaskRepository
            taskRepository = new HibernateTaskRepository(sessionFactory);
            taskService=new TaskService(taskRepository);
            start();
            return true;
        }catch (Exception e){
            log.error("Error al Cargar configuracion del Hilo principal MENSAJE: "+e.getMessage());
            return false;
        }
    }

    public void run() {
        while (status) {
            try {
            if(configReader.getMaxThreads()<=getThreadCount()){
                log.info("Hilos maximo alcanzado esperando que decremente. HILOS: "+getThreadCount());
                Thread.sleep(configReader.getTimeout()); // Esperar el intervalo de tiempo especificado
            }
            log.info("Buscando tareas activas");
            // Consultar la base de datos para obtener las tareas activas
            List<TaskModel> activeTasks = taskService.findByEstado("I");
            if(activeTasks.isEmpty()){
                log.info("sin tareas activas");
                Thread.sleep(configReader.getTimeout());
            }else {
                // Crear un HashMap para agrupar las tareas por process_id
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
                    Thread taskThread = new Thread(new TaskExecuteService(taskList, configReader, processId));
                    taskThread.start();
                    threadCount.incrementAndGet(); // Incrementar el contador de hilos
                }
                log.info("Esperando el intervalo de tiempo especificado para que se vuelvan a consultar las tareas, TIEMPO: "+configReader.getTimeout()+" minutos");
                Thread.sleep(configReader.getTimeout()); // Esperar el intervalo de tiempo especificado para que se vuelvan a consultar las tareas
            }
            } catch (InterruptedException e) {
                log.info("Error mientras se ejecutaba el hilo padre MENSAJE: "+e.getMessage());
                log.info("Se procede a detener el servicio");
                stop();
            }
        }
    }
    public int getThreadCount() {
        return threadCount.get();
    }
}