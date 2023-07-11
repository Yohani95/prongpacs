package org.prongpa.Repository.TaskHistory;

import org.prongpa.Models.TaskHistoryModel;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.GenericRepository;

import java.util.List;

public interface TaskHistoryRepository extends GenericRepository<TaskHistoryModel,Long> {
    List<TaskHistoryModel> findByEstadoOrEstado(String firstEstado, String secondEstado);

    List<TaskHistoryModel> findByEstado(String query);
    // Otros métodos específicos para la clase TaskModel
    boolean saveOfTask(String processId,String query);
    void saveByProcessId(String processId);
}
