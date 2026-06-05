package com.apartment.service;

import com.apartment.entity.Room;
import com.apartment.entity.RoomOccupy;
import com.apartment.entity.RoomOrder;
import com.apartment.repository.RoomOccupyRepository;
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
public class OrderAutoCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCheckoutService.class);

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private RoomOccupyRepository occupyRepository;

    @Autowired
    private RoomRepository roomRepository;

    // @Scheduled(cron = "0 30 14 * * ?")  // 已禁用自动退房定时任务
    @Transactional
    public void autoCheckout() {
        LocalDateTime now = LocalDateTime.now();
        List<RoomOrder> orders = orderRepository.findByStatusAndEndDateBefore(2, now);
        log.info("自动退房检查: 发现 {} 个已过离开时间的在住订单", orders.size());

        for (RoomOrder order : orders) {
            try {
                checkoutExpiredRooms(order, now);
            } catch (Exception e) {
                log.error("自动退房失败, 订单号: {}", order.getOrderNo(), e);
            }
        }
    }

    private void checkoutExpiredRooms(RoomOrder order, LocalDateTime now) {
        List<RoomOccupy> occupies = order.getRoomOccupies();
        if (occupies == null || occupies.isEmpty()) return;

        for (RoomOccupy occupy : occupies) {
            if (occupy.getStatus() != null && occupy.getStatus() == RoomOccupy.STATUS_CHECKED_IN) {
                occupy.setStatus(RoomOccupy.STATUS_CHECKED_OUT);
                occupy.setCheckOutTime(now);
                occupyRepository.save(occupy);

                Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0);
                    roomRepository.save(room);
                }
            }
        }

        boolean allFinished = occupies.stream()
                .allMatch(o -> o.getStatus() != null && (o.getStatus() == RoomOccupy.STATUS_CHECKED_OUT || o.getStatus() == RoomOccupy.STATUS_CANCELED));

        if (allFinished) {
            order.setStatus(3);
            orderRepository.save(order);
            log.info("订单 {} 全部房间已退房，订单状态设为已退房", order.getOrderNo());
        }
    }
}
