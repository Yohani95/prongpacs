package org.prongpa.Service;

import lombok.extern.slf4j.Slf4j;
import org.prongpa.Models.TaskHistoryModel;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Task.TaskRepository;
import org.prongpa.Repository.TaskHistory.TaskHistoryRepository;

import javax.transaction.Transactional;
import java.util.List;
@Slf4j
public class
TaskHistoryService {
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;
    public TaskHistoryService(TaskRepository taskRepository, TaskHistoryRepository taskHistoricoRepository) {
        this.taskRepository = taskRepository;
        this.taskHistoryRepository = taskHistoricoRepository;
    }
    public void saveTask(TaskModel task) {TaskHistoryModel taskHistoryModel=setData(task); taskHistoryRepository.save(taskHistoryModel);}

    public boolean saveOfTask(TaskModel task){
        try {
            TaskHistoryModel taskHistoryModel;
                taskHistoryModel=setData(task);
                taskHistoryRepository.save(taskHistoryModel);
            return true;
        }catch (Exception e){
            log.info("Error Al mover hacia historico :"+ e.getMessage());
            return false;
        }
    }
    @Transactional
    public void copyDataToHistorico(String processId) {
        try{
            List<TaskModel> tasks = taskRepository.findByCustomCriteria("FROM TaskModel t WHERE t.process_id = ?1",  processId);

            for (TaskModel task : tasks) {
                TaskHistoryModel taskHistorico = setData(task);
                taskHistoryRepository.save(taskHistorico);
            }
            taskRepository.deleteByProcessId(processId);
        }catch (Exception e){
            log.info("Error Al mover hacia historico :"+ e.getMessage());
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
            taskHistoryModel.setMercado(task.getMercado());
            return taskHistoryModel;
    }
    public void saveByTaskModel(TaskModel task){
        taskHistoryRepository.save(setData(task));
    }
    public void saveByprocessId(String processId){
        taskHistoryRepository.saveByProcessId(processId);
    }

}
