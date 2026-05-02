package com.todoapp.repository;

import com.todoapp.entity.ModuleMember;
import com.todoapp.entity.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleMemberRepository extends JpaRepository<ModuleMember, Long> {

    List<ModuleMember> findByModuleId(Long moduleId);

    List<ModuleMember> findByUserId(Long userId);

    Optional<ModuleMember> findByModuleIdAndUserId(Long moduleId, Long userId);

    boolean existsByModuleIdAndUserId(Long moduleId, Long userId);

    List<ModuleMember> findByModuleIdAndPermissionLevelIn(Long moduleId, List<PermissionLevel> levels);
}
