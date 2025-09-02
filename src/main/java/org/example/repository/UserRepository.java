package org.example.repository;

import jakarta.persistence.*;
import org.example.model.UserProfile;

import java.time.LocalDateTime;
import java.util.Optional;


public class UserRepository {
    private final EntityManagerFactory emf;

    public UserRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Optional<UserProfile> findByChatId(Long chatId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<UserProfile> q = em.createQuery(
                    "SELECT u FROM UserProfile u WHERE u.chatId = :chatId", UserProfile.class);
            q.setParameter("chatId", chatId);
            return q.getResultStream().findFirst();
        }
    }

    public void save(UserProfile userProfile) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(userProfile);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public void update(UserProfile userProfile) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(userProfile);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public void upsertByChatId(UserProfile userProfile) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Query query = em.createNativeQuery(
                    "INSERT INTO user_profiles (chatid, role, branch, created_at) " +
                    "VALUES (?1, ?2, ?3, ?4) " +
                    "ON CONFLICT (chatid) DO UPDATE SET " +
                    "role = EXCLUDED.role, branch = EXCLUDED.branch"
            );
            query.setParameter(1, userProfile.getChatId());
            query.setParameter(2, userProfile.getRole());
            query.setParameter(3, userProfile.getBranch());
            query.setParameter(4, LocalDateTime.now());

            query.executeUpdate();
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }
}
