package com.kraiven.taskmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.Spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.kraiven.taskmanager.Service.taskService;
import com.kraiven.taskmanager.Service.TaskAuditResponse;
import com.kraiven.taskmanager.controller.AuthenticationController;
import com.kraiven.taskmanager.controller.UserController;
import com.kraiven.taskmanager.dto.AuthenticationRequest;
import com.kraiven.taskmanager.dto.AuthenticationResponse;
import com.kraiven.taskmanager.dto.TaskRequest;
import com.kraiven.taskmanager.dto.userDTO;
import com.kraiven.taskmanager.entity.Role;
import com.kraiven.taskmanager.entity.Category;
import com.kraiven.taskmanager.entity.Task;
import com.kraiven.taskmanager.entity.TaskAuditEntry;
import com.kraiven.taskmanager.entity.User;
import com.kraiven.taskmanager.exception.TaskNotFoundException;
import com.kraiven.taskmanager.repository.UserRepository;
import com.kraiven.taskmanager.repository.CategoryRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.kraiven.taskmanager.security.JwtUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/** Integration tests for authentication, access rules, CRUD, audit, and Swagger. */
@SpringBootTest(properties = {
		"jwt.secret=test-only-signing-key-for-context-test-not-production",
		"jwt.token.lifespan=3600000"
})
@AutoConfigureMockMvc
class TaskmanagerApplicationTests {

	@Autowired
	private UserController userController;

