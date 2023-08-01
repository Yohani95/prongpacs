package org.prongpa.Service;

import org.prongpa.Models.ConfigDBModel;
import org.prongpa.Repository.Config.ConfigRepository;

import java.util.List;
import java.util.Optional;

public class ConfigServices {
    private final ConfigRepository configRepository;
    public ConfigServices(ConfigRepository configRepository){
        this.configRepository=configRepository;
    }
    public List<ConfigDBModel> GetAllConfig(){
        return configRepository.findAll();
    }
    public ConfigDBModel GetConfig(){
        ConfigDBModel config =  configRepository.findAll().get(0);
        return config;
    }
}
