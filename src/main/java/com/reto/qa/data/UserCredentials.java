package com.reto.qa.data;

import java.util.Objects;

/**
 * Datos de prueba inmutables construidos con Builder. {@link #toString()} enmascara la contraseña para que
 * nunca termine en logs ni en el reporte de Allure.
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

    public static Builder builder() {
        return new Builder();
    }

    /** Copia editable: permite derivar variantes ("la válida, pero con otro email"). */
    public Builder toBuilder() {
        return new Builder().email(email).password(password).passwordConfirmation(passwordConfirmation);
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

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

    public static final class Builder {

        private String email;
        private String password;
        private String passwordConfirmation;

        private Builder() {
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /** Opcional: por defecto es igual a la contraseña. */
        public Builder passwordConfirmation(String passwordConfirmation) {
            this.passwordConfirmation = passwordConfirmation;
            return this;
        }

        public UserCredentials build() {
            Objects.requireNonNull(email, "email es obligatorio");
            Objects.requireNonNull(password, "password es obligatorio");
            return new UserCredentials(this);
        }
    }
}
