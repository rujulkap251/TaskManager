package com.teamtaskmanager.repositories;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Repository;

import com.teamtaskmanager.model.Task;

@Repository
@Transactional
public class JpaTaskRepository {

	@PersistenceContext
	private EntityManager entityManager;
	
	public List<Task> findAll(Class<Task> type) {
		return entityManager.createQuery("from " + type.getSimpleName(), type).getResultList();
	}
	
	public void remove(Task t) {
		entityManager.remove(t);
	}
	
	public void persist(Task t) {
		entityManager.persist(t);
	}
	
}
