package com.wbsscaff.wbs;

import com.wbsscaff.department.Department;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "wbs_quick_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WbsQuickItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String category;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "requirement_doc", length = 500)
    private String requirementDoc;

    // null = 全域預設（全員可見，不可管理）；非 null = 科快速子項（僅同科 SECTION_CHIEF/PROJECT_LEADER 管理）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Department section;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
