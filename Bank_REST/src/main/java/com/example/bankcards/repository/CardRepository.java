package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Page<Card> findByOwner_Id(Long ownerId, Pageable pageable);

    long countByOwner_IdAndStatus(Long ownerId, CardStatus status);

    // Read WITHOUT lock only for the owner's card
    Optional<Card> findByIdAndOwner_Id(Long id, Long ownerId);

    // Pessimistic lock for the owner's card (for transfers)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id and c.owner.id = :ownerId")
    Optional<Card> findByIdAndOwner_IdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);

    // Aliases in case userId is used somewhere instead of ownerId
    default Optional<Card> findByIdAndUserId(Long id, Long userId) {
        return findByIdAndOwner_Id(id, userId);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    default Optional<Card> findByIdAndUserIdForUpdate(Long id, Long userId) {
        return findByIdAndOwner_IdForUpdate(id, userId);
    }
}
