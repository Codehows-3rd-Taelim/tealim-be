//package com.codehows.taelimbe.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(name = "store")
//@NoArgsConstructor
//@AllArgsConstructor
//@Getter
//@Setter
//@Builder
//public class Store {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "store_id")
//    private Long storeId;
//
//    @Column(name = "shop_id")
//    private Long shopId;
//
//    @Column(name = "shop_name", length = 20)
//    private String shopName;
//
//    @ManyToOne
//    @JoinColumn(name = "industry_id")
//    private Industry industry;
//
//}
