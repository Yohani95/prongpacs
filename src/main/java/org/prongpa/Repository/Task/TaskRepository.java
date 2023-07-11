package org.prongpa.Repository.Task;

import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.GenericRepository;

import java.util.List;

public interface TaskRepository extends GenericRepository<TaskModel, Long> {
    List<TaskModel> findByEstado(String query);

    boolean UpdateByProcessId(String ProcessId);

    boolean deleteByProcessId(String id);
    // Otros métodos específicos para la clase TaskModel
}