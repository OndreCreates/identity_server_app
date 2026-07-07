package com.ondrecreates.identityserver.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Direct SQL access to oauth2_registered_client for admin listing/deletion.
 * {@link RegisteredClientRepository} deliberately has no findAll()/delete() -- it's
 * scoped to the OAuth2 runtime's lookup-by-id needs, not admin tooling -- so this DAO
 * covers what that interface doesn't.
 */
@Repository
public class RegisteredClientAdminDao {

    private final JdbcTemplate jdbcTemplate;

    public RegisteredClientAdminDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ClientSummary> findAll() {
        return jdbcTemplate.query(
                """
                SELECT client_id, client_authentication_methods, authorization_grant_types,
                       redirect_uris, scopes
                FROM oauth2_registered_client
                ORDER BY client_id
                """,
                (rs, rowNum) -> new ClientSummary(
                        rs.getString("client_id"),
                        rs.getString("client_authentication_methods"),
                        rs.getString("authorization_grant_types"),
                        rs.getString("redirect_uris"),
                        rs.getString("scopes")
                )
        );
    }

    /** Also removes authorizations/consents issued for the client, so nothing is left pointing at a dead client_id. */
    public void deleteByClientId(String clientId) {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM oauth2_registered_client WHERE client_id = ?", String.class, clientId);
        Optional<String> id = ids.stream().findFirst();
        if (id.isEmpty()) {
            return;
        }

        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id.get());
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id.get());
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
    }
}
