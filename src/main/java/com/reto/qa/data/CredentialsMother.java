package com.reto.qa.data;

/**
 * Object Mother sobre el Builder: catálogo de datos con nombre de negocio.
 * Los datos son deterministas a propósito (la R de FIRST: mismo input, mismo resultado en cada corrida).
 */
public final class CredentialsMother {

    private static final String VALID_EMAIL = "qa.automation@webdriver.io";
    private static final String VALID_PASSWORD = "Test1234!";

    private CredentialsMother() {
    }

    public static UserCredentials valid() {
        return UserCredentials.builder()
                .email(VALID_EMAIL)
                .password(VALID_PASSWORD)
                .build();
    }

    public static UserCredentials withInvalidEmail() {
        return valid().toBuilder().email("qa.automation@").build();
    }

    public static UserCredentials withShortPassword() {
        return valid().toBuilder().password("1234").passwordConfirmation("1234").build();
    }
}
