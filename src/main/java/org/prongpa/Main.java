package org.prongpa;


import lombok.extern.slf4j.Slf4j;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Service.ThreadManagerService;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ConfigReader configReader = new ConfigReader();
        log.info("Iniciando Servicio - Version actual 1.1.0");
        log.info("01-06-2023 - version 1.0.0 Inicial");
        log.info("04-07-2023 - version 1.1.0 Agregar logica CALLBACK ");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Deteniendo el proyecto...");
            // Realiza aquí cualquier limpieza o tareas finales antes de cerrar el programa
        }));

        // Manejar la señal SIGINT (Ctrl+C)
        Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal signal) {
                log.info("Recibida la señal SIGINT (Ctrl+C)");
                // Realiza aquí cualquier limpieza o tareas finales antes de cerrar el programa
                System.out.println("intente  cerrarlo con el teclado");
            }
        });
        if(configReader.loadConfig()){
            ThreadManagerService threadManagerService = new ThreadManagerService(configReader);
            if(threadManagerService.configure()){
                Thread thread = new Thread(threadManagerService);
                thread.start();
            }
        };

    }
}