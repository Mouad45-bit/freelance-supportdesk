package com.example.supportdesk.ticket.specification;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.entity.Ticket;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {
    private TicketSpecifications(){}
    //
    public static Specification<Ticket> isNotDeleted(){
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) ->
                priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasAuthorId(Long authorId) {
        return (root, query, cb) ->
                authorId == null ? null : cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Ticket> titleContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("title")),
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        };
    }
}
