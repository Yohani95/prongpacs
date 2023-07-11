package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.prongpa.Models.TaskHistoryModel;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.TaskHistory.TaskHistoryRepository;

import java.util.List;
@Slf4j
public class
TaskHistoryService {
    private final TaskHistoryRepository taskHistoryRepository;

    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository) {this.taskHistoryRepository = taskHistoryRepository;}

    public void saveTask(TaskHistoryModel task) {taskHistoryRepository.save(task);}

    public TaskHistoryModel getTaskById(Long id) {
        return taskHistoryRepository.findById(id);
    }

    public List<TaskHistoryModel> getTasksByEstado(String estado) {
        return taskHistoryRepository.findByEstado(estado);
    }
    public boolean saveByProcessId(String processId){
        try{
            return true;
        }catch (Exception e){
            return false;
        }
    }
    public boolean saveOfTask(List<TaskModel> tasks){
        try {
            TaskHistoryModel taskHistoryModel;
            for (TaskModel task: tasks) {
                taskHistoryModel=setData(task);
                taskHistoryRepository.save(taskHistoryModel);
            }
            return true;
        }catch (Exception e){
            log.info("Error Al mover hacia historico :"+ e.getMessage());
            return false;
        }
    }
    private  TaskHistoryModel setData(TaskModel task){
            TaskHistoryModel taskHistoryModel;
            taskHistoryModel=new TaskHistoryModel();
            taskHistoryModel.setId(task.getId());
            taskHistoryModel.setModel(task.getModel());
            taskHistoryModel.setOrderTask(task.getOrderTask());
            taskHistoryModel.setThread_id(task.getThread_id());
            taskHistoryModel.setEstado(task.getEstado());
            taskHistoryModel.setReintentos(task.getReintentos());
            taskHistoryModel.setCommands(task.getCommands());
            taskHistoryModel.setLastDate(task.getLastDate());
            taskHistoryModel.setManufacturer(task.getManufacturer());
            taskHistoryModel.setNotificationId(task.getNotificationId());
            taskHistoryModel.setFilename(task.getFilename());
            taskHistoryModel.setTasktype(task.getTasktype());
            taskHistoryModel.setVersion(task.getVersion());
            taskHistoryModel.setProcess_id(task.getProcess_id());
            taskHistoryModel.setIdcompuesto(task.getIdcompuesto());
            return taskHistoryModel;
    }
    public void saveByTaskModel(TaskModel task){
        taskHistoryRepository.save(setData(task));
    }

}
