package org.prongpa.Repository.Task;

import org.hibernate.SessionFactory;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Hibernate.HibernateGenericRepository;

import java.util.List;

public class HibernateTaskRepository extends HibernateGenericRepository<TaskModel, Long> implements TaskRepository {
    public HibernateTaskRepository(SessionFactory sessionFactory) {
        super(sessionFactory, TaskModel.class);
    }

    @Override
    public List<TaskModel> findByEstado(String estado) {
        String queryString = "SELECT t FROM TaskModel t WHERE estado = ?1";
        return findByCustomCriteria(queryString, estado);
    }

    @Override
    public boolean UpdateByProcessId(String processId) {
        TaskModel taskToUpdate = findByCustomCriteria("SELECT t FROM TaskModel t WHERE process_id = ?1", processId).get(0);
        taskToUpdate.setEstado("P");
        return save(taskToUpdate);
    }

    @Override
    public boolean deleteByProcessId(String id) {
        List<TaskModel> tasksToDelete = findByCustomCriteria("SELECT t FROM TaskModel t WHERE process_id = ?1", id);
        for (TaskModel task : tasksToDelete) {
            delete(task);
        }
        return tasksToDelete.isEmpty();
    }
}