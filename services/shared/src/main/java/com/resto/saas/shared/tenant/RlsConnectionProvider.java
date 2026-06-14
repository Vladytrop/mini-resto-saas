package com.resto.saas.shared.tenant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
@AllArgsConstructor
public class RlsConnectionProvider implements MultiTenantConnectionProvider<String> {
    private final DataSource dataSource;


    @Override
    public Connection getAnyConnection() throws SQLException {
        log.info(">>> getAnyConnection called!");
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.info(">>> getConnection called with tenant: '{}'", tenantIdentifier);
        Connection connection = dataSource.getConnection();
        setTenant(connection, tenantIdentifier);
        log.debug("Row Level Security: установлен tenant {}", tenantIdentifier);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetTenant(connection);
        log.debug("Row Level Security: сброшен tenant {}", tenantIdentifier);
        connection.close();
    }

    private void setTenant(Connection connection, String tenantIdentifier) throws SQLException {
        log.info(">>> Setting tenant to: {}", tenantIdentifier);
        try (PreparedStatement ps = connection.prepareStatement(
                "select set_config('app.current_tenant', ?, false)")) {
            ps.setString(1, tenantIdentifier);
            ps.execute();
        }
    }

    private void resetTenant(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select set_config('app.current_tenant', '', false)")) {
            ps.execute();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
