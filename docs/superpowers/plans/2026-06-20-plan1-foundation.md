# Spring-WbsScaff Plan 1: Foundation & Auth

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 Spring Boot 應用骨架，實作認證與使用者管理，產出可登入、ADMIN 可管理使用者的完整後台系統。

**Architecture:** Spring Boot 3.4 模組化單體，Thymeleaf fragments（header/sidebar/footer）+ Vue 3 CDN 前端，Spring Security + Spring Session JDBC 驗證，PostgreSQL 儲存 Session。

**Tech Stack:** Java 21, Spring Boot 3.4, Maven, PostgreSQL 16, Spring Security 6, Spring Session JDBC, Thymeleaf 3, Vue 3 CDN（本地靜態檔）, Lombok, Docker Compose

## Global Constraints

- Java 21（Virtual Threads 可用）
- Spring Boot 3.4.x，Maven（pom.xml）
- 基礎套件：`com.wbsscaff`
- Vue 3 存於 `src/main/resources/static/js/vue.global.prod.min.js`（離線可用）
- HttpOnly + SameSite=Strict Cookie（Spring Session），無 JWT、無 localStorage token
- BCrypt 雜湊密碼
- Thymeleaf fragments：`fragments/header`、`fragments/sidebar`、`fragments/footer`
- 測試用 H2 in-memory DB，profile 名稱 `test`

---

### Task 1: Maven 骨架 + Docker Compose

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/wbsscaff/WbsScaffApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `docker-compose.yml`
- Create: `Dockerfile`
- Create: `.env.example`

**Interfaces:**
- Produces: Spring Boot app 啟動並連接 PostgreSQL，Spring Session JDBC 資料表自動建立

- [ ] **Step 1: 建立 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
    </parent>
    <groupId>com.wbsscaff</groupId>
    <artifactId>spring-wbsscaff</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Spring-WbsScaff</name>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity6</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 建立 WbsScaffApplication.java**

```java
package com.wbsscaff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WbsScaffApplication {
    public static void main(String[] args) {
        SpringApplication.run(WbsScaffApplication.class, args);
    }
}
```

- [ ] **Step 3: 建立 application.yml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wbsscaff
    username: wbsscaff
    password: ${DB_PASSWORD:wbsscaff}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always
  thymeleaf:
    cache: false
server:
  port: 8080
logging:
  level:
    com.wbsscaff: INFO
```

- [ ] **Step 4: 建立 application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  session:
    store-type: none
```

- [ ] **Step 5: 建立 docker-compose.yml**

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/wbsscaff
      SPRING_DATASOURCE_USERNAME: wbsscaff
      DB_PASSWORD: ${DB_PASSWORD}

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: wbsscaff
      POSTGRES_USER: wbsscaff
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wbsscaff"]
      interval: 5s
      timeout: 5s
      retries: 5
    ports:
      - "5432:5432"

volumes:
  pgdata:
```

- [ ] **Step 6: 建立 Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 7: 建立 .env.example**

```
DB_PASSWORD=change_me_in_production
```

- [ ] **Step 8: 啟動 DB 確認連線**

```bash
cp .env.example .env
docker-compose up -d db
mvn spring-boot:run
```

Expected: App 啟動於 port 8080，Spring Session JDBC 資料表自動建立。

- [ ] **Step 9: Commit**

```bash
git add pom.xml docker-compose.yml Dockerfile .env.example \
  src/main/java/com/wbsscaff/WbsScaffApplication.java \
  src/main/resources/application.yml \
  src/test/resources/application-test.yml
git commit -m "chore: init Spring Boot 3.4 project scaffold with Docker Compose"
```

---

### Task 2: Department + User Entity

**Files:**
- Create: `src/main/java/com/wbsscaff/department/Department.java`
- Create: `src/main/java/com/wbsscaff/department/DepartmentRepository.java`
- Create: `src/main/java/com/wbsscaff/user/User.java`
- Create: `src/main/java/com/wbsscaff/user/UserRepository.java`
- Create: `src/test/java/com/wbsscaff/user/UserRepositoryTest.java`

**Interfaces:**
- Produces:
  - `UserRepository.findByEmail(String): Optional<User>`
  - `UserRepository.findByEnabledTrue(): List<User>`
  - `DepartmentRepository` 標準 CRUD

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;

    @Test
    void findByEmail_returnsUser() {
        Department dept = new Department();
        dept.setName("資訊部");
        departmentRepository.save(dept);

        User user = new User();
        user.setEmail("admin@example.com");
        user.setPasswordHash("hashed");
        user.setDisplayName("Admin");
        user.setDepartment(dept);
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("admin@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Admin");
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=UserRepositoryTest -q
```
Expected: FAIL — entities 尚未建立。

