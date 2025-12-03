# 📘 Guide API REST paginée — Spring Boot 3 / Angular

Ce fichier décrit comment créer une API REST paginée avec Spring Boot 3 + JDK 17 + Angular.

---

## ✅ 1. Dépendances Maven

Fichier : `pom.xml`

\`\`\`xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
\`\`\`

---

## Configuration DB (`application.properties`)

\`\`\`properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ma_base
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
\`\`\`

---

## ✅ 2. Entité JPA — `User.java`

\`\`\`java
package com.example.demo.user;

import jakarta.persistence.*;

@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

    public User() {}

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
}
\`\`\`

---

## ✅ 3. Repository — `UserRepository.java`

\`\`\`java
package com.example.demo.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
\`\`\`

---

## ✅ 4. Service — `UserService.java`

\`\`\`java
package com.example.demo.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Page<User> getUsers(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }

    public Page<User> searchUsers(String username, int page, int size) {
        return repo.findByUsernameContainingIgnoreCase(username, PageRequest.of(page, size));
    }
}
\`\`\`

---

## ✅ 5. Controller — `UserController.java`

\`\`\`java
package com.example.demo.user;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok(service.getUsers(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<User>> search(
            @RequestParam String username,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok(service.searchUsers(username, page, size));
    }
}
\`\`\`

---

## ✅ 6. Exemple de JSON retourné

\`\`\`json
{
  "content": [
    { "id": 1, "username": "hamdi", "email": "h@example.com" },
    { "id": 2, "username": "salma", "email": "s@example.com" }
  ],
  "totalElements": 1543,
  "totalPages": 155,
  "size": 10,
  "number": 0
}
\`\`\`

---

## ✅ 7. Angular — Interface + Service

### interface `Page<T>` et `User`

\`\`\`ts
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface User {
  id: number;
  username: string;
  email: string;
}
\`\`\`

### Service Angular — `user.service.ts`

\`\`\`ts
@Injectable({ providedIn: 'root' })
export class UserService {

  private baseUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(page: number, size: number) {
    return this.http.get<Page<User>>(this.baseUrl, {
      params: { page, size }
    });
  }
}
\`\`\`

---

## ✅ 8. Angular Component — `user-list.component.ts`

\`\`\`ts
export class UserListComponent {

  users: User[] = [];
  total = 0;
  pageIndex = 0;
  pageSize = 10;

  constructor(private service: UserService) {}

  ngOnInit() {
    this.load(0);
  }

  load(page: number) {
    this.service.getUsers(page, this.pageSize).subscribe(p => {
      this.users = p.content;
      this.total = p.totalElements;
      this.pageIndex = p.number;
    });
  }
}
\`\`\`

---

## 🎯 9. Bonnes pratiques

- Pagination **obligatoire** côté backend  
- Limiter size (10 / 20 / 50)  
- Ajouter index SQL sur les colonnes filtrées  
- Utiliser DTO pour éviter surcharge JSON  
- Activer compression GZIP :

\`\`\`properties
server.compression.enabled=true
server.compression.mime-types=application/json
server.compression.min-response-size=1024
\`\`\`

---

## 🚫 10. Pièges à éviter

- ❌ Paginer seulement côté Angular  
- ❌ Envoyer 50 000 lignes en une seule réponse  
- ❌ Ne pas indexer les colonnes filtrées  
- ❌ Utiliser List<T> au lieu de Page<T>

---

## 🏁 Fin du document