package dev.yoon.springguide.domain.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.bytebuddy.utility.RandomString;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ReferralCode {

    @Column(name = "referral_code", length = 50)
    private String value;

    private ReferralCode(String value) {
        this.value = value;
    }

    public static ReferralCode of(final String value) {
        return new ReferralCode(value);
    }

    public static ReferralCode generateCode() {
        return new ReferralCode(RandomString.make(10));
    }
}
