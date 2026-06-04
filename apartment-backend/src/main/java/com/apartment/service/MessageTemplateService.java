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

    public String buildEmailSubject() {
        return "房间入住信息通知";
    }

    /**
     * Build a dynamic email subject with booker/group name and stay dates.
     * Format: 订单提醒：{name}将于{startDate}至{endDate}入住本公寓
     *
     * @param name      booker real name or group name
     * @param startDate check-in date (yyyy-MM-dd)
     * @param endDate   check-out date (yyyy-MM-dd)
     */
    public String buildEmailSubject(String name, String startDate, String endDate) {
        String safeName = (name != null && !name.isBlank()) ? name : "-";
        String safeStart = (startDate != null && !startDate.isBlank()) ? startDate : "-";
        String safeEnd = (endDate != null && !endDate.isBlank()) ? endDate : "-";
        return "订单提醒：" + safeName + "将于" + safeStart + "至" + safeEnd + "入住本公寓";
    }

    // ===== Cancel Notification =====

    public String buildCancelSubject(String locale, String name) {
        String safeName = (name != null && !name.isBlank()) ? name : "-";
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Order Cancelled: " + safeName + "'s reservation has been cancelled";
            case "ja" -> "注文キャンセル：" + safeName + "の予約がキャンセルされました";
            default -> "订单取消通知：" + safeName + "的预订已取消";
        };
    }

    public String buildCancelContent(String locale, String orderNo, String name, String startDate, String endDate) {
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Your reservation has been cancelled.\n"
                + "Order No: " + orderNo + "\n"
                + "Guest: " + name + "\n"
                + "Check-in: " + startDate + "\n"
                + "Check-out: " + endDate;
            case "ja" -> "ご予約がキャンセルされました。\n"
                + "注文番号: " + orderNo + "\n"
                + "宿泊者: " + name + "\n"
                + "チェックイン: " + startDate + "\n"
                + "チェックアウト: " + endDate;
            default -> "您的预订已取消。\n"
                + "订单号: " + orderNo + "\n"
                + "订房人: " + name + "\n"
                + "入住日期: " + startDate + "\n"
                + "离店日期: " + endDate;
        };
    }

    // ===== Room Change Notification =====

    public String buildRoomChangeSubject(String locale, String name, String oldRoom, String newRoom) {
        String safeName = (name != null && !name.isBlank()) ? name : "-";
        String safeOld = (oldRoom != null && !oldRoom.isBlank()) ? oldRoom : "-";
        String safeNew = (newRoom != null && !newRoom.isBlank()) ? newRoom : "-";
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Room Change: " + safeName + "'s room changed from " + safeOld + " to " + safeNew;
            case "ja" -> "部屋変更：" + safeName + "の部屋が" + safeOld + "から" + safeNew + "に変更されました";
            default -> "换房通知：" + safeName + "的房间从" + safeOld + "变更为" + safeNew;
        };
    }

    public String buildRoomChangeContent(String locale, String orderNo, String name,
            String oldRoom, String newRoom, String switchDate) {
        return switch (locale != null ? locale : "zh") {
            case "en" -> "Your room has been changed.\n"
                + "Order No: " + orderNo + "\n"
                + "Guest: " + name + "\n"
                + "Previous Room: " + oldRoom + "\n"
                + "New Room: " + newRoom + "\n"
                + "Effective Date: " + switchDate;
            case "ja" -> "お部屋が変更されました。\n"
                + "注文番号: " + orderNo + "\n"
                + "宿泊者: " + name + "\n"
                + "変更前の部屋: " + oldRoom + "\n"
                + "変更後の部屋: " + newRoom + "\n"
                + "適用日: " + switchDate;
            default -> "您的房间已变更。\n"
                + "订单号: " + orderNo + "\n"
                + "订房人: " + name + "\n"
                + "原房间: " + oldRoom + "\n"
                + "新房间: " + newRoom + "\n"
                + "生效日期: " + switchDate;
        };
    }
}
