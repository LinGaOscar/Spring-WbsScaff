package com.wbsscaff.template;

import com.wbsscaff.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<WbsTemplate, Long> {
    List<WbsTemplate> findByIsSystemTrue();
    List<WbsTemplate> findByOwnerIdAndIsSystemFalse(Long ownerId);
    // 取得模板並驗證擁有者，確保非本人無法修改
    Optional<WbsTemplate> findByIdAndOwner(Long id, User owner);
}
