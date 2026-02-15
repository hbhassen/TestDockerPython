import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.DriverManager;

@SpringBootTest
class IT_WithEmbeddedPostgres {

  static EmbeddedPostgres pg;

  @BeforeAll
  static void startPg() throws Exception {
    pg = EmbeddedPostgres.start(); // démarre un vrai postgres local (port aléatoire)
  }

  @AfterAll
  static void stopPg() throws Exception {
    if (pg != null) pg.close();
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    String url = pg.getJdbcUrl("postgres", "postgres"); // db=postgres user=postgres
    r.add("spring.datasource.url", () -> url);
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "postgres");
  }

  // Tes @Test ici
}
*/
<dependency>
  <groupId>io.zonky.test</groupId>
  <artifactId>embedded-postgres</artifactId>
  <scope>test</scope>
</dependency>
*/