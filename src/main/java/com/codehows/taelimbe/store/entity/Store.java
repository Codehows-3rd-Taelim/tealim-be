package com.codehows.taelimbe.store.entity;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "shop_name", length = 20, nullable = false)
    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DeleteStatus delYn = DeleteStatus.N;

    @ManyToOne
    @JoinColumn(name = "industry_id")
    private Industry industry;

}
