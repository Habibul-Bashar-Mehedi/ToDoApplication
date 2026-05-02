package com.todoapp.repository;

import com.todoapp.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    boolean existsByOwnerIdAndNameAndDeletedAtIsNull(Long ownerId, String name);

    Optional<Module> findByIdAndDeletedAtIsNull(Long id);
}
