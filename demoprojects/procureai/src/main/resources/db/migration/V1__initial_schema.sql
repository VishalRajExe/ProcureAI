-- ProcureAI Database Schema
-- V1__initial_schema.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    department VARCHAR(100),
    phone VARCHAR(50),
    avatar_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);

-- Vendors table
CREATE TABLE vendors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(500),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    gst_number VARCHAR(50),
    pan_number VARCHAR(50),
    contact_person VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    payment_terms VARCHAR(100),
    delivery_terms VARCHAR(100),
    currency VARCHAR(10) DEFAULT 'INR',
    reliability_score DECIMAL(3,2) DEFAULT 5.00,
    total_orders INTEGER DEFAULT 0,
    on_time_delivery_rate DECIMAL(5,2) DEFAULT 0.00,
    quality_rating DECIMAL(3,2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_vendors_name ON vendors(name);
CREATE INDEX idx_vendors_email ON vendors(email);
CREATE INDEX idx_vendors_gst ON vendors(gst_number);
CREATE INDEX idx_vendors_is_active ON vendors(is_active);
CREATE INDEX idx_vendors_is_preferred ON vendors(is_preferred);

-- Quotes table
CREATE TABLE quotes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quote_number VARCHAR(50) NOT NULL UNIQUE,
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    source_type VARCHAR(50) NOT NULL DEFAULT 'PDF',
    source_filename VARCHAR(255),
    source_file_path VARCHAR(500),
    source_file_hash VARCHAR(64),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    shipping_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    valid_until DATE,
    payment_terms VARCHAR(255),
    delivery_terms VARCHAR(255),
    warranty_period VARCHAR(100),
    notes TEXT,
    ai_extracted BOOLEAN NOT NULL DEFAULT FALSE,
    ai_confidence DECIMAL(3,2),
    ai_raw_response JSONB,
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_quotes_quote_number ON quotes(quote_number);
CREATE INDEX idx_quotes_vendor_id ON quotes(vendor_id);
CREATE INDEX idx_quotes_status ON quotes(status);
CREATE INDEX idx_quotes_uploaded_by ON quotes(uploaded_by);
CREATE INDEX idx_quotes_created_at ON quotes(created_at);

-- Quote Items table
CREATE TABLE quote_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quote_id UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    sku VARCHAR(100),
    category VARCHAR(100),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit VARCHAR(50),
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount_percent DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    tax_percent DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    line_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    specifications JSONB,
    warranty VARCHAR(255),
    delivery_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quote_items_quote_id ON quote_items(quote_id);
CREATE INDEX idx_quote_items_category ON quote_items(category);

-- Benchmarks table (Market pricing data)
CREATE TABLE benchmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(100) NOT NULL,
    sub_category VARCHAR(100),
    sku VARCHAR(100),
    description TEXT,
    brand VARCHAR(100),
    min_price DECIMAL(15,2),
    avg_price DECIMAL(15,2),
    max_price DECIMAL(15,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    source VARCHAR(100),
    source_date DATE,
    region VARCHAR(100),
    quantity_tier VARCHAR(50),
    confidence DECIMAL(3,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_benchmarks_category ON benchmarks(category);
CREATE INDEX idx_benchmarks_sku ON benchmarks(sku);
CREATE INDEX idx_benchmarks_is_active ON benchmarks(is_active);

-- Negotiations table
CREATE TABLE negotiations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    negotiation_number VARCHAR(50) NOT NULL UNIQUE,
    quote_id UUID NOT NULL REFERENCES quotes(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    strategy VARCHAR(50) NOT NULL DEFAULT 'BALANCED',
    target_price DECIMAL(15,2),
    max_acceptable_price DECIMAL(15,2),
    min_warranty_months INTEGER,
    max_delivery_days INTEGER,
    max_rounds INTEGER NOT NULL DEFAULT 3,
    current_round INTEGER NOT NULL DEFAULT 0,
    target_discount_percent DECIMAL(5,2),
    required_payment_terms VARCHAR(255),
    initiated_by UUID REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,
    completed_at TIMESTAMP,
    final_price DECIMAL(15,2),
    savings_amount DECIMAL(15,2),
    savings_percent DECIMAL(5,2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_negotiations_quote_id ON negotiations(quote_id);
CREATE INDEX idx_negotiations_vendor_id ON negotiations(vendor_id);
CREATE INDEX idx_negotiations_status ON negotiations(status);
CREATE INDEX idx_negotiations_initiated_by ON negotiations(initiated_by);

-- Negotiation Rounds table
CREATE TABLE negotiation_rounds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    negotiation_id UUID NOT NULL REFERENCES negotiations(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ai_recommendation JSONB,
    ai_strategy VARCHAR(50),
    ai_target_price DECIMAL(15,2),
    ai_max_price DECIMAL(15,2),
    ai_reasoning TEXT,
    human_action VARCHAR(50),
    human_notes TEXT,
    human_modified_email TEXT,
    sent_at TIMESTAMP,
    sent_by UUID REFERENCES users(id),
    vendor_response_received_at TIMESTAMP,
    vendor_response TEXT,
    vendor_counter_price DECIMAL(15,2),
    vendor_counter_terms TEXT,
    ai_evaluation JSONB,
    ai_recommendation_action VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_negotiation_rounds_negotiation_id ON negotiation_rounds(negotiation_id);
CREATE INDEX idx_negotiation_rounds_round_number ON negotiation_rounds(negotiation_id, round_number);

-- Approvals table
CREATE TABLE approvals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    requester_id UUID NOT NULL REFERENCES users(id),
    approver_id UUID REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    ai_recommendation JSONB,
    requested_action VARCHAR(100),
    action_data JSONB,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_approvals_entity ON approvals(entity_type, entity_id);
CREATE INDEX idx_approvals_requester ON approvals(requester_id);
CREATE INDEX idx_approvals_approver ON approvals(approver_id);
CREATE INDEX idx_approvals_status ON approvals(status);

-- Email Messages table
CREATE TABLE email_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    negotiation_id UUID REFERENCES negotiations(id),
    negotiation_round_id UUID REFERENCES negotiation_rounds(id),
    direction VARCHAR(20) NOT NULL,
    from_email VARCHAR(255) NOT NULL,
    to_email VARCHAR(255) NOT NULL,
    cc_emails TEXT,
    subject VARCHAR(500),
    body TEXT,
    body_html TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    replied_at TIMESTAMP,
    error_message TEXT,
    message_id VARCHAR(255),
    in_reply_to VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_messages_negotiation ON email_messages(negotiation_id);
CREATE INDEX idx_email_messages_round ON email_messages(negotiation_round_id);
CREATE INDEX idx_email_messages_status ON email_messages(status);

-- Purchase Orders table
CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    po_number VARCHAR(50) NOT NULL UNIQUE,
    negotiation_id UUID NOT NULL REFERENCES negotiations(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    quote_id UUID NOT NULL REFERENCES quotes(id),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    shipping_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    payment_terms VARCHAR(255),
    delivery_terms VARCHAR(255),
    delivery_date DATE,
    delivery_address TEXT,
    billing_address TEXT,
    notes TEXT,
    pdf_path VARCHAR(500),
    pdf_generated_at TIMESTAMP,
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,
    sent_to_vendor_at TIMESTAMP,
    acknowledged_by_vendor_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_purchase_orders_po_number ON purchase_orders(po_number);
CREATE INDEX idx_purchase_orders_negotiation ON purchase_orders(negotiation_id);
CREATE INDEX idx_purchase_orders_vendor ON purchase_orders(vendor_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders(status);

-- Purchase Order Items table
CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    sku VARCHAR(100),
    category VARCHAR(100),
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit VARCHAR(50),
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount_percent DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    tax_percent DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    line_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    specifications JSONB,
    warranty VARCHAR(255),
    delivery_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_po_items_po_id ON purchase_order_items(purchase_order_id);

-- Workflow Executions table
CREATE TABLE workflow_executions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    current_step VARCHAR(100),
    total_steps INTEGER,
    completed_steps INTEGER DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    execution_data JSONB,
    created_by UUID REFERENCES users(id)
);

CREATE INDEX idx_workflow_executions_entity ON workflow_executions(entity_type, entity_id);
CREATE INDEX idx_workflow_executions_status ON workflow_executions(status);

-- Audit Logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id),
    user_email VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(100),
    correlation_id VARCHAR(100),
    severity VARCHAR(20) DEFAULT 'INFO',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_correlation ON audit_logs(correlation_id);

-- Procurement Rules table
CREATE TABLE procurement_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_key VARCHAR(100) NOT NULL UNIQUE,
    rule_name VARCHAR(255) NOT NULL,
    rule_description TEXT,
    rule_type VARCHAR(50) NOT NULL,
    rule_value TEXT NOT NULL,
    data_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    category VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_procurement_rules_key ON procurement_rules(rule_key);
CREATE INDEX idx_procurement_rules_category ON procurement_rules(category);
CREATE INDEX idx_procurement_rules_active ON procurement_rules(is_active);

-- Vendor Scoring Weights table
CREATE TABLE vendor_scoring_weights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price_weight DECIMAL(3,2) NOT NULL DEFAULT 0.40,
    warranty_weight DECIMAL(3,2) NOT NULL DEFAULT 0.15,
    delivery_weight DECIMAL(3,2) NOT NULL DEFAULT 0.15,
    payment_terms_weight DECIMAL(3,2) NOT NULL DEFAULT 0.10,
    reliability_weight DECIMAL(3,2) NOT NULL DEFAULT 0.20,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX idx_vendor_scoring_weights_default ON vendor_scoring_weights(is_default);