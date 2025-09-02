package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.example.model.Feedback;

import java.util.List;

public class FeedbackRepository {
    private final EntityManagerFactory emf;

    public FeedbackRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void save(Feedback feedback) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(feedback);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public List<Feedback> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Feedback> query = em.createQuery(
                    "SELECT f FROM Feedback f ORDER BY f.createdAt DESC",
                    Feedback.class);
            return query.getResultList();
        }
    }

    public List<Feedback> findAllWithUsers() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Feedback> query = em.createQuery(
                    "SELECT f FROM Feedback f LEFT JOIN FETCH f.user ORDER BY f.createdAt DESC",
                    Feedback.class);
            return query.getResultList();
        }
    }

    public List<Feedback> findFiltered(String branch, String role, String criticality) {
        try (EntityManager em = emf.createEntityManager()) {
            StringBuilder jpql = new StringBuilder("SELECT f FROM Feedback f JOIN FETCH f.user u WHERE 1=1");

            if (branch != null && !branch.isEmpty()) {
                jpql.append(" AND u.branch = :branch");
            }
            if (role != null && !role.isEmpty()) {
                jpql.append(" AND u.role = :role");
            }
            if (criticality != null && !criticality.isEmpty()) {
                jpql.append(" AND f.criticality = :criticality");
            }

            TypedQuery<Feedback> q = em.createQuery(jpql.toString(), Feedback.class);

            if (branch != null && !branch.isEmpty()) {
                q.setParameter("branch", branch);
            }
            if (role != null && !role.isEmpty()) {
                q.setParameter("role", role);
            }
            if (criticality != null && !criticality.isEmpty()) {
                q.setParameter("criticality", Integer.parseInt(criticality));
            }

            return q.getResultList();
        }
    }

    public List<String> findDistinctBranches() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<String> query = em.createQuery(
                    "SELECT DISTINCT u.branch FROM UserProfile u WHERE u.branch IS NOT NULL ORDER BY u.branch",
                    String.class);
            return query.getResultList();
        }
    }

    public List<String> findDistinctRoles() {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<String> query = em.createQuery(
                    "SELECT DISTINCT u.role FROM UserProfile u WHERE u.role IS NOT NULL ORDER BY u.role",
                    String.class);
            return query.getResultList();
        }
    }

}
