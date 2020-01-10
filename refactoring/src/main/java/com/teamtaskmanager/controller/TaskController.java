package com.teamtaskmanager.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamtaskmanager.dto.TaskDTO;
import com.teamtaskmanager.model.Task;
import com.teamtaskmanager.service.TaskService;

@RestController
public class TaskController {

	@Autowired
	private TaskService  taskService;
	
	@Value("${application.host}")
    private String appRoot;
	
	/**
     * Get a task by its id - returns {"id": 4324324, "user": "billybob", "task": "do this", "due": "yyyy-MM-dd", "isCompleted" : true}
     *
     * @param id
     * @return
     * @throws JsonProcessingException
     */
	@RequestMapping(method = RequestMethod.GET, path = "tasks/{id}")
    public ResponseEntity get(@PathVariable Long id) throws JsonProcessingException {
        TaskDTO result = taskService.get(id);
        
        if (null != result) {
            return ResponseEntity.ok(new ObjectMapper().writeValueAsString(result));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
	
	/**
     * Delete a task by its id
     *
     * @param id
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(method = RequestMethod.DELETE, path = "tasks/{id}")
    public ResponseEntity delete(@PathVariable Long id) throws JsonProcessingException {
        Task result = taskService.taskFilter(id);
        
        if (taskService.isDeleted(result)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search for tasks - returns values in a list of  {"id": 4324324, "user": "billybob", "task": "do this", "due": "tyyyy-MM-dd", "isCompleted" : true}
     *
     * @param user      optional - if set then only tasks for this user are returned - if the user doesn't exist return nothing
     * @param dateAfter optional - if set then only tasks _after_ this date are returned
     * @param sortBy    sort by the field, in the form: fieldName:asc , fieldName:desc, fieldName (defaults to ascending) - optional, defaults to id:asc
     * @return
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.GET, path = "tasks")
    public List<TaskDTO> search(@RequestParam(required = false) String user, @RequestParam(required = false) String dateAfter, @RequestParam(required = false) String sortBy, @RequestParam(required = false) Boolean includeCompleted
    ) throws IOException {
        return taskService.search(user, dateAfter, sortBy, includeCompleted);

    }
    
    
    /**
     * Create a task; expects the form {"user": "billybob", "task": "do this", "due": "yyyy-MM-dd"}
     * <p>
     * The identified user will be created in the DB if it's not already there.
     * <p>
     * All fields are mandatory. The due date cannot be in the past.
     *
     * @param taskJson
     * @return the body will be empty, but there will be a "Location" header that will contain a URL to the new resource
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.POST, path = "tasks")
    public ResponseEntity create(@RequestBody String taskJson) throws IOException {
        TaskDTO dto = new ObjectMapper().readValue(taskJson, TaskDTO.class);
        String errorMessage = taskService.checkBadRequest(dto);
        
        if (errorMessage!=null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Error", errorMessage).build();
        }
        
        Task task = taskService.createTask(dto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Location", appRoot + "/tasks/" + task.id)
                .build();
    }

    /**
     * Completes a task - i.e. marks isCompleted = true
     *
     * @param id
     * @return
     * @throws JsonProcessingException
     */
    @RequestMapping(method = RequestMethod.PUT, path = "tasks/{id}/complete")
    public ResponseEntity complete(@PathVariable Long id) throws JsonProcessingException {
        Task result = taskService.taskFilter(id);
                                  
        if (taskService.isTaskSetToComplete(result)) {
            return ResponseEntity.ok(new ObjectMapper().writeValueAsString(result));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
	
}
