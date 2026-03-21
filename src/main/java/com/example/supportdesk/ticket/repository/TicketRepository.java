package com.example.supportdesk.ticket.repository;

import com.example.supportdesk.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
}