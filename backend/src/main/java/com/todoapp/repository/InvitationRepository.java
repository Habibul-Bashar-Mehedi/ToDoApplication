package com.todoapp.repository;

import com.todoapp.entity.Invitation;
import com.todoapp.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    List<Invitation> findByModuleId(Long moduleId);

    boolean existsByModuleIdAndInviteeIdAndStatus(Long moduleId, Long inviteeId, InvitationStatus status);
}
