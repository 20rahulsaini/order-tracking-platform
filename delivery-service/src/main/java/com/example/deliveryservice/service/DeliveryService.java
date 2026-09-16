package com.example.deliveryservice.service;


public interface DeliveryService {

   public void ship(String orderId);

   public void outForDelivery(String orderId);

   public void deliver(String orderId);
}
