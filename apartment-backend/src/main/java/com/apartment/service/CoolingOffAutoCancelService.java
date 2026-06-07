package com.apartment.service;

import com.apartment.entity.OrderLog;
import com.apartment.entity.RoomOrder;
import com.apartment.repository.OrderLogRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoolingOffAutoCancelService {

    private static final Logger log = LoggerFactory.getLogger(CoolingOffAutoCancelService.class);

    private static final int STATUS_COOLING_OFF = 0;
    private static final int STATUS_CANCELED = 4;
    private static final long COOLING_OFF_MINUTES = 15;

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * 定时任务：每5分钟检查冷静期订单，超过15分钟未确认的自动取消
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void autoCancelExpiredCoolingOffOrders() {
        LocalDateTime cutoff = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(COOLING_OFF_MINUTES);
        List<RoomOrder> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(STATUS_COOLING_OFF, cutoff);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("发现 {} 个超时冷静期订单，开始自动取消", expiredOrders.size());

        for (RoomOrder order : expiredOrders) {
            try {
                cancelOrder(order);
                log.info("订单 {} 已自动取消（冷静期超时）", order.getOrderNo());
            } catch (Exception e) {
                log.error("自动取消订单 {} 失败", order.getOrderNo(), e);
            }
        }

        log.info("冷静期订单自动取消完成，共处理 {} 个", expiredOrders.size());
    }

    private void cancelOrder(RoomOrder order) {
        order.setStatus(STATUS_CANCELED);

        // 释放关联房间，并将房间入住记录状态设为取消
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setStatus(com.apartment.entity.RoomOccupy.STATUS_CANCELED);
                com.apartment.entity.Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0); // Available
                    roomRepository.save(room);
                }
            }
        }

        orderRepository.save(order);

        // 记录订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrder(order);
        orderLog.setOperator(null); // 系统自动操作
        orderLog.setOperationTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        orderLog.setOperationType("AUTO_CANCEL");
        orderLog.setOperationContent("超时自动取消");
        orderLogRepository.save(orderLog);
    }
}
