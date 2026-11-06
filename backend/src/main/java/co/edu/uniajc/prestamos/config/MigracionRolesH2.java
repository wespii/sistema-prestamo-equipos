package co.edu.uniajc.prestamos.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

/** Amplía el esquema H2 anterior antes de que Hibernate o la semilla consulten roles. */
@Component
public class MigracionRolesH2 implements HibernatePropertiesCustomizer {
    private final DataSource dataSource;

    public MigracionRolesH2(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        try (Connection connection = dataSource.getConnection()) {
            if (!"H2".equals(connection.getMetaData().getDatabaseProductName())) return;
            try (ResultSet columns = connection.getMetaData().getColumns(null, null, "ROLES", "CODIGO")) {
                if (!columns.next() || columns.getInt("DATA_TYPE") == Types.VARCHAR) return;
            }
            // H2 conservaba un ENUM con solo los tres roles originales. VARCHAR permite
            // añadir perfiles sin tocar las claves foráneas ni el historial existente.
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE roles ALTER COLUMN codigo VARCHAR(30)");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo migrar el esquema de roles existente", exception);
        }
    }
}
