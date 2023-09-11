package org.prongpa.Repository.TaskHistory;

import org.hibernate.SessionFactory;
import org.prongpa.Models.TaskHistoryModel;
import org.prongpa.Repository.Hibernate.HibernateGenericRepository;

import java.util.List;

public class HibernateTaskHistoryRepository extends HibernateGenericRepository<TaskHistoryModel,Long> implements TaskHistoryRepository{
    public HibernateTaskHistoryRepository(SessionFactory sessionFactory) {
        super(sessionFactory, TaskHistoryModel.class);
    }

    @Override
    public List<TaskHistoryModel> findByEstadoOrEstado(String firstEstado, String secondEstado) {
        String queryString = "SELECT t FROM TaskHistoryModel t WHERE estado = ?1 OR estado = ?2";
        return findByCustomCriteria(queryString, firstEstado, secondEstado);
    }


    @Override
    public List<TaskHistoryModel> findByEstado(String estado) {
        String queryString = "SELECT t FROM TaskHistoryModel t WHERE estado = ?1";
        return findByCustomCriteria(queryString, estado);
    }

    @Override
    public boolean saveOfTask(String processId,String query) {
       return true;
    }

    @Override
    public void saveByProcessId(String processId) {

    }

}
