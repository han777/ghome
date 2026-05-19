package com.apartment.service;

import org.springframework.stereotype.Service;

@Service
public class MessageTemplateService {

    public String buildCheckInNotification(String locale, String orderNo, String roomNo,
            String keyBoxNo, String boxPassword, String checkInTime, String checkOutTime) {
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Your room information:\n"
                + "Room No: " + roomNo + "\n"
                + "Key Box No: " + keyBoxNo + "\n"
                + "Box Password: " + boxPassword + "\n"
                + "Check-in: " + checkInTime + "\n"
                + "Check-out: " + checkOutTime;
            case "ja" -> "お部屋の情報：\n"
                + "部屋番号: " + roomNo + "\n"
                + "キーボックス番号: " + keyBoxNo + "\n"
                + "ボックスパスワード: " + boxPassword + "\n"
                + "チェックイン: " + checkInTime + "\n"
                + "チェックアウト: " + checkOutTime;
            default -> "您的房间信息如下：\n"
                + "房间号: " + roomNo + "\n"
                + "房卡箱号: " + keyBoxNo + "\n"
                + "箱密码: " + boxPassword + "\n"
                + "入住时间: " + checkInTime + "\n"
                + "离店时间: " + checkOutTime;
        };
    }

    public String buildEmailSubject(String locale) {
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Room Check-in Information";
            case "ja" -> "チェックイン情報のお知らせ";
            default -> "房间入住信息通知";
        };
    }
}
