package com.winter.orderprocessing.dao;

import com.winter.orderprocessing.entity.OrderEntity;
import com.winter.orderprocessing.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OrderEntity o set o.status = :processing, o.version = o.version + 1 where o.status = :pending")
    int processPendingOrders(@Param("pending") OrderStatus pending,
                             @Param("processing") OrderStatus processing);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OrderEntity o set o.status = :cancelled, o.version = o.version + 1 " +
            "where o.id = :id and o.status = :pending")
    int cancelPendingOrder(@Param("id") Long id,
                           @Param("pending") OrderStatus pending,
                           @Param("cancelled") OrderStatus cancelled);
}
