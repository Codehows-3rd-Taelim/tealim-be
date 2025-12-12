package com.codehows.taelimbe.ai.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateRangeParser {

    public static LocalDateTime[] extractDateRange(String userMessage) {

        String msg = userMessage.trim();
        LocalDateTime now = LocalDateTime.now();

        // ============================
        // 1) YYYY-MM-DD ~ YYYY-MM-DD
        // ============================
        Pattern rangePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*[~–-]\\s*(\\d{4}-\\d{2}-\\d{2})");
        Matcher r = rangePattern.matcher(msg);
        if (r.find()) {
            LocalDate startD = LocalDate.parse(r.group(1));
            LocalDate endD = LocalDate.parse(r.group(2));
            return new LocalDateTime[]{
                    startD.atStartOfDay(),
                    endD.atTime(23, 59, 59)
            };
        }

        // ============================
        // 2) 단일 날짜 YYYY-MM-DD
        // ============================
        Pattern singleDatePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher s = singleDatePattern.matcher(msg);
        if (s.find()) {
            LocalDate d = LocalDate.parse(s.group(1));
            return new LocalDateTime[]{
                    d.atStartOfDay(),
                    d.atTime(23, 59, 59)
            };
        }

        // ============================
        // 3) YYYY-MM → 월 전체
        // ============================
        Pattern ymPattern = Pattern.compile("(\\d{4})-(\\d{2})");
        Matcher ym = ymPattern.matcher(msg);
        if (ym.find()) {
            int year = Integer.parseInt(ym.group(1));
            int month = Integer.parseInt(ym.group(2));

            LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 4) "11월", "3월 보고서"
        // ============================
        Pattern monthPattern = Pattern.compile("(\\d{1,2})월");
        Matcher m = monthPattern.matcher(msg);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int year = now.getYear();

            LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 5) 자연어 기간
        // ============================

        // 작년
        if (msg.contains("작년")) {
            LocalDateTime start = LocalDate.of(now.getYear() - 1, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear() - 1, 12, 31).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // 올해 / 금년
        if (msg.contains("올해") || msg.contains("금년") || msg.contains("이번년")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 12, 31).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // 지난달
        if (msg.contains("지난달")) {
            LocalDateTime start = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            return new LocalDateTime[]{start, end};
        }

        // 이번달 / 당월
        if (msg.contains("이번달") || msg.contains("당월")) {
            LocalDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            return new LocalDateTime[]{start, end};
        }

        // 오늘
        if (msg.contains("오늘")) {
            LocalDateTime start = now.toLocalDate().atStartOfDay();
            LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // 어제
        if (msg.contains("어제")) {
            LocalDateTime start = now.minusDays(1).toLocalDate().atStartOfDay();
            LocalDateTime end = now.minusDays(1).toLocalDate().atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // 그제 / 그저께
        if (msg.contains("그제") || msg.contains("그저께")) {
            LocalDateTime start = now.minusDays(2).toLocalDate().atStartOfDay();
            LocalDateTime end = now.minusDays(2).toLocalDate().atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 6) 최근 N일 (방식 A)
        // ============================
        Pattern recentNDays = Pattern.compile("최근\\s*(\\d+)일");
        Matcher nd = recentNDays.matcher(msg);
        if (nd.find()) {
            int days = Integer.parseInt(nd.group(1));
            LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
            LocalDateTime start = end.minusDays(days - 1).toLocalDate().atStartOfDay(); // 방식 A
            return new LocalDateTime[]{start, end};
        }

        // 7) 최근 N개월
        Pattern recentNMonths = Pattern.compile("최근\\s*(\\d+)개월");
        Matcher nm = recentNMonths.matcher(msg);
        if (nm.find()) {
            int months = Integer.parseInt(nm.group(1));
            LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
            LocalDateTime start = end.minusMonths(months).toLocalDate().atStartOfDay();
            return new LocalDateTime[]{start, end};
        }

        // 8) 최근 N주
        Pattern recentNWeeks = Pattern.compile("최근\\s*(\\d+)주");
        Matcher nw = recentNWeeks.matcher(msg);
        if (nw.find()) {
            int weeks = Integer.parseInt(nw.group(1));
            LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
            LocalDateTime start = end.minusWeeks(weeks).toLocalDate().atStartOfDay();
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 9) 이번주
        // ============================
        if (msg.contains("이번주")) {
            LocalDateTime start = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
            LocalDateTime end = start.plusDays(6).withHour(23).withMinute(59).withSecond(59);
            return new LocalDateTime[]{start, end};
        }

        // 지난주
        if (msg.contains("지난주")) {
            LocalDateTime thisMonday = now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
            LocalDateTime start = thisMonday.minusWeeks(1);
            LocalDateTime end = start.plusDays(6).withHour(23).withMinute(59).withSecond(59);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 10) 분기
        // ============================
        if (msg.contains("1분기") || msg.toLowerCase().contains("q1")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 3, 31).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }
        if (msg.contains("2분기") || msg.toLowerCase().contains("q2")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 4, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 6, 30).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }
        if (msg.contains("3분기") || msg.toLowerCase().contains("q3")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 7, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 9, 30).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }
        if (msg.contains("4분기") || msg.toLowerCase().contains("q4")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 10, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 12, 31).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 11) 상반기 / 하반기
        // ============================
        if (msg.contains("상반기")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 6, 30).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        if (msg.contains("하반기")) {
            LocalDateTime start = LocalDate.of(now.getYear(), 7, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(now.getYear(), 12, 31).atTime(23, 59, 59);
            return new LocalDateTime[]{start, end};
        }

        // ============================
        // 12) 어떤 날짜도 안 맞음
        // ============================
        return null;
    }
}
