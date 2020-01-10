package com.teamtaskmanager.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.teamtaskmanager.model.User;
import com.teamtaskmanager.service.TaskService;
import com.teamtaskmanager.service.UserService;

@RestController
public class UserController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private TaskService taskService;
	
	/**
     * Get a list of all users in the form {"id": 12345, "name": "billybob"}
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "users")
    public ResponseEntity getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

	
    
    /**
     * Used for testing - should clear down all the database entities
     *
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "clear")
    public void clear() throws IOException {
		taskService.clear();
        userService.clear();
    }
    
}
