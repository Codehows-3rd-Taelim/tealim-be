package com.codehows.taelimbe.store.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "industry")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "industry_id")
    private Long industryId;

    @Column(name = "industry_name", length = 255)
    private String industryName;

}
