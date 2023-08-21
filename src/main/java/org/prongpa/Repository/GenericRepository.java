package org.prongpa.Repository;

import java.util.List;

public interface GenericRepository<T, ID> {
    boolean save(T entity);
    boolean update(T entity);
    T findById(ID id);
    List<T> findAll();
    void delete(T entity);
    List<T> findByCustomCriteria(String customCriteria,Object... params);
}
