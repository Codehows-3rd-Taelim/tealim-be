package com.codehows.taelimbe.config;


/**
 * 현재 스레드의 사용자 컨텍스트(예: 사용자 이름)를 저장하고 검색하기 위한 유틸리티 클래스입니다.
 * `ThreadLocal`을 사용하여 각 스레드마다 독립적인 사용자 정보를 유지할 수 있도록 합니다.
 * 이는 웹 요청과 같이 여러 스레드가 동시에 작업을 처리할 때 각 요청의 사용자 정보를 분리하여 관리하는 데 유용합니다.
 */
public class UserContextHolder {
    // ThreadLocal 변수를 선언하여 각 스레드에 String 타입의 사용자 이름을 저장할 수 있도록 합니다.
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    /**
     * 현재 스레드의 사용자 이름을 설정합니다.
     *
     * @param userName 설정할 사용자 이름 문자열
     */
    public static void setUserName(String userName) {
        currentUser.set(userName);
    }

    /**
     * 현재 스레드에 저장된 사용자 이름을 가져옵니다.
     *
     * @return 현재 스레드의 사용자 이름 문자열. 설정되지 않았다면 null을 반환합니다.
     */
    public static String getUserName() {
        return currentUser.get();
    }

    /**
     * 현재 스레드에 저장된 사용자 이름을 제거합니다.
     * 이는 스레드가 재사용될 때 이전 요청의 사용자 정보가 남아있지 않도록 하여
     * 데이터 누출이나 잘못된 정보 사용을 방지하는 데 중요합니다.
     */
    public static void clear() {
        currentUser.remove();
    }
}