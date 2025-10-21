package com.cp.data.crud.interfaces;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import com.cp.util.AppLog;
import jakarta.persistence.EntityManager;

public abstract class AbstractCrud<T> {

    private static final Logger logger = Logger.getLogger(AbstractCrud.class.getName());
    private Class<T> entityClass;

    protected AbstractCrud(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    protected abstract void close();

    private void executeTransaction(Runnable action) {
        try {
            getEntityManager().getTransaction().begin();
            action.run();
            getEntityManager().flush();
            getEntityManager().getTransaction().commit();
        } catch (Exception e) {
            getEntityManager().getTransaction().rollback();
            throw e;
        }
    }

    public Exception persist(T entity) {
        try {
            logger.info("Iniciando operação de persistência...");
            executeTransaction(() -> getEntityManager().persist(entity));
            logger.info("Entidade persistida com sucesso!");
            AppLog.getInstance().info("Registro inserido com sucesso pela classe: " + this.getClass().getName());
            return null;
        } catch (Exception e) {
            AppLog.getInstance().warn("Erro ao inserir no banco de dados: " + this.getClass().getName() + "==>" + e.getMessage());
            return e;
        }
    }

    public Exception merge(T entity) {
        try {
            executeTransaction(() -> getEntityManager().merge(entity));
            AppLog.getInstance().info("Registro alterado com sucesso pela classe: " + this.getClass().getName());
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public Exception remove(T entity) {
        try {
            executeTransaction(() -> getEntityManager().remove(getEntityManager().merge(entity)));
            AppLog.getInstance().info("Registro removido com sucesso pela classe: " + this.getClass().getName());
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> getAll() {
        try {
            jakarta.persistence.criteria.CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
            cq.select(cq.from(entityClass));
            return getEntityManager().createQuery(cq).getResultList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<T> findRange(int[] range) {
        jakarta.persistence.criteria.CriteriaQuery<T> cq = getEntityManager().getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        jakarta.persistence.Query q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public int count() {
        jakarta.persistence.criteria.CriteriaQuery<Long> cq = getEntityManager().getCriteriaBuilder().createQuery(Long.class);
        jakarta.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        jakarta.persistence.Query q = getEntityManager().createQuery(cq);
        return ((Long) q.getSingleResult()).intValue();
    }
}