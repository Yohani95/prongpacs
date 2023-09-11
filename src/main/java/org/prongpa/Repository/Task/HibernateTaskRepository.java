package org.prongpa.Repository.Task;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.Hibernate.HibernateGenericRepository;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
public class HibernateTaskRepository extends HibernateGenericRepository<TaskModel, Long> implements TaskRepository {
    private final SessionFactory sessionFactory;
    public HibernateTaskRepository(SessionFactory sessionFactory) {
        super(sessionFactory, TaskModel.class);
        this.sessionFactory = sessionFactory;
    }
    @Override
    public List<TaskModel> findFromViewCandidates() {
        try (Session session = sessionFactory.openSession()) {
            String nativeQuery = "SELECT * FROM view_candidates";
            List<Object[]> resultSet = session.createSQLQuery(nativeQuery).list();

            List<TaskModel> results = new ArrayList<>();

            for (Object[] row : resultSet) {
                TaskModel task = new TaskModel();
                task.setId((int) row[0]);
                task.setEstado((String) row[1]);
                task.setLastDate((Date) row[2]);
                task.setProcess_id((String) row[3]);
                Object threadIdValue = row[4];
                if (threadIdValue != null) {
                    try {
                        task.setThread_id(Integer.valueOf(String.valueOf(threadIdValue)));
                    } catch (NumberFormatException e) {
                        // Handle conversion error
                        task.setThread_id(null); // Or set a default value or handle accordingly
                    }
                } else {
                    task.setThread_id(null);
                }
                task.setModel((String) row[5]);
                task.setOrderTask((int) row[6]);
                task.setReintentos((int) row[7]);
                task.setVersion((String) row[8].toString());
                task.setTasktype((String) row[9]);
                task.setIdcompuesto((String) row[10]);
                task.setFilename((String) row[11]);
                task.setNotificationId((String) row[12]);
                task.setCommands((String) row[13]);
                // Verifica si hay un campo 'mercado' en la consulta
                if (row.length > 14) {
                    task.setMercado((String) row[14]);
                }
                task.setManufacturer((String) row[15]);
                results.add(task);
            }

            return results;
        } catch (Exception e) {
            log.error("Error en la llamada a la vista view_candidate , mensaje"+e.getMessage());
            return Collections.emptyList();
        }
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

    @Override
    public boolean updateCriteria(TaskModel taskModel) {
        TaskModel taskToUpdate = findByCustomCriteria("SELECT t FROM TaskModel t WHERE id = ?1", taskModel.getId()).get(0);
        long currentTime = System.currentTimeMillis();
        taskToUpdate.setLastDate(new Date(currentTime));
        return save(taskToUpdate);
    }

}