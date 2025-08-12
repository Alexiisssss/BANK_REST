package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class CardMapper {

    private static final Logger log = LoggerFactory.getLogger(CardMapper.class);

    private CardMapper() {
    }

    public static CardResponse toResponse(Card c) {
        String reqId = MDC.get("reqId");
        if (c == null) {
            log.warn("[{}] CardMapper.toResponse called with null Card", reqId);
            return null;
        }
        if (c.getOwner() == null) {
            log.warn("[{}] CardMapper.toResponse: card {} has null owner", reqId, c.getId());
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] Mapping Card -> CardResponse: id={}, panMask={}, status={}",
                    reqId, c.getId(), c.getPanMask(), c.getStatus());
        }

        CardResponse dto = new CardResponse();
        dto.setId(c.getId());
        dto.setOwnerId(c.getOwner() != null ? c.getOwner().getId() : null);
        dto.setHolderName(c.getHolderName());
        dto.setPanMask(c.getPanMask()); // masked, safe to log if needed
        dto.setExpiryDate(c.getExpiryDate());
        dto.setStatus(c.getStatus());
        dto.setBalance(c.getBalance());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        if (log.isDebugEnabled()) {
            log.debug("[{}] CardMapper.toResponse done: id={}, ownerId={}", reqId, dto.getId(), dto.getOwnerId());
        }
        return dto;
    }
}
