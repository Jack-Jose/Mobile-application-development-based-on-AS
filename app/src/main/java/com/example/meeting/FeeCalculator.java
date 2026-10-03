package com.example.meeting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;

public class FeeCalculator {

    public static final int DEPOSIT_PER_PERSON = 50;
    public static final double DEPOSIT_FORFEIT_RATE = 0.10;
    public static final double SUBSCRIPTION_DISCOUNT = 0.85;
    public static final int SUBSCRIPTION_DISCOUNT_PCT = 15;
    public static final double VIP_SUBSCRIPTION_DISCOUNT = 0.80;
    public static final int VIP_SUBSCRIPTION_DISCOUNT_PCT = 20;
    public static final double SERVICE_DISCOUNT = 0.90;
    public static final int OVERTIME_GRACE = 30;
    public static final double OVERTIME_RATE_LOW = 0.05;
    public static final double OVERTIME_RATE_HIGH = 0.15;
    public static final int MAX_DURATION_MIN = 360;
    public static final long DATA_RETENTION_MS = 180L * 24 * 60 * 60 * 1000;
    public static final String VIP_ROOM_PREFIX = "C101";
    public static final int VIP_BASE_PRICE = 25;
    public static final double SUB_FEE_MONTHLY = 99.0;

    public static final int OPEN_HOUR = 6;
    public static final int OPEN_MIN = 0;
    public static final int CLOSE_HOUR = 23;
    public static final int CLOSE_MIN = 30;

    public static int minDurationByCapacity(int capacity) {
        if (capacity <= 10) return 5;
        if (capacity <= 30) return 10;
        if (capacity <= 59) return 20;
        return 30;
    }

    public static boolean isVipRoom(String roomName) {
        return roomName != null && roomName.startsWith(VIP_ROOM_PREFIX);
    }

    public static int getTierMultiplier(long durationMinutes) {
        if (durationMinutes <= 30) return 1;
        if (durationMinutes <= 120) return 2;
        return 3;
    }

    public static String getTierName(long durationMinutes) {
        switch (getTierMultiplier(durationMinutes)) {
            case 1: return "T1 (5-30min)";
            case 2: return "T2 (30min-2h)";
            case 3: return "T3 (2h-6h)";
            default: return "";
        }
    }

    public static String checkBusinessHours(long startMs, long endMs) {
        Calendar s = Calendar.getInstance(); s.setTimeInMillis(startMs);
        Calendar e = Calendar.getInstance(); e.setTimeInMillis(endMs);
        if (endMs <= startMs || s.get(Calendar.YEAR) != e.get(Calendar.YEAR)
                || s.get(Calendar.DAY_OF_YEAR) != e.get(Calendar.DAY_OF_YEAR)) {
            return "预约必须在同一营业日内结束";
        }
        int sMin = s.get(Calendar.HOUR_OF_DAY) * 60 + s.get(Calendar.MINUTE);
        int eMin = e.get(Calendar.HOUR_OF_DAY) * 60 + e.get(Calendar.MINUTE);
        int openMin = OPEN_HOUR * 60 + OPEN_MIN;
        int closeMin = CLOSE_HOUR * 60 + CLOSE_MIN;
        if (sMin < openMin) return "营业时间 6:00-23:30，开始时间太早";
        if (eMin > closeMin) return "营业时间 6:00-23:30，结束时间超过23:30";
        return null;
    }

    public static String checkChairRedundancy(int attendees, int capacity, int chairs) {
        if (attendees > chairs)
            return "BLOCK:参会人数(" + attendees + ")超过椅子数(" + chairs + ")，请换更大会议室";
        if (attendees > capacity)
            return "BLOCK:参会人数(" + attendees + ")超过容量(" + capacity + ")，不可用租借椅子解决，请换更大会议室";
        return null;
    }

    public static double calculateFee(int capacity, int basePrice, long durationMinutes, boolean isSubscription, String roomName) {
        int tier = getTierMultiplier(durationMinutes);
        double raw = (double) capacity * tier * basePrice;
        if (isSubscription) {
            double disc = isVipRoom(roomName) ? VIP_SUBSCRIPTION_DISCOUNT : SUBSCRIPTION_DISCOUNT;
            raw *= disc;
        }
        return round(raw);
    }

    public static double calculateDeposit(int capacity) { return (double) DEPOSIT_PER_PERSON * capacity; }
    public static double calculateForfeitDeposit(double deposit) { return round(deposit * DEPOSIT_FORFEIT_RATE); }

    public static double calculateOvertimeFee(double originalFee, int overtimeMinutes) {
        if (overtimeMinutes <= 0 || overtimeMinutes > OVERTIME_GRACE) return 0;
        double rate = overtimeMinutes <= 15 ? OVERTIME_RATE_LOW : OVERTIME_RATE_HIGH;
        return round(originalFee * rate);
    }

    public static double calculateServiceFee(double marketPrice, int quantity) {
        return round(marketPrice * SERVICE_DISCOUNT * quantity);
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String getDiscountLabel(boolean isSub, String roomName) {
        if (!isSub) return "标准收费";
        return isVipRoom(roomName) ? "订阅VIP8折" : "订阅85折";
    }

    public static String toFeeDetail(int capacity, int basePrice, long durationMin, boolean isSub, String roomName, double overtimeMin, double serviceFee) {
        int tier = getTierMultiplier(durationMin);
        double raw = (double) capacity * tier * basePrice;
        double actual = calculateFee(capacity, basePrice, durationMin, isSub, roomName);
        double deposit = calculateDeposit(capacity);
        boolean vip = isVipRoom(roomName);
        StringBuilder sb = new StringBuilder();
        sb.append("Pricing: ").append(roomName != null ? roomName : "").append("\n");
        sb.append("Formula: ").append(capacity).append(" x ").append(tier).append(" x ").append(basePrice).append(" = ").append(String.format("%.2f", raw));
        if (isSub) { sb.append(" "); sb.append(vip ? "(VIP8折=" : "(85折="); sb.append(String.format("%.2f", actual)).append(")"); }
        sb.append("\nDeposit: ").append(String.format("%.2f", deposit));
        if (vip) sb.append("\n[VIP: services included]");
        if (overtimeMin > 0) sb.append("\nOvertime: ").append(String.format("%.2f", calculateOvertimeFee(actual, (int)overtimeMin)));
        if (!vip && serviceFee > 0) sb.append("\nServices: ").append(String.format("%.2f", serviceFee));
        double total = actual + (overtimeMin > 0 ? calculateOvertimeFee(actual, (int)overtimeMin) : 0) + (vip ? 0 : serviceFee);
        sb.append("\nTotal: ").append(String.format("%.2f", total));
        return sb.toString();
    }
}
