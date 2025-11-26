//package com.codehows.taelimbe.controller;
//
//import com.codehows.taelimbe.service.GeneralBasicService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/shop")
//public class GeneralBasicController {
//
//    private final GeneralBasicService generalBasicService;
//
//    @GetMapping("/list")
//    public ResponseEntity<String> getShopList(
//            @RequestParam(defaultValue = "20") int limit,
//            @RequestParam(defaultValue = "0") int offset) {
//        return generalBasicService.getShopList(limit, offset);
//    }
//
//    @GetMapping("/robot/list")
//    public ResponseEntity<String> getRobotList(
//            @RequestParam(defaultValue = "20") int limit,
//            @RequestParam(defaultValue = "0") int offset,
//            @RequestParam(required = false) Long shop_id,
//            @RequestParam(required = false) String[] product_code) {
//        return generalBasicService.getRobotList(limit, offset, shop_id, product_code);
//    }
//}