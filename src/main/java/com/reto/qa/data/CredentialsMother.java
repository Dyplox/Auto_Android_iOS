package com.reto.qa.data;

/**
 * Object Mother on top of the Builder: a catalog of test data with business names.
 * The data is deterministic on purpose (the R in FIRST: same input, same result on every run).
 */
public final class CredentialsMother {

    private static final String VALID_EMAIL = "qa.automation@webdriver.io";
    private static final String VALID_PASSWORD = "Test1234!";

    private CredentialsMother() {
    }

    /**
     * Returns credentials that pass the app's validation.
     *
     * @return valid credentials
     */
    public static UserCredentials valid() {
        return UserCredentials.builder()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .build();
    }

    /**
     * Returns valid credentials with a malformed email.
     *
     * @return credentials with an invalid email
     */
    public static UserCredentials withInvalidEmail() {
        return valid().toBuilder().email("qa.automation@").build();
    }

    /**
     * Returns valid credentials with a password shorter than 8 characters (and a matching confirmation).
     *
     * @return credentials with a short password
     */
    public static UserCredentials withShortPassword() {
        return valid().toBuilder().password("1234").passwordConfirmation("1234").build();
    }
}
