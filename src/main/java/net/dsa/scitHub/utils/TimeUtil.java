package net.dsa.scitHub.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

@Component("timeUtil")
public class TimeUtil {
    // 일본 서비스 기준?
    private static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

    public String timeAgoJa(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime createdZoned = createdAt.atZone(ZONE);

        if (createdZoned.isAfter(now)) {
            return "たった今";
        }

        Duration duration = Duration.between(createdZoned, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "たった今";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "時間前";
        }
        long days = hours / 24;
        if (days < 30) {
            return days + "日前";
        }
        long months = days / 30;
        if (months < 12) {
            return months + "ヶ月前";
        }
        long years = months / 12;
        return years + "年前";
    }
}
