package com.resources.service;

import com.resources.dao.CustomerDAO;
import com.resources.dao.OrderDAO;
import com.resources.dao.PricingDAO;
import com.resources.model.Customer;
import com.resources.model.Order;
import com.resources.model.Pricing;
import java.sql.SQLException;
import java.util.List;

public class LaundryService {
    
    private CustomerDAO customerDAO;
    private OrderDAO orderDAO;
    private PricingDAO pricingDAO;
    
    public LaundryService() {
        customerDAO = new CustomerDAO();
        orderDAO = new OrderDAO();
        pricingDAO = new PricingDAO();
    }
    
    // ===== CUSTOMER METHODS =====
    public Customer login(String contact, String password) throws SQLException {
        return customerDAO.findByContactAndPassword(contact, password);
    }
    
    public boolean register(Customer customer) throws SQLException {
        if (customerDAO.findByContact(customer.getContact()) != null) {
            throw new IllegalArgumentException("Contact already registered");
        }
        return customerDAO.save(customer);
    }
    
    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.findAll();
    }
    
    public Customer getCustomerById(int id) throws SQLException {
        return customerDAO.findById(id);
    }
    
    public Customer getCustomerByContact(String contact) throws SQLException {
        return customerDAO.findByContact(contact);
    }
    
    // ===== ORDER METHODS =====
    public Order createOrder(Order order) throws SQLException {
        order.setQueueNumber(orderDAO.getNextQueueNumber());
        order.setStatus("set_pricing");
        return orderDAO.saveToSetPricing(order);
    }
    
    public boolean updateSetPricingWeightAndPrice(int orderId, double weight, double price) throws SQLException {
        return orderDAO.updateSetPricingWeightAndPrice(orderId, weight, price);
    }
    
    public Order moveToPending(int orderId) throws SQLException {
        return orderDAO.moveToPending(orderId);
    }
    
    public Order getSetPricingById(int orderId) throws SQLException {
        return orderDAO.getSetPricingById(orderId);
    }
    
    public Order getPendingById(int orderId) throws SQLException {
        return orderDAO.getPendingById(orderId);
    }
    
    public List<Order> getSetPricingOrders() throws SQLException {
        return orderDAO.getSetPricingOrders();
    }
    
    public List<Order> getOrdersByTable(String tableName) throws SQLException {
        return orderDAO.getOrdersByTable(tableName);
    }
    
    public List<Order> getOrdersByCustomer(int customerId) throws SQLException {
        return orderDAO.getOrdersByCustomer(customerId);
    }
    
    public Order moveToWash(int orderId) throws SQLException {
        return orderDAO.moveToWash(orderId);
    }
    
    public Order moveToDry(int orderId) throws SQLException {
        return orderDAO.moveToDry(orderId);
    }
    
    public Order moveToIron(int orderId) throws SQLException {
        return orderDAO.moveToIron(orderId);
    }
    
    public Order moveToFold(int orderId) throws SQLException {
        return orderDAO.moveToFold(orderId);
    }
    
    public Order moveToForPickup(int orderId) throws SQLException {
        return orderDAO.moveToForPickup(orderId);
    }
    
    public Order moveToDeliver(int orderId) throws SQLException {
        return orderDAO.moveToDeliver(orderId);
    }
    
    public Order moveToClaimed(int orderId) throws SQLException {
        return orderDAO.moveToClaimed(orderId);
    }
    
    public Order moveToClaimedFromPickup(int orderId) throws SQLException {
        return orderDAO.moveToClaimedFromPickup(orderId);
    }
    
    // ===== PRICING METHODS =====
    public Pricing getPricing() throws SQLException {
        Pricing p = pricingDAO.getPricing();
        if (p == null) {
            pricingDAO.insertDefaultPricing();
            p = pricingDAO.getPricing();
        }
        return p;
    }
    
    public boolean updatePricing(Pricing pricing) throws SQLException {
        return pricingDAO.updatePricing(pricing);
    }
}