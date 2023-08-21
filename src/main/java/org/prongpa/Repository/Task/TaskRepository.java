package org.prongpa.Repository.Task;

import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.GenericRepository;

import java.util.List;

public interface TaskRepository extends GenericRepository<TaskModel, Long> {
    List<TaskModel> findByEstado(String query);
    List<TaskModel> findFromViewCandidates();

    boolean UpdateByProcessId(String ProcessId);

    boolean deleteByProcessId(String id);
    // Otros métodos específicos para la clase TaskModel
    boolean updateCriteria(TaskModel taskModel);
}