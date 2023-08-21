package org.prongpa.Service;

import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.TaskRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    public void remove(TaskModel task){
        this.taskRepository.delete(task);
    }
    public void update(TaskModel task) {
       taskRepository.updateCriteria(task);
    }
    public List<TaskModel> findFromViewCandidates(){
        return taskRepository.findFromViewCandidates();
    }
    public void delete(TaskModel taskModel){
        taskRepository.delete(taskModel);
    }
    public void deleteByProcessId(String id){
        taskRepository.deleteByProcessId(id);
    }
 }