	@Autowired
	private AuthenticationController authenticationController;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private taskService taskService;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private MockMvc mockMvc;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}
	// Confirms Spring can start the complete application context.
	@Test
	void contextLoads() {
	}

	// Registers a user and verifies the same raw password can authenticate.
	@Test
	void registeredUserCanLogInWithTheSamePassword() {
		userController.saveUser(user("login-test-user", "test-password"));

		ResponseEntity<AuthenticationResponse> response =
				authenticationController.createAuthenticationToken(
						new AuthenticationRequest("login-test-user", "test-password"));

		assertNotNull(response.getBody());
		assertNotNull(response.getBody().getJwt());
	}

	// Ensures public registration assigns USER and Spring sees ROLE_USER.
	@Test
	void registrationAlwaysCreatesAUserRole() {
		String username = uniqueUsername();
		userController.saveUser(user(username, "test-password"));

		User registeredUser = userRepository.findByUsername(username).orElseThrow();
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);

		assertEquals(Role.USER, registeredUser.getRole());
		assertEquals(
				Set.of(new SimpleGrantedAuthority("ROLE_USER")),
				userDetails.getAuthorities());
	}

	// Checks owner-only task access and the admin ownership bypass.
	@Test
	void usersCanOnlyAccessTheirOwnTasksButAdminsCanAccessAnyTask() {
		User owner = saveUser(uniqueUsername(), Role.USER);
		User otherUser = saveUser(uniqueUsername(), Role.USER);
		User admin = saveUser(uniqueUsername(), Role.ADMIN);

		authenticate(owner.getUsername(), "USER");
		var createdTask = taskService.createTask(new TaskRequest(
				"Owner task",
				"Task owned by the first user",
				"MEDIUM",
				LocalDate.now().plusDays(1),
				null,
				null));

		assertNotNull(taskService.getTaskByID(createdTask.id()));

		authenticate(otherUser.getUsername(), "USER");
		assertThrows(TaskNotFoundException.class, () -> taskService.getTaskByID(createdTask.id()));

		authenticate(admin.getUsername(), "ADMIN");
		assertNotNull(taskService.getTaskByID(createdTask.id()));
	}

	// Verifies create/update/delete snapshots and protects history by owner.
	@Test
	void taskHistoryRecordsCreateUpdateAndDeleteWithActorAndSnapshots() throws Exception {
		User owner = saveUser(uniqueUsername(), Role.USER);
		User otherUser = saveUser(uniqueUsername(), Role.USER);
		authenticate(owner.getUsername(), "USER");

		var createdTask = taskService.createTask(new TaskRequest(
				"Original task",
				"Initial description",
				"LOW",
				LocalDate.now().plusDays(1),
				null,
				null));
		taskService.updateTask(createdTask.id(), new TaskRequest(
				"Updated task",
				"Revised description",
				"HIGH",
				LocalDate.now().plusDays(2),
				Task.TaskStatus.COMPLETED,
				null));
		taskService.deleteTask(createdTask.id());

		List<TaskAuditResponse> history = taskService.getTaskHistory(createdTask.id());
		assertEquals(
				List.of(
						TaskAuditEntry.Action.CREATED,
						TaskAuditEntry.Action.UPDATED,
						TaskAuditEntry.Action.DELETED),
				history.stream().map(TaskAuditResponse::action).toList());
		assertEquals(owner.getUsername(), history.get(0).actorUsername());
		assertEquals("Original task", history.get(0).title());
		assertEquals("Updated task", history.get(1).title());
		assertEquals("Updated task", history.get(2).title());
		assertTrue(history.stream().allMatch(entry -> entry.occurredAt() != null));

		String ownerToken = jwtUtil.generateToken(
				userDetailsService.loadUserByUsername(owner.getUsername()));
		mockMvc.perform(get("/api/v1/tasks/{id}/history", createdTask.id())
				.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("DELETED")));

		authenticate(otherUser.getUsername(), "USER");
		assertTrue(taskService.getTaskHistory(createdTask.id()).isEmpty());
	}

	// Exercises actual HTTP authorization outcomes for users and admins.
	@Test
	void jwtRolesControlEndpointAccess() throws Exception {
		User user = saveUser(uniqueUsername(), Role.USER);
		String userToken = jwtUtil.generateToken(
				userDetailsService.loadUserByUsername(user.getUsername()));

		mockMvc.perform(get("/api/v1/tasks"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/tasks")
				.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", "Bearer " + userToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"User category\",\"description\":\"Not permitted\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden());

		User admin = saveUser(uniqueUsername(), Role.ADMIN);
		String adminToken = jwtUtil.generateToken(
				userDetailsService.loadUserByUsername(admin.getUsername()));

		mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Admin category\",\"description\":\"Admin only\"}"))
				.andExpect(status().isCreated());
	}

	// Tests category CRUD and confirms in-use categories cannot be deleted.
	@Test
	void adminCanCompleteCategoryCrud() throws Exception {
		User admin = saveUser(uniqueUsername(), Role.ADMIN);
		String token = jwtUtil.generateToken(
				userDetailsService.loadUserByUsername(admin.getUsername()));
		String categoryName = "category-" + UUID.randomUUID().toString().substring(0, 8);

		mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + categoryName + "\",\"description\":\"Initial\"}"))
				.andExpect(status().isCreated());

		Category category = categoryRepository.findAll().stream()
				.filter(candidate -> candidate.getName().equals(categoryName))
				.findFirst()
				.orElseThrow();

		mockMvc.perform(get("/api/v1/categories")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(categoryName)));
		mockMvc.perform(get("/api/v1/categories/{id}", category.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		String updatedName = categoryName + "-updated";
		mockMvc.perform(put("/api/v1/categories/{id}", category.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + updatedName + "\",\"description\":\"Updated\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(updatedName)));
		authenticate(admin.getUsername(), "ADMIN");
		var task = taskService.createTask(new TaskRequest(
				"Category-bound task",
				"Prevents deleting an in-use category",
				"LOW",
				LocalDate.now().plusDays(1),
				null,
				category.getId()));
		mockMvc.perform(delete("/api/v1/categories/{id}", category.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/v1/tasks/{id}", task.id())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/categories/{id}", category.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/categories/{id}", category.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	// Tests user registration and admin CRUD while checking passwords stay hidden.
	@Test
	void adminCanCompleteUserCrudWithoutExposingPassword() throws Exception {
		String username = uniqueUsername();
		String registrationBody = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"test-password\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertFalse(registrationBody.contains("password"));
		assertTrue(registrationBody.contains(username));

		User registeredUser = userRepository.findByUsername(username).orElseThrow();
		User admin = saveUser(uniqueUsername(), Role.ADMIN);
		String token = jwtUtil.generateToken(
				userDetailsService.loadUserByUsername(admin.getUsername()));

		mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(username)));
		mockMvc.perform(get("/api/v1/users/{id}", registeredUser.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("password"))));

		String updatedUsername = uniqueUsername();
		mockMvc.perform(put("/api/v1/users/{id}", registeredUser.getId())
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + updatedUsername + "\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("\"role\":\"ADMIN\"")));
		mockMvc.perform(delete("/api/v1/users/{id}", registeredUser.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/users/{id}", registeredUser.getId())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	// Confirms Swagger is public and the generated document lists key secured routes.
	@Test
	void openApiDocumentationIsPublicAndIncludesSecuredTaskEndpoints() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/tasks/{id}/history")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/users/{id}")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("bearerAuth")));
	}

	private userDTO user(String username, String password) {
		userDTO user = new userDTO();
		user.setUsername(username);
		user.setPassword(password);
		return user;
	}

	// Creates test accounts directly so each test can choose a role.
	private User saveUser(String username, Role role) {
		User user = new User();
		user.setUsername(username);
		user.setPassword("test-password");
		user.setRole(role);
		return userRepository.save(user);
	}

	private String uniqueUsername() {
		return "test-" + UUID.randomUUID().toString().substring(0, 8);
	}

	// Sets up an authenticated identity for service-level authorization tests.
	private void authenticate(String username, String role) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						username,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_" + role))));
	}
}
