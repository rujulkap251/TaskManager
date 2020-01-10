package com.teamtaskmanager.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teamtaskmanager.model.User;
import com.teamtaskmanager.repositories.JpaUserRepository;

@Service
public class UserService {

	
	@Autowired
	private JpaUserRepository jpaUserRepository;
	
	public List<User> findAll(Class<User> type) {
        return jpaUserRepository.findAll(type);
    }

	
	public User findUserById(Long id) {
        return findAll(User.class).stream().filter(u -> u.id.equals(id)).findFirst().orElse(null);
    }
	
	public void clear() throws IOException {
		findAll(User.class).forEach(t -> jpaUserRepository.remove(t));
    }

	public Stream<User> getAllUsers() {
		return findAll(User.class).stream().sorted(Comparator.comparing(u -> u.name));
	}
	
	public User getOrCreateUser(String userName) {
		User existing = findAll(User.class).stream().filter(u -> u.name.equals(userName)).findFirst().orElse(null);

		if (null == existing) {
			existing = new User();
			existing.name = userName;
			jpaUserRepository.persist(existing);
		}
		return existing;
	}
	
}
