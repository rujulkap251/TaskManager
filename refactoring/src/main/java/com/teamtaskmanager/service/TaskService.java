package com.teamtaskmanager.service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.teamtaskmanager.dto.TaskDTO;
import com.teamtaskmanager.model.Task;
import com.teamtaskmanager.repositories.JpaTaskRepository;
import com.teamtaskmanager.utilities.FormatDate;

@Service
public class TaskService {

	
	@Autowired
	private JpaTaskRepository jpaTaskRepository;
	
	@Autowired
	private UserService userService;
	
	
	public List<Task> findAll(Class<Task> type) {
		return jpaTaskRepository.findAll(type);
	}

	

	public void clear() throws IOException {
		findAll(Task.class).forEach(t -> jpaTaskRepository.remove(t));
	}

	public Boolean isDeleted(Task taskToDelete) {
		if (null != taskToDelete) {
			jpaTaskRepository.remove(taskToDelete);
            return true;
        } else {
            return false;
        }
		
	}

	public Task taskFilter(Long id) {
		return findAll(Task.class).stream().filter(t -> t.id.equals(id)).findFirst().orElse(null);
	}

	public int sort(String sortBy, TaskDTO o1, TaskDTO o2) {
		if (null == sortBy) {
			return o1.id.compareTo(o2.id);
		} else {
			String sortField = sortBy.contains(":") ? sortBy.substring(0, sortBy.indexOf(":")) : sortBy;
			boolean isDescending = sortBy.contains(":") && sortBy.substring(sortBy.indexOf(":") + 1).equals("desc");

			Comparable f1 = selectField(o1, sortField);
			Comparable f2 = selectField(o2, sortField);

			// sort by ids ascending if the fields are the same
			if (f1.equals(f2)) {
				return o1.id.compareTo(o2.id);
			}
			if (isDescending) {
				return f2.compareTo(f1);
			} else {
				return f1.compareTo(f2);
			}

		}
	}

	public List<TaskDTO> search(String user, String dateAfter, String sortBy, Boolean includeCompleted) {
		return findAll(Task.class)
                .stream()
                .map(this::taskToDTO)
                .filter(t -> null == user || t.user.equals(user))
                .filter(t -> null == dateAfter || FormatDate.toDate(dateAfter).before(FormatDate.toDate(t.due)))
                .filter(t -> Boolean.TRUE.equals(includeCompleted) || !t.isCompleted)
                .sorted((o1, o2) -> sort(sortBy, o1, o2))
                .collect(Collectors.toList());
	}

	private TaskDTO taskToDTO(Task t) {
        TaskDTO dto = new TaskDTO();
        dto.due = t.due;
        dto.id = t.id;
        dto.isCompleted = t.isCompleted;
        dto.task = t.task;
        dto.user = userService.findUserById(t.userId).name;
        return dto;
    }
	
	public TaskDTO get(Long id) {
		return findAll(Task.class).stream()
                .filter(t -> t.id.equals(id))
                .map(this::taskToDTO)
                .findFirst().orElse(null);
	}
	
	private Comparable<?> selectField(TaskDTO task, String field) {
		switch (field) {
		case "user":
			return task.user;
		case "id":
			return task.id;
		case "due":
			return task.due;
		case "task":
			return task.task;
		default:
			throw new IllegalArgumentException("Not a field: " + field);
		}
	}

	public Date toDate(String date) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Bad date: " + date, e);
		}
	}

	public String checkBadRequest(com.teamtaskmanager.dto.TaskDTO dto) {
		if (null == dto.due) {
			return "Due can't be empty";
		}
		if ((FormatDate.toDate(dto.due)).before(new Date())) {
			return "Due can't be in the past";
		}
		if (null == dto.user) {
			return "User can't be empty";
		}
		if (null == dto.task) {
			return "Task can't be empty";
		}
		return null;
	}

	public Task createTask(TaskDTO dto) {
		Task task = new Task();
		task.task = dto.task;
		task.due = dto.due;
		task.userId = userService.getOrCreateUser(dto.user).id;
		jpaTaskRepository.persist(task);
		return task;
	}

	public Boolean isTaskSetToComplete(Task result) {
		if (null != result) {
			result.isCompleted = true;
			jpaTaskRepository.persist(result);
			return true;
		}
		return false;
	}

}
