package com.reto.qa.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCredentialsTest {

    @Test
    void confirmationDefaultsToPassword() {
        UserCredentials credentials = UserCredentials.builder().email("a@b.co").password("Secret123").build();

        assertThat(credentials.passwordConfirmation()).isEqualTo("Secret123");
    }

    @Test
    void toStringNeverExposesThePassword() {
        assertThat(CredentialsMother.valid().toString())
                .contains("qa.automation@webdriver.io")
                .doesNotContain("Test1234!");
    }

    @Test
    void toBuilderDerivesVariantsWithoutMutatingTheOriginal() {
        UserCredentials valid = CredentialsMother.valid();
        UserCredentials variant = valid.toBuilder().email("other@b.co").build();

        assertThat(variant.email()).isEqualTo("other@b.co");
        assertThat(variant.password()).isEqualTo(valid.password());
        assertThat(valid.email()).isEqualTo("qa.automation@webdriver.io");
    }

    @Test
    void motherProvidesDeterministicData() {
        assertThat(CredentialsMother.valid()).isEqualTo(CredentialsMother.valid());
        assertThat(CredentialsMother.withShortPassword().password()).hasSizeLessThan(8);
        assertThat(CredentialsMother.withInvalidEmail().email()).doesNotMatch(".+@.+\\..+");
    }

    @Test
    void emailAndPasswordAreMandatory() {
        UserCredentials.Builder builder = UserCredentials.builder().password("x");

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("email");
    }
}
