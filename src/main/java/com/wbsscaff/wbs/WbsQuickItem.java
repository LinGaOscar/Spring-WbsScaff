package com.wbsscaff.wbs;

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

    @CreationTimestamp
    private LocalDateTime createdAt;
}
