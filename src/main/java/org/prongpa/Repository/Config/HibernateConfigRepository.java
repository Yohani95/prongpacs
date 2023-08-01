package org.prongpa.Repository.Config;

import org.hibernate.SessionFactory;
import org.prongpa.Models.ConfigDBModel;
import org.prongpa.Repository.Hibernate.HibernateGenericRepository;

public class HibernateConfigRepository extends HibernateGenericRepository<ConfigDBModel, Long>
        implements ConfigRepository {
    public HibernateConfigRepository(SessionFactory sessionFactory) {
        super(sessionFactory,ConfigDBModel.class);
    }
}
