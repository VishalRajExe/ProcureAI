-- Demo Seed Data
-- V2__demo_seed_data.sql

-- Insert default procurement rules
INSERT INTO procurement_rules (rule_key, rule_name, rule_description, rule_type, rule_value, data_type, category, is_system, created_by) VALUES
('TARGET_PRICE_THRESHOLD', 'Target Price Threshold', 'Maximum percentage above benchmark price to consider acceptable', 'PERCENTAGE', '0.15', 'DECIMAL', 'PRICING', TRUE, NULL),
('MAX_NEGOTIATION_ROUNDS', 'Maximum Negotiation Rounds', 'Maximum number of negotiation rounds allowed', 'INTEGER', '3', 'INTEGER', 'NEGOTIATION', TRUE, NULL),
('DEFAULT_STRATEGY', 'Default Negotiation Strategy', 'Default negotiation strategy when none specified', 'ENUM', 'BALANCED', 'STRING', 'NEGOTIATION', TRUE, NULL),
('MIN_WARRANTY_MONTHS', 'Minimum Warranty Period', 'Minimum acceptable warranty in months', 'INTEGER', '12', 'INTEGER', 'TERMS', TRUE, NULL),
('MAX_DELIVERY_DAYS', 'Maximum Delivery Days', 'Maximum acceptable delivery time in days', 'INTEGER', '30', 'INTEGER', 'TERMS', TRUE, NULL),
('TARGET_DISCOUNT_PERCENT', 'Target Discount Percentage', 'Target discount percentage from quoted price', 'PERCENTAGE', '0.10', 'DECIMAL', 'PRICING', TRUE, NULL),
('REQUIRED_PAYMENT_TERMS', 'Required Payment Terms', 'Standard required payment terms', 'STRING', 'Net 30', 'STRING', 'TERMS', TRUE, NULL),
('AUTO_APPROVE_THRESHOLD', 'Auto-Approve Threshold', 'Savings percentage above which approval is auto-granted', 'PERCENTAGE', '0.20', 'DECIMAL', 'APPROVAL', TRUE, NULL),
('MAX_PRICE_DEVIATION', 'Maximum Price Deviation', 'Maximum allowed deviation from benchmark', 'PERCENTAGE', '0.25', 'DECIMAL', 'PRICING', TRUE, NULL),
('VENDOR_MIN_RELIABILITY', 'Minimum Vendor Reliability Score', 'Minimum reliability score for vendor consideration', 'DECIMAL', '3.0', 'DECIMAL', 'VENDOR', TRUE, NULL);

-- Insert default vendor scoring weights
INSERT INTO vendor_scoring_weights (name, description, price_weight, warranty_weight, delivery_weight, payment_terms_weight, reliability_weight, is_default, is_active, created_by) VALUES
('Standard Weights', 'Balanced scoring for general procurement', 0.40, 0.15, 0.15, 0.10, 0.20, TRUE, TRUE, NULL),
('Price Focused', 'Emphasizes lowest price', 0.60, 0.10, 0.10, 0.10, 0.10, FALSE, TRUE, NULL),
('Quality Focused', 'Emphasizes warranty and reliability', 0.25, 0.25, 0.15, 0.10, 0.25, FALSE, TRUE, NULL),
('Speed Focused', 'Emphasizes fast delivery', 0.30, 0.10, 0.40, 0.10, 0.10, FALSE, TRUE, NULL);

-- Insert demo users (password: Demo@123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, department, is_active, email_verified) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@procureai.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSJFdJ8iYqiSJFdJ8iYqiSJFdJ', 'Admin', 'User', 'ADMIN', 'IT', TRUE, TRUE),
('22222222-2222-2222-2222-222222222222', 'manager@procureai.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSJFdJ8iYqiSJFdJ8iYqiSJFdJ', 'Procurement', 'Manager', 'MANAGER', 'Procurement', TRUE, TRUE),
('33333333-3333-3333-3333-333333333333', 'buyer@procureai.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSJFdJ8iYqiSJFdJ8iYqiSJFdJ', 'Senior', 'Buyer', 'BUYER', 'Procurement', TRUE, TRUE),
('44444444-4444-4444-4444-444444444444', 'approver@procureai.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSJFdJ8iYqiSJFdJ8iYqiSJFdJ', 'Finance', 'Approver', 'APPROVER', 'Finance', TRUE, TRUE);

