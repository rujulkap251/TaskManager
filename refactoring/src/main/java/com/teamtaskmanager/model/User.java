package com.teamtaskmanager.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class User {
	@Id
	@GeneratedValue
	public Long id;
	
	public String name;

	public User() {}

	public User(String name) {
		super();
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	
}