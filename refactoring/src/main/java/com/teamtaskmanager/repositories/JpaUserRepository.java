package com.teamtaskmanager.repositories;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.springframework.stereotype.Repository;

import com.teamtaskmanager.model.User;

@Repository
@Transactional
public class JpaUserRepository {

	@PersistenceContext
    private EntityManager entityManager;
	
	public List<User> findAll(Class<User> type) {
		return entityManager.createQuery("from " + type.getSimpleName(), type).getResultList();
	}
	
	public void remove(User t) {
		entityManager.remove(t);
	}
	
	public void persist(User t) {
		entityManager.persist(t);
	}
	
}
