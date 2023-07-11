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

    public void saveTask(TaskModel task) {
        taskRepository.save(task);
    }

    public TaskModel getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<TaskModel> getTasksByEstado(String estado) {
        return taskRepository.findByEstado(estado);
    }

    public boolean setTaskState(Long taskId, String state) {
        try {
            Optional<TaskModel> taskOptional = Optional.ofNullable(taskRepository.findById(taskId));
            if (taskOptional.isPresent()) {
                TaskModel task = taskOptional.get();
                task.setEstado(state);
                task.setLastDate(new Date());
                task.setThread_id(1);
                taskRepository.save(task);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public List<TaskModel> findByEstado(String firstStatus){
        return taskRepository.findByEstado(firstStatus);
    }
    public void updateByProcessId(List<TaskModel> tasks){
        List<TaskModel> listTasks=new ArrayList<>(tasks);
        for (TaskModel task: listTasks){
            String oldStatus=task.getEstado();
            task.setEstado("P");
            taskRepository.save(task);
            task.setEstado(oldStatus);
        }
    }
    public void delete(TaskModel taskModel){
        taskRepository.delete(taskModel);
    }
    public void deleteByProcessId(String id){
        taskRepository.deleteByProcessId(id);
    }
 }