-- Insert demo vendors
INSERT INTO vendors (id, name, legal_name, email, phone, website, address_line1, city, state, postal_code, country, gst_number, pan_number, contact_person, contact_email, payment_terms, delivery_terms, currency, reliability_score, total_orders, on_time_delivery_rate, quality_rating, is_active, is_preferred, notes) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'TechCorp Solutions', 'TechCorp Solutions Private Limited', 'sales@techcorp.com', '+91-80-1234-5678', 'https://techcorp.com', '123 Tech Park, Whitefield', 'Bangalore', 'Karnataka', '560066', 'India', '29AAACT1234F1Z5', 'AAACT1234F', 'Rajesh Kumar', 'rajesh@techcorp.com', 'Net 30', 'FOB Bangalore', 'INR', 8.5, 150, 92.5, 4.7, TRUE, TRUE, 'Preferred vendor for IT hardware'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Global Laptops Inc', 'Global Laptops India Pvt Ltd', 'orders@globallaptops.in', '+91-22-9876-5432', 'https://globallaptops.in', '456 Industrial Area, Andheri East', 'Mumbai', 'Maharashtra', '400069', 'India', '27AABCG5678H2Z6', 'AABCG5678H', 'Priya Sharma', 'priya@globallaptops.in', 'Net 45', 'CIF Mumbai', 'INR', 7.8, 89, 87.2, 4.3, TRUE, FALSE, 'Good for bulk orders'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Prime Electronics', 'Prime Electronics Limited', 'procurement@prime-electronics.com', '+91-11-5555-1234', 'https://prime-electronics.com', '789 Commercial Complex, Nehru Place', 'New Delhi', 'Delhi', '110019', 'India', '07AAACP9876J1Z3', 'AAACP9876J', 'Amit Singh', 'amit@prime-electronics.com', 'Net 60', 'Ex-Works Delhi', 'INR', 7.2, 67, 82.1, 4.1, TRUE, FALSE, 'Competitive pricing on accessories');

-- Insert demo benchmarks (market pricing for laptops)
INSERT INTO benchmarks (category, sub_category, sku, description, brand, min_price, avg_price, max_price, currency, source, source_date, region, quantity_tier, confidence, is_active) VALUES
('Laptops', 'Business', 'DELL-LAT-7430', 'Dell Latitude 7430 i7/16GB/512GB', 'Dell', 78000, 82000, 88000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '1-10', 0.9, TRUE),
('Laptops', 'Business', 'DELL-LAT-7430', 'Dell Latitude 7430 i7/16GB/512GB', 'Dell', 75000, 79000, 84000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '11-50', 0.9, TRUE),
('Laptops', 'Business', 'DELL-LAT-7430', 'Dell Latitude 7430 i7/16GB/512GB', 'Dell', 72000, 76000, 80000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '51-100', 0.9, TRUE),
('Laptops', 'Business', 'HP-ELITE-840', 'HP EliteBook 840 G9 i7/16GB/512GB', 'HP', 76000, 80000, 85000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '1-10', 0.85, TRUE),
('Laptops', 'Business', 'HP-ELITE-840', 'HP EliteBook 840 G9 i7/16GB/512GB', 'HP', 73000, 77000, 82000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '11-50', 0.85, TRUE),
('Laptops', 'Business', 'HP-ELITE-840', 'HP EliteBook 840 G9 i7/16GB/512GB', 'HP', 70000, 74000, 78000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '51-100', 0.85, TRUE),
('Laptops', 'Business', 'LENOVO-T14', 'Lenovo ThinkPad T14 Gen 3 i7/16GB/512GB', 'Lenovo', 72000, 76000, 81000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '1-10', 0.88, TRUE),
('Laptops', 'Business', 'LENOVO-T14', 'Lenovo ThinkPad T14 Gen 3 i7/16GB/512GB', 'Lenovo', 69000, 73000, 77000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '11-50', 0.88, TRUE),
('Laptops', 'Business', 'LENOVO-T14', 'Lenovo ThinkPad T14 Gen 3 i7/16GB/512GB', 'Lenovo', 66000, 70000, 74000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '51-100', 0.88, TRUE),
('Laptops', 'Accessories', 'DOCK-USBC', 'USB-C Docking Station', 'Generic', 8000, 10000, 12000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '1-10', 0.8, TRUE),
('Laptops', 'Accessories', 'DOCK-USBC', 'USB-C Docking Station', 'Generic', 7000, 8500, 10000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '11-50', 0.8, TRUE),
('Laptops', 'Accessories', 'DOCK-USBC', 'USB-C Docking Station', 'Generic', 6000, 7500, 9000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '51-100', 0.8, TRUE),
('Laptops', 'Warranty', 'WARRANTY-3YR', '3 Year Extended Warranty', 'OEM', 4000, 5500, 7000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '1-10', 0.9, TRUE),
('Laptops', 'Warranty', 'WARRANTY-3YR', '3 Year Extended Warranty', 'OEM', 3500, 5000, 6500, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '11-50', 0.9, TRUE),
('Laptops', 'Warranty', 'WARRANTY-3YR', '3 Year Extended Warranty', 'OEM', 3000, 4500, 6000, 'INR', 'Market Survey Q1 2024', '2024-03-15', 'India', '51-100', 0.9, TRUE);

-- Note: Quotes, negotiations, and other transactional data will be created by the demo workflow