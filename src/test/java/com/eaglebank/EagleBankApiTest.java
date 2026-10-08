package com.eaglebank;

import com.eaglebank.model.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Provides support for eagle bank api test.
 *
 * @author mattbateup
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EagleBankApiTest {

    private static final String PASSWORD = "ExamplePassword123!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void createUser_withRequiredData_returns201() throws Exception {
        JsonNode body = json(mvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Ada Lovelace", email())))
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(body.get("id").asText()).startsWith("usr-");
        assertThat(body.get("name").asText()).isEqualTo("Ada Lovelace");
        assertThat(body.get("email").asText()).endsWith("@example.com");
        assertThat(body.get("phoneNumber").asText()).isEqualTo("+447700900123");
        assertThat(body.get("address").get("line1").asText()).isEqualTo("1 Example Road");
        assertThat(body.get("createdTimestamp").asText()).isNotBlank();
        assertThat(body.get("updatedTimestamp").asText()).isEqualTo(body.get("createdTimestamp").asText());
        assertThat(body.toString()).doesNotContain("password");
    }

    @Test
    void createUser_missingRequiredData_returns400() throws Exception {
        JsonNode body = json(mvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andReturn());

        assertThat(body.get("message").asText()).isEqualTo("Invalid details supplied");
        assertThat(body.get("details").isArray()).isTrue();
        assertThat(body.get("details").findValuesAsText("field")).contains("address", "phoneNumber", "email", "password");
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        String email = email();
        mvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(userJson("Ada", email)))
                .andExpect(status().isCreated());

        JsonNode body = json(mvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Ada", email.toUpperCase())))
                .andExpect(status().isConflict())
                .andReturn());

        assertThat(body.get("message").asText()).contains("email");
    }

    @Test
    void login_withValidCredentials_returnsJwt() throws Exception {
        String email = email();
        mvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(userJson("Ada", email)))
                .andExpect(status().isCreated());

        JsonNode body = json(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asInt()).isEqualTo(3600);
        assertThat(body.get("accessToken").asText()).isNotBlank();
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = email();
        mvc.perform(post("/v1/users").contentType(MediaType.APPLICATION_JSON).content(userJson("Ada", email)))
                .andExpect(status().isCreated());

        JsonNode body = json(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "wrong-password-value")))
                .andExpect(status().isUnauthorized())
                .andReturn());

        assertThat(body.get("message").asText()).isEqualTo("Invalid email or password");
    }

    @Test
    void fetchUser_ownDetails_returns200() throws Exception {
        Session session = signup();
        JsonNode body = json(mvc.perform(get("/v1/users/" + session.userId()).header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("id").asText()).isEqualTo(session.userId());
    }

    @Test
    void fetchUser_anotherUser_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        JsonNode body = json(mvc.perform(get("/v1/users/" + second.userId()).header("Authorization", first.bearer()))
                .andExpect(status().isForbidden())
                .andReturn());
        assertThat(body.get("message").asText()).isNotBlank();
    }

    @Test
    void fetchUser_missingUser_returns404() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/users/usr-missinguser1").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchUser_withoutToken_returns401() throws Exception {
        Session session = signup();
        JsonNode body = json(mvc.perform(get("/v1/users/" + session.userId()))
                .andExpect(status().isUnauthorized())
                .andReturn());
        assertThat(body.get("message").asText()).isEqualTo("Access token is missing or invalid");
    }

    @Test
    void fetchUser_withExpiredToken_returns401() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/users/" + session.userId()).header("Authorization", "Bearer " + expiredToken(session.userId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_ownDetails_returnsUpdatedUser() throws Exception {
        Session session = signup();
        JsonNode body = json(mvc.perform(patch("/v1/users/" + session.userId())
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\",\"phoneNumber\":\"+447700900999\"}"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("name").asText()).isEqualTo("Updated Name");
        assertThat(body.get("phoneNumber").asText()).isEqualTo("+447700900999");
        assertThat(body.get("address").get("town").asText()).isEqualTo("Stockport");
    }

    @Test
    void updateUser_anotherUser_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        mvc.perform(patch("/v1/users/" + second.userId())
                        .header("Authorization", first.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden());

        JsonNode unchanged = json(mvc.perform(get("/v1/users/" + second.userId()).header("Authorization", second.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(unchanged.get("name").asText()).isNotEqualTo("Hacked");
    }

    @Test
    void updateUser_missingUser_returns404() throws Exception {
        Session session = signup();
        mvc.perform(patch("/v1/users/usr-missinguser1")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nobody\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_withoutAccount_returns204() throws Exception {
        Session session = signup();
        mvc.perform(delete("/v1/users/" + session.userId()).header("Authorization", session.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/v1/accounts").header("Authorization", session.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_withAccount_returns409() throws Exception {
        Session session = signup();
        openAccount(session);
        JsonNode body = json(mvc.perform(delete("/v1/users/" + session.userId()).header("Authorization", session.bearer()))
                .andExpect(status().isConflict())
                .andReturn());
        assertThat(body.get("message").asText()).contains("bank account");
    }

    @Test
    void deleteUser_afterAccountDeleted_returns204() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        mvc.perform(delete("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/v1/users/" + session.userId()).header("Authorization", session.bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_anotherUser_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        mvc.perform(delete("/v1/users/" + second.userId()).header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_missingUser_returns404() throws Exception {
        Session session = signup();
        mvc.perform(delete("/v1/users/usr-missinguser1").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAccount_withRequiredData_returns201() throws Exception {
        Session session = signup();
        MvcResult result = mvc.perform(post("/v1/accounts")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Personal account\",\"accountType\":\"personal\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json(result);

        assertThat(body.get("accountNumber").asText()).matches("01\\d{6}");
        assertThat(body.get("sortCode").asText()).isEqualTo("10-10-10");
        assertThat(body.get("name").asText()).isEqualTo("Personal account");
        assertThat(body.get("accountType").asText()).isEqualTo("personal");
        assertThat(body.get("currency").asText()).isEqualTo("GBP");
        assertThat(result.getResponse().getContentAsString()).contains("\"balance\":0.00");
    }

    @Test
    void createAccount_missingRequiredData_returns400() throws Exception {
        Session session = signup();
        mvc.perform(post("/v1/accounts")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAccounts_returnsOnlyTheAuthenticatedUsersAccounts() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(first);
        openAccount(second);

        JsonNode body = json(mvc.perform(get("/v1/accounts").header("Authorization", first.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("accounts")).hasSize(1);
        assertThat(body.get("accounts").get(0).get("accountNumber").asText()).isEqualTo(accountNumber);
    }

    @Test
    void fetchAccount_ownAccount_returns200() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        JsonNode body = json(mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("accountNumber").asText()).isEqualTo(accountNumber);
    }

    @Test
    void fetchAccount_anotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchAccount_missingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/accounts/01999999").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchAccount_invalidAccountNumber_returns400() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/accounts/12345678").header("Authorization", session.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_ownAccount_returnsUpdatedAccount() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        MvcResult result = mvc.perform(patch("/v1/accounts/" + accountNumber)
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Holiday fund\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json(result);
        assertThat(body.get("name").asText()).isEqualTo("Holiday fund");
        assertThat(result.getResponse().getContentAsString()).contains("\"balance\":0.00");
    }

    @Test
    void updateAccount_anotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        mvc.perform(patch("/v1/accounts/" + accountNumber)
                        .header("Authorization", first.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAccount_missingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(patch("/v1/accounts/01999999")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAccount_ownAccount_returns204() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        mvc.perform(delete("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAccount_anotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        mvc.perform(delete("/v1/accounts/" + accountNumber).header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAccount_missingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(delete("/v1/accounts/01999999").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deposit_updatesBalanceAndStoresTransaction() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        JsonNode transaction = json(mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.25,\"currency\":\"GBP\",\"type\":\"deposit\",\"reference\":\"Initial deposit\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        assertThat(transaction.get("id").asText()).startsWith("tan-");
        assertThat(transaction.get("amount").asText()).isEqualTo("100.25");
        assertThat(transaction.get("type").asText()).isEqualTo("deposit");
        assertThat(transaction.get("reference").asText()).isEqualTo("Initial deposit");
        assertThat(transaction.get("userId").asText()).isEqualTo(session.userId());

        JsonNode account = json(mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(account.get("balance").asText()).isEqualTo("100.25");
    }

    @Test
    void withdrawal_withSufficientFunds_updatesBalance() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        transact(session, accountNumber, "100.25", "deposit");
        JsonNode transaction = json(mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20.10,\"currency\":\"GBP\",\"type\":\"withdrawal\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        assertThat(transaction.get("type").asText()).isEqualTo("withdrawal");

        JsonNode account = json(mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andReturn());
        assertThat(account.get("balance").asText()).isEqualTo("80.15");
    }

    @Test
    void withdrawal_withInsufficientFunds_returns422AndLeavesBalanceUnchanged() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        transact(session, accountNumber, "10.00", "deposit");

        JsonNode body = json(mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.01,\"currency\":\"GBP\",\"type\":\"withdrawal\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn());
        assertThat(body.get("message").asText()).isEqualTo("Insufficient funds to process transaction");

        MvcResult accountResult = mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andReturn();
        assertThat(accountResult.getResponse().getContentAsString()).contains("\"balance\":10.00");
        JsonNode transactions = json(mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer()))
                .andReturn());
        assertThat(transactions.get("transactions")).hasSize(1);
    }

    @Test
    void deposit_aboveMaximumBalance_returns422() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        transact(session, accountNumber, "6000.00", "deposit");
        mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":6000.00,\"currency\":\"GBP\",\"type\":\"deposit\"}"))
                .andExpect(status().isUnprocessableEntity());

        MvcResult accountResult = mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andReturn();
        assertThat(accountResult.getResponse().getContentAsString()).contains("\"balance\":6000.00");
    }

    @Test
    void createTransaction_onAnotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", first.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"currency\":\"GBP\",\"type\":\"deposit\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_onMissingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(post("/v1/accounts/01999999/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"currency\":\"GBP\",\"type\":\"deposit\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_missingRequiredData_returns400() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        JsonNode body = json(mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GBP\"}"))
                .andExpect(status().isBadRequest())
                .andReturn());
        assertThat(body.get("details").findValuesAsText("field")).contains("amount", "type");
    }

    @Test
    void createTransaction_withTooManyDecimalPlaces_returns400() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.001,\"currency\":\"GBP\",\"type\":\"deposit\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTransactions_ownAccount_returnsTransactions() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        transact(session, accountNumber, "15.00", "deposit");
        transact(session, accountNumber, "5.00", "withdrawal");

        JsonNode body = json(mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("transactions")).hasSize(2);
        assertThat(body.get("transactions").get(0).get("type").asText()).isEqualTo("deposit");
        assertThat(body.get("transactions").get(1).get("type").asText()).isEqualTo("withdrawal");
    }

    @Test
    void listTransactions_anotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions").header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listTransactions_missingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/accounts/01999999/transactions").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchTransaction_ownTransaction_returns200() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        String transactionId = transact(session, accountNumber, "15.50", "deposit");

        JsonNode body = json(mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/" + transactionId)
                        .header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("id").asText()).isEqualTo(transactionId);
        assertThat(new BigDecimal(body.get("amount").asText())).isEqualByComparingTo("15.50");
    }

    @Test
    void fetchTransaction_anotherUsersAccount_returns403() throws Exception {
        Session first = signup();
        Session second = signup();
        String accountNumber = openAccount(second);
        String transactionId = transact(second, accountNumber, "8.00", "deposit");
        mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/" + transactionId)
                        .header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchTransaction_missingAccount_returns404() throws Exception {
        Session session = signup();
        mvc.perform(get("/v1/accounts/01999999/transactions/tan-abc123").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchTransaction_missingTransaction_returns404() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        mvc.perform(get("/v1/accounts/" + accountNumber + "/transactions/tan-missingtx").header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fetchTransaction_onTheWrongAccount_returns404() throws Exception {
        Session session = signup();
        String firstAccount = openAccount(session);
        String secondAccount = openAccount(session);
        String transactionId = transact(session, firstAccount, "3.00", "deposit");
        mvc.perform(get("/v1/accounts/" + secondAccount + "/transactions/" + transactionId)
                        .header("Authorization", session.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void concurrentWithdrawals_cannotOverdraw() throws Exception {
        Session session = signup();
        String accountNumber = openAccount(session);
        transact(session, accountNumber, "50.00", "deposit");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> withdraw = () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                return 0;
            }
            return mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                            .header("Authorization", session.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":40.00,\"currency\":\"GBP\",\"type\":\"withdrawal\"}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
        try {
            Future<Integer> first = pool.submit(withdraw);
            Future<Integer> second = pool.submit(withdraw);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 422);
        } finally {
            pool.shutdownNow();
        }

        MvcResult accountResult = mvc.perform(get("/v1/accounts/" + accountNumber).header("Authorization", session.bearer()))
                .andReturn();
        assertThat(accountResult.getResponse().getContentAsString()).contains("\"balance\":10.00");
    }

    private Session signup() throws Exception {
        String email = email();
        JsonNode created = json(mvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Ada Lovelace", email)))
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode token = json(mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn());
        return new Session(created.get("id").asText(), token.get("accessToken").asText());
    }

    private String openAccount(Session session) throws Exception {
        JsonNode body = json(mvc.perform(post("/v1/accounts")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Personal account\",\"accountType\":\"personal\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        return body.get("accountNumber").asText();
    }

    private String transact(Session session, String accountNumber, String amount, String type) throws Exception {
        JsonNode body = json(mvc.perform(post("/v1/accounts/" + accountNumber + "/transactions")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + ",\"currency\":\"GBP\",\"type\":\"" + type + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());
        return body.get("id").asText();
    }

    private String expiredToken(String userId) {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("eagle-bank")
                .subject(userId)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static String email() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static String userJson(String name, String email) {
        return """
                {"name":"%s","address":{"line1":"1 Example Road","town":"Stockport","county":"Greater Manchester","postcode":"SK1 1AA"},"phoneNumber":"+447700900123","email":"%s","password":"%s"}
                """.formatted(name, email, PASSWORD);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

}
