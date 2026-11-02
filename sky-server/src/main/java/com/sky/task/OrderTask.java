package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义定时任务类
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * *")
    public void processTimeoutOrder() {
        log.info("处理超时订单");

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // 查询超时订单
        List<Orders> orders = orderMapper.selectTimeoutOrders(Orders.PENDING_PAYMENT, time);

        if(orders != null && !orders.isEmpty()){
            for(Orders order : orders){

                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("超时未支付");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理长时间处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
//    @Scheduled(cron = "0/10 * * * * *") // test
    public void processDeliveryOrder() {
        log.info("处理长时间处于派送中的订单");

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        // 查询长时间处于派送中的订单
        List<Orders> orders = orderMapper.selectTimeoutOrders(Orders.DELIVERY_IN_PROGRESS, time);

        if(orders != null && !orders.isEmpty()){
            for(Orders order : orders){

                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
