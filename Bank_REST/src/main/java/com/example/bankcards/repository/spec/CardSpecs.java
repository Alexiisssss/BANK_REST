package com.example.bankcards.repository.spec;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class CardSpecs {

    public static Specification<Card> ownerId(Long ownerId) {
        return (root, q, cb) -> ownerId == null ? null : cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Card> status(CardStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Card> holderNameContains(String text) {
        return (root, q, cb) -> (text == null || text.isBlank())
                ? null
                : cb.like(cb.lower(root.get("holderName")), "%" + text.toLowerCase() + "%");
    }

    public static Specification<Card> panMaskEndsWith(String last4) {
        return (root, q, cb) -> (last4 == null || last4.isBlank())
                ? null
                : cb.like(root.get("panMask"), "% " + last4.trim());
    }

    public static Specification<Card> expiryBetween(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("expiryDate"), from, to);
            return from != null ? cb.greaterThanOrEqualTo(root.get("expiryDate"), from)
                    : cb.lessThanOrEqualTo(root.get("expiryDate"), to);
        };
    }
}