- [ ] **Step 3: 建立 Department.java**

```java
package com.wbsscaff.department;

import com.wbsscaff.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Getter @Setter
public class Department {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 建立 DepartmentRepository.java**

```java
package com.wbsscaff.department;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
```

- [ ] **Step 5: 建立 User.java**

```java
package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 200)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.MEMBER;

    private boolean canCreateProject = false;
    private boolean enabled = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Role { ADMIN, MEMBER }
}
```

- [ ] **Step 6: 建立 UserRepository.java**

```java
package com.wbsscaff.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByEnabledTrue();
    List<User> findByDepartmentId(Long departmentId);
}
```

- [ ] **Step 7: Run test — 確認通過**

```bash
mvn test -Dtest=UserRepositoryTest -q
```
Expected: BUILD SUCCESS，1 test passed。

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/wbsscaff/department/ \
        src/main/java/com/wbsscaff/user/ \
        src/test/java/com/wbsscaff/user/UserRepositoryTest.java
git commit -m "feat: add Department and User JPA entities with repositories"
```

---

### Task 3: Spring Security + Spring Session

**Files:**
- Create: `src/main/java/com/wbsscaff/auth/CustomUserDetailsService.java`
- Create: `src/main/java/com/wbsscaff/auth/SecurityConfig.java`
- Create: `src/test/java/com/wbsscaff/auth/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `UserRepository.findByEmail(String)`
- Produces:
  - `CustomUserDetailsService` implements `UserDetailsService`
  - `PasswordEncoder` bean（BCrypt）
  - `/login` public；`/admin/**` 需 ROLE_ADMIN；其餘需登入

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;

    @Test
    void unauthenticatedRequest_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/projects"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=SecurityConfigTest -q
```
Expected: FAIL — `/login` 回傳 404。

- [ ] **Step 3: 建立 CustomUserDetailsService.java**

```java
package com.wbsscaff.auth;

import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPasswordHash(),
            user.isEnabled(), true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
```

- [ ] **Step 4: 建立 SecurityConfig.java**

```java
package com.wbsscaff.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/projects", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("SESSION")
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=SecurityConfigTest -q
```
Expected: BUILD SUCCESS，2 tests passed。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/auth/ \
        src/test/java/com/wbsscaff/auth/SecurityConfigTest.java
git commit -m "feat: configure Spring Security with Spring Session JDBC and BCrypt"
```

---

### Task 4: Auth Controller + Thymeleaf Layout

**Files:**
- Create: `src/main/java/com/wbsscaff/auth/AuthController.java`
- Create: `src/main/resources/templates/fragments/header.html`
- Create: `src/main/resources/templates/fragments/sidebar.html`
- Create: `src/main/resources/templates/fragments/footer.html`
- Create: `src/main/resources/templates/auth/login.html`
- Create: `src/main/resources/templates/error/403.html`
- Create: `src/main/resources/static/css/app.css`
- Create: `src/test/java/com/wbsscaff/auth/AuthControllerTest.java`

**Interfaces:**
- Produces: GET `/login` → `auth/login` view；登入成功跳轉 `/projects`

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.auth;

import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("admin@test.com");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setDisplayName("Admin");
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
    }

    @Test
    void loginPage_renders() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"));
    }

    @Test
    void validCredentials_redirectToProjects() throws Exception {
        mockMvc.perform(formLogin("/auth/login").user("admin@test.com").password("password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/projects"));
    }

    @Test
    void invalidCredentials_redirectWithError() throws Exception {
        mockMvc.perform(formLogin("/auth/login").user("admin@test.com").password("wrong"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=AuthControllerTest -q
```
Expected: FAIL — no controller, no template。

- [ ] **Step 3: 建立 AuthController.java**

```java
package com.wbsscaff.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
```

- [ ] **Step 4: 建立 fragments/header.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<body>
<header th:fragment="header">
  <nav class="navbar">
    <div class="navbar-brand">
      <a th:href="@{/projects}">WBS 管理系統</a>
    </div>
    <div class="navbar-user" sec:authorize="isAuthenticated()">
      <span sec:authentication="principal.username"></span>
      <form th:action="@{/auth/logout}" method="post" style="display:inline">
        <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
        <button type="submit" class="btn-link">登出</button>
      </form>
    </div>
  </nav>
</header>
</body>
</html>
```

- [ ] **Step 5: 建立 fragments/sidebar.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<body>
<nav th:fragment="sidebar" class="sidebar">
  <ul>
    <li><a th:href="@{/projects}">專案列表</a></li>
    <li><a th:href="@{/templates}">模板管理</a></li>
    <li sec:authorize="hasRole('ADMIN')">
      <a th:href="@{/admin/users}">使用者管理</a>
    </li>
  </ul>
</nav>
</body>
</html>
```

- [ ] **Step 6: 建立 fragments/footer.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<footer th:fragment="footer" class="footer">
  <p>WBS 管理系統 &copy; 2026</p>
</footer>
</body>
</html>
```

- [ ] **Step 7: 建立 auth/login.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head>
  <meta charset="UTF-8">
  <title>登入 - WBS 管理系統</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body class="login-page">
<div class="login-box">
  <h1>WBS 管理系統</h1>
  <div th:if="${param.error}" class="alert alert-error">帳號或密碼錯誤</div>
  <div th:if="${param.logout}" class="alert alert-info">已成功登出</div>
  <form th:action="@{/auth/login}" method="post">
    <div class="form-group">
      <label for="username">電子郵件</label>
      <input type="email" id="username" name="username" required autofocus>
    </div>
    <div class="form-group">
      <label for="password">密碼</label>
      <input type="password" id="password" name="password" required>
    </div>
    <button type="submit" class="btn btn-primary">登入</button>
  </form>
</div>
</body>
</html>
```

- [ ] **Step 8: 建立 error/403.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head><meta charset="UTF-8"><title>403</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <div class="layout">
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    <main class="main-content">
      <h2>403 — 您沒有存取此頁面的權限</h2>
      <a th:href="@{/projects}">返回首頁</a>
    </main>
  </div>
  <div th:replace="~{fragments/footer :: footer}"></div>
</body>
</html>
```

- [ ] **Step 9: 建立 static/css/app.css**

```css
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f6fa; color: #2d3436; }
.navbar { display: flex; justify-content: space-between; align-items: center; padding: 0 1.5rem; height: 56px; background: #2d3436; color: #fff; }
.navbar a { color: #fff; text-decoration: none; font-weight: 600; font-size: 1.1rem; }
.navbar-user { display: flex; align-items: center; gap: 1rem; font-size: 0.9rem; }
.btn-link { background: none; border: none; color: #fff; cursor: pointer; font-size: 0.9rem; text-decoration: underline; }
.layout { display: flex; min-height: calc(100vh - 56px - 48px); }
.sidebar { width: 220px; background: #fff; border-right: 1px solid #dfe6e9; padding: 1rem 0; flex-shrink: 0; }
.sidebar ul { list-style: none; }
.sidebar ul li a { display: block; padding: 0.75rem 1.5rem; color: #2d3436; text-decoration: none; }
.sidebar ul li a:hover { background: #f5f6fa; color: #0984e3; }
.main-content { flex: 1; padding: 2rem; }
.footer { height: 48px; background: #dfe6e9; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; color: #636e72; }
.login-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; }
.login-box { background: #fff; padding: 2.5rem; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); width: 380px; }
.login-box h1 { text-align: center; margin-bottom: 1.5rem; font-size: 1.4rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.4rem; font-size: 0.9rem; font-weight: 500; }
.form-group input, .form-group select { width: 100%; padding: 0.6rem 0.8rem; border: 1px solid #b2bec3; border-radius: 4px; font-size: 0.95rem; }
.form-group input:focus, .form-group select:focus { outline: none; border-color: #0984e3; }
.btn { padding: 0.6rem 1.2rem; border: 1px solid #b2bec3; border-radius: 4px; cursor: pointer; font-size: 0.95rem; background: #fff; }
.btn-primary { background: #0984e3; color: #fff; border-color: #0984e3; width: 100%; margin-top: 0.5rem; }
.btn-primary:hover { background: #0773c5; }
.btn-sm { padding: 0.3rem 0.6rem; font-size: 0.8rem; margin-right: 0.25rem; }
.btn-danger { border-color: #e17055; color: #e17055; }
.btn-danger:hover { background: #e17055; color: #fff; }
.alert { padding: 0.75rem 1rem; border-radius: 4px; margin-bottom: 1rem; font-size: 0.9rem; }
.alert-error { background: #ffe0e0; color: #c0392b; }
.alert-info { background: #e0f0ff; color: #2980b9; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.data-table th { background: #f8f9fa; font-weight: 600; color: #636e72; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #fff; border-radius: 8px; padding: 2rem; width: 440px; box-shadow: 0 8px 32px rgba(0,0,0,0.15); }
.modal h3 { margin-bottom: 1.5rem; font-size: 1.1rem; }
.modal-actions { display: flex; gap: 0.75rem; margin-top: 1.5rem; }
```

- [ ] **Step 10: Run test — 確認通過**

```bash
mvn test -Dtest=AuthControllerTest -q
```
Expected: BUILD SUCCESS，3 tests passed。

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/wbsscaff/auth/AuthController.java \
        src/main/resources/templates/ \
        src/main/resources/static/css/app.css \
        src/test/java/com/wbsscaff/auth/AuthControllerTest.java
git commit -m "feat: add login page and Thymeleaf layout fragments"
```

---

### Task 5: UserService + UserController

**Files:**
- Create: `src/main/java/com/wbsscaff/common/ApiResponse.java`
- Create: `src/main/java/com/wbsscaff/user/UserDto.java`
- Create: `src/main/java/com/wbsscaff/user/UserService.java`
- Create: `src/main/java/com/wbsscaff/user/UserController.java`
- Create: `src/test/java/com/wbsscaff/user/UserServiceTest.java`
- Create: `src/test/java/com/wbsscaff/user/UserControllerTest.java`

**Interfaces:**
- Consumes: `UserRepository`, `DepartmentRepository`, `PasswordEncoder`
- Produces:
  - `UserService.createUser(UserDto.CreateRequest): User`
  - `UserService.listUsers(): List<User>`
  - `UserService.updateUser(Long, UserDto.UpdateRequest): User`
  - `UserService.disableUser(Long): void`
  - `UserService.setCanCreateProject(Long, boolean): void`
  - REST: GET/POST/PUT/DELETE `/api/users`，PATCH `/api/users/{id}/can-create-project`

- [ ] **Step 1: 建立 ApiResponse.java**

```java
package com.wbsscaff.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
```

- [ ] **Step 2: 建立 UserDto.java**

```java
package com.wbsscaff.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class UserDto {

    @Data
    public static class CreateRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
        @NotBlank private String displayName;
        private Long departmentId;
        @NotNull private User.Role role;
    }

    @Data
    public static class UpdateRequest {
        private String displayName;
        private Long departmentId;
        private User.Role role;
    }

    @Data
    public static class Response {
        private Long id;
        private String email;
        private String displayName;
        private String departmentName;
        private User.Role role;
        private boolean canCreateProject;
        private boolean enabled;

        public static Response from(User user) {
            Response r = new Response();
            r.id = user.getId();
            r.email = user.getEmail();
            r.displayName = user.getDisplayName();
            r.departmentName = user.getDepartment() != null ? user.getDepartment().getName() : null;
            r.role = user.getRole();
            r.canCreateProject = user.isCanCreateProject();
            r.enabled = user.isEnabled();
            return r;
        }
    }
}
```

- [ ] **Step 3: 寫 failing UserService test**

```java
package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        deptRepository.deleteAll();
        Department dept = new Department();
        dept.setName("資訊部");
        deptRepository.save(dept);
    }

    @Test
    void createUser_savesWithHashedPassword() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("user@test.com");
        req.setPassword("secret123");
        req.setDisplayName("測試使用者");
        req.setRole(User.Role.MEMBER);

        User created = userService.createUser(req);

        assertThat(created.getId()).isNotNull();
        assertThat(passwordEncoder.matches("secret123", created.getPasswordHash())).isTrue();
    }

    @Test
    void setCanCreateProject_updatesFlag() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("pm@test.com");
        req.setPassword("pass");
        req.setDisplayName("PM");
        req.setRole(User.Role.MEMBER);
        User user = userService.createUser(req);

        userService.setCanCreateProject(user.getId(), true);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isCanCreateProject()).isTrue();
    }

    @Test
    void disableUser_setsEnabledFalse() {
        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setEmail("old@test.com");
        req.setPassword("pass");
        req.setDisplayName("舊員工");
        req.setRole(User.Role.MEMBER);
        User user = userService.createUser(req);

        userService.disableUser(user.getId());

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isEnabled()).isFalse();
    }
}
```

- [ ] **Step 4: Run failing test**

```bash
mvn test -Dtest=UserServiceTest -q
```
Expected: FAIL — `UserService` 不存在。

- [ ] **Step 5: 建立 UserService.java**

```java
package com.wbsscaff.user;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserDto.CreateRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("電子郵件已存在：" + req.getEmail());
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setRole(req.getRole());
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }

    public List<User> listUsers() {
        return userRepository.findByEnabledTrue();
    }

    @Transactional
    public User updateUser(Long id, UserDto.UpdateRequest req) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName());
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void setCanCreateProject(Long id, boolean value) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        user.setCanCreateProject(value);
        userRepository.save(user);
    }
}
```

- [ ] **Step 6: 建立 UserController.java**

```java
package com.wbsscaff.user;

import com.wbsscaff.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String usersPage() {
        return "admin/users";
    }

    @GetMapping("/api/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDto.Response>> listUsers() {
        return ApiResponse.ok(userService.listUsers().stream()
            .map(UserDto.Response::from).toList());
    }

    @PostMapping("/api/users")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto.Response> createUser(@Valid @RequestBody UserDto.CreateRequest req) {
        return ApiResponse.ok(UserDto.Response.from(userService.createUser(req)));
    }

    @PutMapping("/api/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto.Response> updateUser(
            @PathVariable Long id, @RequestBody UserDto.UpdateRequest req) {
        return ApiResponse.ok(UserDto.Response.from(userService.updateUser(id, req)));
    }

    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/users/{id}/can-create-project")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> setCanCreateProject(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        userService.setCanCreateProject(id, body.get("value"));
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 7: 建立 UserControllerTest.java**

```java
package com.wbsscaff.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void listUsers_asMember_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 8: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS，所有 tests 通過。

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/wbsscaff/user/ \
        src/main/java/com/wbsscaff/common/ApiResponse.java \
        src/test/java/com/wbsscaff/user/
git commit -m "feat: add UserService, UserController and user management API"
```

---

### Task 6: 使用者管理頁面（Thymeleaf + Vue 3）

**Files:**
- Create: `src/main/resources/static/js/vue.global.prod.min.js`
- Create: `src/main/resources/templates/admin/users.html`

**Interfaces:**
- Consumes: GET/POST/PUT/DELETE `/api/users`，PATCH `/api/users/{id}/can-create-project`
- Produces: ADMIN 可 CRUD 使用者的完整前端頁面

- [ ] **Step 1: 下載 Vue 3 本地檔案**

```bash
curl -L "https://unpkg.com/vue@3/dist/vue.global.prod.js" \
  -o src/main/resources/static/js/vue.global.prod.min.js
```

- [ ] **Step 2: 建立 admin/users.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head>
  <meta charset="UTF-8">
  <title>使用者管理</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <div class="layout">
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    <main class="main-content" id="app">
      <div class="page-header">
        <h2>使用者管理</h2>
        <button class="btn btn-primary" style="width:auto" @click="openCreate">新增使用者</button>
      </div>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <table class="data-table" v-if="users.length">
        <thead>
          <tr>
            <th>姓名</th><th>電子郵件</th><th>部門</th>
            <th>角色</th><th>可建專案</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.displayName }}</td>
            <td>{{ u.email }}</td>
            <td>{{ u.departmentName || '-' }}</td>
            <td>{{ u.role === 'ADMIN' ? '系統管理員' : '一般成員' }}</td>
            <td>
              <input type="checkbox" :checked="u.canCreateProject"
                @change="toggleCanCreate(u.id, $event.target.checked)">
            </td>
            <td>
              <button class="btn btn-sm" @click="openEdit(u)">編輯</button>
              <button class="btn btn-sm btn-danger" @click="disable(u.id)">停用</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else style="color:#636e72">尚無使用者</p>

      <div v-if="modal.open" class="modal-overlay" @click.self="closeModal">
        <div class="modal">
          <h3>{{ modal.isEdit ? '編輯使用者' : '新增使用者' }}</h3>
          <div class="form-group">
            <label>電子郵件</label>
            <input v-model="modal.form.email" type="email" :disabled="modal.isEdit">
          </div>
          <div class="form-group" v-if="!modal.isEdit">
            <label>密碼</label>
            <input v-model="modal.form.password" type="password">
          </div>
          <div class="form-group">
            <label>姓名</label>
            <input v-model="modal.form.displayName">
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="modal.form.role">
              <option value="MEMBER">一般成員</option>
              <option value="ADMIN">系統管理員</option>
            </select>
          </div>
          <div class="modal-actions">
            <button class="btn btn-primary" style="width:auto" @click="submit">確認</button>
            <button class="btn" @click="closeModal">取消</button>
          </div>
        </div>
      </div>
    </main>
  </div>
  <div th:replace="~{fragments/footer :: footer}"></div>

  <script th:src="@{/js/vue.global.prod.min.js}"></script>
  <script th:inline="javascript">
    const CSRF_HEADER = /*[[${_csrf.headerName}]]*/ 'X-CSRF-TOKEN';
    const CSRF_TOKEN  = /*[[${_csrf.token}]]*/ '';
  </script>
  <script>
    const { createApp, ref, onMounted } = Vue;
    createApp({
      setup() {
        const users = ref([]);
        const error = ref('');
        const modal = ref({ open: false, isEdit: false, editId: null, form: {} });

        const h = () => ({ 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN });

        async function load() {
          const res = await fetch('/api/users');
          const d = await res.json();
          if (d.success) users.value = d.data;
        }

        function openCreate() {
          modal.value = { open: true, isEdit: false, editId: null,
            form: { email: '', password: '', displayName: '', role: 'MEMBER' } };
        }

        function openEdit(u) {
          modal.value = { open: true, isEdit: true, editId: u.id,
            form: { displayName: u.displayName, role: u.role } };
        }

        function closeModal() { modal.value.open = false; }

        async function submit() {
          const { isEdit, editId, form } = modal.value;
          const res = await fetch(isEdit ? `/api/users/${editId}` : '/api/users', {
            method: isEdit ? 'PUT' : 'POST', headers: h(), body: JSON.stringify(form)
          });
          const d = await res.json();
          if (d.success) { closeModal(); load(); }
          else error.value = d.message;
        }

        async function disable(id) {
          if (!confirm('確定停用此使用者？')) return;
          await fetch(`/api/users/${id}`, { method: 'DELETE', headers: h() });
          load();
        }

        async function toggleCanCreate(id, value) {
          await fetch(`/api/users/${id}/can-create-project`, {
            method: 'PATCH', headers: h(), body: JSON.stringify({ value })
          });
          load();
        }

        onMounted(load);
        return { users, error, modal, openCreate, openEdit, closeModal, submit, disable, toggleCanCreate };
      }
    }).mount('#app');
  </script>
