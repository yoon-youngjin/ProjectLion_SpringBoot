package dev.yoon.sss.Account.dto;

import dev.yoon.sss.Account.domain.Account;
import dev.yoon.sss.Account.domain.Address;
import dev.yoon.sss.Account.domain.Email;
import dev.yoon.sss.Account.domain.Password;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class AccountDto {
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SignUpReq {

        @Valid
        private Email email;

        @NotEmpty
        private String firstName;
        @NotEmpty
        private String lastName;

        private String password;

        @Valid
        private Address address;

        @Builder
        public SignUpReq(Email email, String fistName, String lastName, String password, Address address) {
            this.email = email;
            this.firstName = fistName;
            this.lastName = lastName;
            this.password = password;
            this.address = address;
        }

        public Account toEntity() {
            return Account.builder()
                    .email(this.email)
                    .firstName(this.firstName)
                    .lastName(this.lastName)
                    .password(Password.builder().value(this.password).build())
                    .address(this.address)
                    .build();
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MyAccountReq {
        private Address address;

        @Builder
        public MyAccountReq(final Address address) {
            this.address = address;
        }

    }

    @Getter
    public static class Res {

        private Email email;
        private Password password;
        private String fistName;
        private String lastName;
        private Address address;

        public Res(Account account) {
            this.email = account.getEmail();
            this.fistName = account.getFirstName();
            this.lastName = account.getLastName();
            this.address = account.getAddress();
            this.password = account.getPassword();
        }
    }
}
