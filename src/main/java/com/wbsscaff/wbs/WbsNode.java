package com.wbsscaff.wbs;

import com.wbsscaff.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wbs_nodes")
@Getter @Setter
public class WbsNode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // null 表示根節點（L1）；非 null 表示子節點（L2），最多兩層
    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 300)
    private String title;

    // 自由文字欄位，不關聯 users 表，方便填寫外部協作人員或暫時代理人
    @Column(length = 100)
    private String owner;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_STARTED;

    // notes 欄位僅在 L3（前端 depth >= 3）顯示，根節點與中層節點不使用
    @Column(columnDefinition = "TEXT")
    private String notes;

    // 同層節點的排列順序，拖曳排序後由 reorder API 批次更新
    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum Status { NOT_STARTED, IN_PROGRESS, DONE }
}
