package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateRepository extends JpaRepository<WbsTemplate, Long> {
    List<WbsTemplate> findByIsSystemTrue();
    List<WbsTemplate> findByOwnerIdAndIsSystemFalse(Long ownerId);
}
