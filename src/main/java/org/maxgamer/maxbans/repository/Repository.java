package org.maxgamer.maxbans.repository;

import org.maxgamer.maxbans.transaction.Transactor;

import java.io.Serializable;

/**
 * @author Dirk Jamieson
 */
public abstract class Repository<ID extends Serializable, T> {
    protected final Transactor worker;
    private Class<ID> idClass;
    private Class<T> entityClass;

    public Repository(Transactor worker, Class<ID> idClass, Class<T> entityClass) {
        this.worker = worker;
        this.idClass = idClass;
        this.entityClass = entityClass;
    }

    public T find(ID id) {
        return worker.retrieve((session) -> session.get(entityClass, id));
    }
    
    public void persist(T t) {
        worker.work((session) -> session.persist(t));
    }
    
    public void save(T t) {
        worker.work((session) -> session.saveOrUpdate(t));
    }
}
