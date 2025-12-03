# 📘 Guide API REST paginée — Spring Boot 3 / Angular

Ce fichier décrit comment créer une API REST paginée avec Spring Boot 3 + JDK 17 + Angular.

-----------------------------------------------------
SECTION 1 — DÉPENDANCES MAVEN
-----------------------------------------------------

Fichier : pom.xml (extrait)

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

application.properties :

spring.datasource.url=jdbc:postgresql://localhost:5432/ma_base
spring.datasource.username=monuser
spring.datasource.password=monpassword
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true


-----------------------------------------------------
SECTION 2 — ENTITÉ JPA
-----------------------------------------------------

Fichier : User.java

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


-----------------------------------------------------
SECTION 3 — REPOSITORY
-----------------------------------------------------

Fichier : UserRepository.java

package com.example.demo.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}


-----------------------------------------------------
SECTION 4 — SERVICE
-----------------------------------------------------

Fichier : UserService.java

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


-----------------------------------------------------
SECTION 5 — CONTROLLER
-----------------------------------------------------

Fichier : UserController.java

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


-----------------------------------------------------
SECTION 6 — EXEMPLE RÉPONSE JSON
-----------------------------------------------------

Exemple de retour JSON :

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


-----------------------------------------------------
SECTION 7 — ANGULAR SERVICE
-----------------------------------------------------

Fichier : user.service.ts

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

@Injectable({ providedIn: 'root' })
export class UserService {
  private base = '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(page: number, size: number) {
    return this.http.get<Page<User>>(this.base, {
      params: { page, size }
    });
  }
}


-----------------------------------------------------
SECTION 8 — ANGULAR COMPONENT
-----------------------------------------------------

Fichier : user-list.component.ts

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


-----------------------------------------------------
SECTION 9 — BONNES PRATIQUES
-----------------------------------------------------

✔ Pagination toujours côté backend  
✔ Limiter size max  
✔ Ajouter des index DB  
✔ DTO allégés pour les grosses entités  
✔ Activer GZIP dans application.properties :

server.compression.enabled=true
server.compression.mime-types=application/json
server.compression.min-response-size=1024


-----------------------------------------------------
SECTION 10 — PIÈGES CLASSIQUES
-----------------------------------------------------

❌ Paginer seulement côté Angular  
❌ Renvoyer 10k lignes en un seul JSON  
❌ Utiliser List<T> au lieu de Page<T>  
❌ Faire des filtres non indexés  
❌ Faire size = 50000 (abusé)  


-----------------------------------------------------
FIN DU DOCUMENT
-----------------------------------------------------