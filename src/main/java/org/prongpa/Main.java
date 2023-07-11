package org.prongpa;


import lombok.extern.slf4j.Slf4j;
import org.prongpa.Models.ConfigReader;
import org.prongpa.Service.ThreadManagerService;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ConfigReader configReader = new ConfigReader();
        log.info("01-06-2023 - Iniciando Servicio version 1.0.0");
        log.info("04-07-2023 - Agregar logica al finalizar tasks CALLBACK 1.1.0");
        if(configReader.loadConfig()){
            ThreadManagerService threadManagerService = new ThreadManagerService(configReader);
            if(threadManagerService.configure()){
                Thread thread = new Thread(threadManagerService);
                thread.start();
            }
        };

    }
}