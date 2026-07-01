package com.resources.dao;

import com.resources.model.Order;
import com.resources.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    
    // ===== SAVE TO SET PRICING =====
    public Order saveToSetPricing(Order order) throws SQLException {
        String sql = "INSERT INTO set_pricing_orders (customer_id, services, service_type, queue_number, weight, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getCustomerId());
            pstmt.setString(2, order.getServices());
            pstmt.setString(3, order.getServiceType());
            pstmt.setInt(4, order.getQueueNumber());
            pstmt.setDouble(5, order.getWeight());
            pstmt.setDouble(6, order.getPrice());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) order.setId(rs.getInt(1));
                return order;
            }
            return null;
        }
    }
    
    // ===== MOVE FROM SET PRICING TO PENDING =====
    public Order moveToPending(int orderId) throws SQLException {
        String selectSql = "SELECT o.*, c.first_name, c.last_name FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        Order order = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                order = mapWithName(rs, "pending");
            }
        }
        if (order == null) return null;
        
        String insertSql = "INSERT INTO pending_orders (customer_id, services, service_type, queue_number, weight, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getCustomerId());
            pstmt.setString(2, order.getServices());
            pstmt.setString(3, order.getServiceType());
            pstmt.setInt(4, order.getQueueNumber());
            pstmt.setDouble(5, order.getWeight());
            pstmt.setDouble(6, order.getPrice());
            pstmt.executeUpdate();
        }
        
        String deleteSql = "DELETE FROM set_pricing_orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
        }
        
        return order;
    }
    
    // ===== UPDATE SET PRICING WEIGHT & PRICE =====
    public boolean updateSetPricingWeightAndPrice(int orderId, double weight, double price) throws SQLException {
        String sql = "UPDATE set_pricing_orders SET weight = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, weight);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, orderId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // ===== GET SET PRICING BY ID =====
    public Order getSetPricingById(int orderId) throws SQLException {
        String sql = "SELECT o.*, c.first_name, c.last_name FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapWithName(rs, "set_pricing");
        }
        return null;
    }
    
    // ===== GET PENDING BY ID =====
    public Order getPendingById(int orderId) throws SQLException {
        String sql = "SELECT o.*, c.first_name, c.last_name FROM pending_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapWithName(rs, "pending");
        }
        return null;
    }
    
    // ===== GET SET PRICING ORDERS =====
    public List<Order> getSetPricingOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.first_name, c.last_name FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.queue_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapWithName(rs, "set_pricing"));
            }
        }
        return list;
    }
    
    // ===== GET ORDERS BY TABLE =====
    public List<Order> getOrdersByTable(String tableName) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.first_name, c.last_name FROM " + tableName + " o LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.queue_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String status = tableName.replace("_orders", "");
                list.add(mapWithName(rs, status));
            }
        }
        return list;
    }
    
    // ===== GET ORDER BY ID FROM SPECIFIC TABLE =====
    private Order getOrderById(int orderId, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Order o = new Order();
                o.setId(rs.getInt("id"));
                o.setCustomerId(rs.getInt("customer_id"));
                o.setServices(rs.getString("services"));
                o.setServiceType(rs.getString("service_type"));
                o.setQueueNumber(rs.getInt("queue_number"));
                o.setWeight(rs.getDouble("weight"));
                o.setPrice(rs.getDouble("price"));
                o.setCreatedAt(rs.getString("created_at"));
                return o;
            }
        }
        return null;
    }
    
    // ===== GET NEXT QUEUE NUMBER =====
    public int getNextQueueNumber() throws SQLException {
        String sql = "SELECT COALESCE(MAX(queue_number), 0) + 1 FROM set_pricing_orders";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            return 1;
        }
    }
    
    // ===== GET ORDERS BY CUSTOMER =====
    public List<Order> getOrdersByCustomer(int customerId) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT 'set_pricing' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM set_pricing_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'pending' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM pending_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_wash' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM to_wash_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_dry' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM to_dry_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_iron' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM to_iron_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_fold' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM to_fold_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'for_pickup' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM for_pickup_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'to_deliver' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM to_deliver_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "UNION ALL " +
                     "SELECT 'claimed' as status, o.id, o.customer_id, o.services, o.service_type, o.queue_number, o.weight, o.price, o.created_at, c.first_name, c.last_name FROM claimed_orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.customer_id = ? " +
                     "ORDER BY queue_number ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 9; i++) pstmt.setInt(i, customerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapWithStatus(rs));
            }
        }
        return list;
    }
    
    // ===== MOVE TO WASH =====
    public Order moveToWash(int orderId) throws SQLException {
        return moveOrder(orderId, "pending_orders", "to_wash_orders", "to_wash");
    }
    
    // ===== MOVE TO DRY =====
    public Order moveToDry(int orderId) throws SQLException {
        Order order = getOrderById(orderId, "pending_orders");
        if (order != null) {
            return moveOrder(orderId, "pending_orders", "to_dry_orders", "to_dry");
        }
        return moveOrder(orderId, "to_wash_orders", "to_dry_orders", "to_dry");
    }
    
    // ===== MOVE TO IRON =====
    public Order moveToIron(int orderId) throws SQLException {
        Order order = getOrderById(orderId, "pending_orders");
        if (order != null) {
            return moveOrder(orderId, "pending_orders", "to_iron_orders", "to_iron");
        }
        return moveOrder(orderId, "to_dry_orders", "to_iron_orders", "to_iron");
    }
    
    // ===== MOVE TO FOLD =====
    public Order moveToFold(int orderId) throws SQLException {
        String[] tables = {"pending_orders", "to_iron_orders", "to_dry_orders", "to_wash_orders"};
        for (String table : tables) {
            Order order = getOrderById(orderId, table);
            if (order != null) {
                return moveOrder(orderId, table, "to_fold_orders", "to_fold");
            }
        }
        return null;
    }
    
    // ===== MOVE TO FOR PICKUP =====
    public Order moveToForPickup(int orderId) throws SQLException {
        return moveOrder(orderId, "to_fold_orders", "for_pickup_orders", "for_pickup");
    }
    
    // ===== MOVE TO DELIVER =====
    public Order moveToDeliver(int orderId) throws SQLException {
        return moveOrder(orderId, "for_pickup_orders", "to_deliver_orders", "to_deliver");
    }
    
    // ===== MOVE TO CLAIMED =====
    public Order moveToClaimed(int orderId) throws SQLException {
        return moveOrder(orderId, "to_deliver_orders", "claimed_orders", "claimed");
    }
    
    // ===== MOVE TO CLAIMED FROM PICKUP =====
    public Order moveToClaimedFromPickup(int orderId) throws SQLException {
        return moveOrder(orderId, "for_pickup_orders", "claimed_orders", "claimed");
    }
    
    // ===== GENERIC MOVE ORDER =====
    private Order moveOrder(int orderId, String fromTable, String toTable, String newStatus) throws SQLException {
        String selectSql = "SELECT o.*, c.first_name, c.last_name FROM " + fromTable + " o LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ?";
        Order order = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                order = mapWithName(rs, newStatus);
            }
        }
        if (order == null) return null;
        
        String insertSql = "INSERT INTO " + toTable + " (customer_id, services, service_type, queue_number, weight, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, order.getCustomerId());
            pstmt.setString(2, order.getServices());
            pstmt.setString(3, order.getServiceType());
            pstmt.setInt(4, order.getQueueNumber());
            pstmt.setDouble(5, order.getWeight());
            pstmt.setDouble(6, order.getPrice());
            pstmt.executeUpdate();
        }
        
        String deleteSql = "DELETE FROM " + fromTable + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
        }
        
        return order;
    }
    
    // ===== MAPPERS =====
    private Order mapWithName(ResultSet rs, String status) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setServices(rs.getString("services"));
        o.setServiceType(rs.getString("service_type"));
        o.setQueueNumber(rs.getInt("queue_number"));
        o.setWeight(rs.getDouble("weight"));
        o.setPrice(rs.getDouble("price"));
        o.setCreatedAt(rs.getString("created_at"));
        o.setStatus(status);
        String fn = rs.getString("first_name");
        String ln = rs.getString("last_name");
        if (fn != null && ln != null) o.setCustomerName(fn + " " + ln);
        return o;
    }
    
    private Order mapWithStatus(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setServices(rs.getString("services"));
        o.setServiceType(rs.getString("service_type"));
        o.setQueueNumber(rs.getInt("queue_number"));
        o.setWeight(rs.getDouble("weight"));
        o.setPrice(rs.getDouble("price"));
        o.setCreatedAt(rs.getString("created_at"));
        o.setStatus(rs.getString("status"));
        String fn = rs.getString("first_name");
        String ln = rs.getString("last_name");
        if (fn != null && ln != null) o.setCustomerName(fn + " " + ln);
        return o;
    }
}