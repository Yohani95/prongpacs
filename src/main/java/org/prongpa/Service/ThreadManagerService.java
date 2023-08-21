package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
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
    public static final Map<String, TaskModel> taskMap = new HashMap<>();
    public static ConfigReader configReader;
    private TaskRepository taskRepository;
    private SessionFactory sessionFactory;
    private TaskService taskService;
    private boolean status = false;
    private ConfigDBModel configDBModel;
    public static AtomicInteger threadCount = new AtomicInteger(0); // Variable de contador

    public ThreadManagerService(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public void start() {
        status = true;
    }

    public void stop() {
        status = false;
    }

    public boolean configure() {
        try {
            log.info("Iniciando y cargando configuracion de Hilo padre");
            //Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
            log.info("cargando configuracion de Hibernate BBDD");
            sessionFactory = getSessionFactory();
            // Crear una instancia del repositorio HibernateTaskRepository
            taskRepository = new HibernateTaskRepository(sessionFactory);
            taskService = new TaskService(taskRepository);
            //sesion nueva para configuracion en bbdd
            log.info("Obteniendo Configuracion de bbdd");
            ReloadConfig();
            log.info("Configuracion cargada Correctamente de hilo padre");
            start();
            return true;
        } catch (Exception e) {
            log.error("Error al Cargar configuracion del Hilo principal MENSAJE: " + e.getMessage());
            log.error("No se Iniciara el servicio");
            return false;
        }
    }

    private void ReloadConfig() {
        ConfigRepository configRepository = new HibernateConfigRepository(sessionFactory);
        ConfigServices configServices = new ConfigServices(configRepository);
        configDBModel = configServices.GetConfig();
        //configReader.setTimeout(Integer.parseInt(configDBModel.getAcsRestartTime())* 60 * 1000);
        configReader.setMaxThreads(Integer.parseInt(configDBModel.getMaxThreads()));
        configReader.setMaxRetries(Integer.parseInt(configDBModel.getMaxReintentos()));
    }

    public void run() {
        while (status) {
            try {
                ReloadConfig();
                log.info("Buscando tareas activas");
                // Consultar la base de datos para obtener las tareas activas
                List<TaskModel> activeTasks = taskService.findFromViewCandidates();
                if (activeTasks.isEmpty()) {
                    log.info("sin tareas activas");
                    log.info("Esperando el intervalo de tiempo especificado para que se vuelvan a consultar las tareas, TIEMPO: " + configReader.getTimeout() / (60 * 1000) + " minutos");
                    Thread.sleep(configReader.getTimeout());
                } else {
                    long currentTime = System.currentTimeMillis();
                    List<TaskModel> filteredTasks = new ArrayList<>();
                    for (TaskModel task : activeTasks) {
                        Long time = currentTime - task.getLastDate().getTime();
                        if (task.getEstado().equals("P")) {
                            if (time >= configReader.getWaitProcess()) { // milisegundos
                                task.setLastDate(new Date(currentTime)); // Actualizar la fecha localmente
                                taskService.update(task);
                                filteredTasks.add(task);
                            }
                            continue;
                        }
                        filteredTasks.add(task);
                    }
                    synchronized (taskMap) { // Bloquear el acceso al taskMap
                        for (TaskModel task : filteredTasks) {
                            long taskId = task.getId();
                            taskMap.put(String.valueOf(taskId), task);
                        }
                    } // Desbloquear el acceso al taskMap
                }
                for (int i = 0; i < configReader.getMaxThreads(); i++) {
                    Thread taskThread = new Thread(new TaskExecuteService(configReader));
                    taskThread.start();
                    threadCount.incrementAndGet();
                }
                log.info("Esperando el intervalo de tiempo especificado para que se vuelvan a consultar las tareas, TIEMPO: " + configReader.getWaitProcess() / (60 * 1000) + " minutos");
                log.info("Hilos trabajando actualmente: " + getThreadCount());
                Thread.sleep(configReader.getWaitProcess()); // Esperar el intervalo de tiempo especificado para que se vuelvan a consultar las tareas
            } catch (InterruptedException e) {
                log.error("Error mientras se ejecutaba el hilo padre MENSAJE: " + e.getMessage());
                stop();
            } catch (Exception e) {
                log.error("Error al obtener informacion BBDD,  : " + e.getMessage());
                try {
                    Thread.sleep(configReader.getWaitProcess());
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public int getThreadCount() {
        return threadCount.get();
    }

    private void UpdateDateMap() {
        long currentTime = System.currentTimeMillis();
        synchronized (taskMap) {
            for (TaskModel task : taskMap.values()) {
                if (currentTime - task.getLastDate().getTime() >= 2 * 60 * 1000) {
                    // Realizar la actualización de lastDate en la base de datos
                    task.setLastDate(new Date(currentTime)); // Actualizar la fecha localmente
                    taskService.update(task); // Actualizar en la base de datos
                }
            }
        }
    }
}