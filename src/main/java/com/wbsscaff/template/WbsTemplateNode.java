package com.wbsscaff.template;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wbs_template_nodes")
@Getter @Setter
public class WbsTemplateNode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 使用 ManyToOne 關聯，避免裸 Long FK 導致無法做 JOIN 查詢
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private WbsTemplate template;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;
}