</body>
</html>
```

- [ ] **Step 3: 啟動並手動驗證**

```bash
mvn spring-boot:run
```

瀏覽 `http://localhost:8080/admin/users`，確認：
- 使用者表格正常渲染
- 新增 Modal 可開關並成功送出
- 勾選「可建專案」PATCH 即時生效

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/admin/users.html \
        src/main/resources/static/js/vue.global.prod.min.js
git commit -m "feat: add user management page with Vue 3 inline reactive UI"
```

---

### Task 7: Seed 資料（部門 + 初始 ADMIN）

**Files:**
- Create: `src/main/java/com/wbsscaff/common/DataSeeder.java`

**Interfaces:**
- Produces: 啟動時植入 5 個預設部門與初始 ADMIN 帳號（`admin@wbsscaff.com` / `admin1234`）

- [ ] **Step 1: 建立 DataSeeder.java**

```java
package com.wbsscaff.common;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDepartments();
        seedAdminUser();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) return;
        for (String name : new String[]{ "資訊部", "業務部", "財務部", "人資部", "產品部" }) {
            Department dept = new Department();
            dept.setName(name);
            departmentRepository.save(dept);
        }
        log.info("已植入 5 個預設部門");
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@wbsscaff.com").isPresent()) return;
        User admin = new User();
        admin.setEmail("admin@wbsscaff.com");
        admin.setPasswordHash(passwordEncoder.encode("admin1234"));
        admin.setDisplayName("系統管理員");
        admin.setRole(User.Role.ADMIN);
        admin.setCanCreateProject(true);
        userRepository.save(admin);
        log.info("已建立初始 ADMIN：admin@wbsscaff.com / admin1234");
    }
}
```

- [ ] **Step 2: 重啟並確認 seed**

```bash
mvn spring-boot:run
```

Log 應出現：
```
已植入 5 個預設部門
已建立初始 ADMIN：admin@wbsscaff.com / admin1234
```

- [ ] **Step 3: 完整流程驗證**

1. 瀏覽 `http://localhost:8080/login`
2. 輸入 `admin@wbsscaff.com` / `admin1234`
3. 成功跳轉 `/projects`（目前 404，Plan 2 實作）
4. 瀏覽 `/admin/users` 確認使用者管理頁面正常
5. 登出後確認 Session 清除

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wbsscaff/common/DataSeeder.java
git commit -m "feat: add DataSeeder for default departments and admin account"
```
