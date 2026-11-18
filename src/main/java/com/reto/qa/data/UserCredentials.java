package com.reto.qa.data;

import java.util.Objects;

/**
 * Immutable test data built with a Builder. {@link #toString()} masks the password so it never ends up in logs or in
 * the Allure report.
 */
public final class UserCredentials {

    private final String email;
    private final String password;
    private final String passwordConfirmation;

    private UserCredentials(Builder builder) {
        this.email = builder.email;
        this.password = builder.password;
        this.passwordConfirmation = builder.passwordConfirmation != null ? builder.passwordConfirmation : builder.password;
    }

    /**
     * Starts building credentials.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an editable copy, so variants can be derived ("the valid one, but with another email").
     *
     * @return a builder pre-filled with these values
     */
    public Builder toBuilder() {
        return new Builder().email(email).password(password).passwordConfirmation(passwordConfirmation);
    }

    /**
     * Returns the email.
     *
     * @return the email
     */
    public String email() {
        return email;
    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    public String password() {
        return password;
    }

    /**
     * Returns the password confirmation (the password itself unless set explicitly).
     *
     * @return the password confirmation
     */
    public String passwordConfirmation() {
        return passwordConfirmation;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserCredentials that)) {
            return false;
        }
        return email.equals(that.email)
                && password.equals(that.password)
                && passwordConfirmation.equals(that.passwordConfirmation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password, passwordConfirmation);
    }

    @Override
    public String toString() {
        return "UserCredentials{email='" + email + "', password='" + "*".repeat(password.length()) + "'}";
    }

    /** Fluent builder for {@link UserCredentials}. */
    public static final class Builder {

        private String email;
        private String password;
        private String passwordConfirmation;

        private Builder() {
        }

        /**
         * Sets the email (required).
         *
         * @param email the email
         * @return this builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the password (required).
         *
         * @param password the password
         * @return this builder
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the password confirmation. Optional: defaults to the password.
         *
         * @param passwordConfirmation the confirmation
         * @return this builder
         */
        public Builder passwordConfirmation(String passwordConfirmation) {
            this.passwordConfirmation = passwordConfirmation;
            return this;
        }

        /**
         * Builds the credentials.
         *
         * @return the immutable credentials
         * @throws NullPointerException if the email or password is missing
         */
        public UserCredentials build() {
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(password, "password is required");
            return new UserCredentials(this);
        }
    }
}
