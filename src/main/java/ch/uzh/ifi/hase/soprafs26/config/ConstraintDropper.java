package ch.uzh.ifi.hase.soprafs26.config;

import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConstraintDropper {

    private final DataSource dataSource;

    public ConstraintDropper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void dropConstraint() {
        try (Statement stmt = dataSource.getConnection().createStatement()) {
            stmt.execute("ALTER TABLE game_event DROP CONSTRAINT IF EXISTS constraint_8c");
            stmt.execute("ALTER TABLE GAME_EVENT DROP CONSTRAINT IF EXISTS CONSTRAINT_8C");
            System.out.println("Successfully dropped constraint_8c");
        } catch (Exception e) {
            System.out.println("Could not drop constraint (may not exist): " + e.getMessage());
        }
    }
}