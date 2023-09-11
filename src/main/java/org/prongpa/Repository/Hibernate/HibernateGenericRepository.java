package org.prongpa.Repository.Hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.prongpa.Models.TaskHistoryModel;
import org.prongpa.Models.TaskModel;
import org.prongpa.Repository.GenericRepository;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;
@Slf4j
public abstract class HibernateGenericRepository<T, ID> implements GenericRepository<T, ID> {
    private final SessionFactory sessionFactory;
    private final Class<T> entityClass;

    public HibernateGenericRepository(SessionFactory sessionFactory, Class<T> entityClass) {
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    @Override
    public boolean save(T entity) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        boolean status=false;
        try {
            session.saveOrUpdate(entity);
            transaction.commit();
            status=true;
        } catch (Exception e) {
            transaction.rollback();
            log.error("Error en update con la entidad, mensaje"+e.getMessage());
            throw e;
        } finally {
            session.close();
            return status;
        }
    }

    @Override
    public boolean update(T entity) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        boolean status=false;
        try {
            session.update(entity);
            transaction.commit();
            status=true;
        } catch (Exception e) {
            transaction.rollback();
            log.error("Error en update con la entidad, mensaje"+e.getMessage());
            throw e;
        } finally {
            session.close();
            return status;
        }
    }

    @Override
    public T findById(ID id) {
        Session session = sessionFactory.openSession();
        try {
            return session.get(entityClass, (Serializable) id);
        } finally {
            session.close();
        }
    }

    @Override
    public List<T> findAll() {
        Session session = sessionFactory.openSession();
        try {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(entityClass);
            Root<T> root = query.from(entityClass);
            query.select(root);
            return session.createQuery(query).getResultList();
        } finally {
            session.close();
        }
    }

    @Override
    public void delete(T entity) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.delete(entity);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            log.error("Error en update con la entidad, mensaje"+e.getMessage());
            throw e;
        } finally {
            session.close();
        }
    }
    @Override
    public List<T> findByCustomCriteria(String customCriteria, Object... params) {
        Session session = sessionFactory.openSession();
        try {
            Query query = session.createQuery(customCriteria);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
            return query.getResultList();
        } finally {
            session.close();
        }
    }
}

