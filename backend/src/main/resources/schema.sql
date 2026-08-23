-- ---------------------------------------------------------------------------
-- Reference schema for the AI-Powered Developer Knowledge & Policy Assistant.
-- This file is NOT executed automatically (spring.sql.init.mode=never).
-- Hibernate (ddl-auto=update) creates/maintains these tables for you.
-- Kept here for documentation, manual setup, and grading purposes.
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS devassistant;
USE devassistant;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- ADMIN | DEVELOPER
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    current_version_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS policy_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    file_name VARCHAR(255),
    effective_date DATE,
    status VARCHAR(20) NOT NULL, -- ACTIVE | ARCHIVED
    content LONGTEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE CASCADE,
    INDEX idx_policy_versions_policy_id (policy_id),
    INDEX idx_policy_versions_status (status)
);

CREATE TABLE IF NOT EXISTS policy_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_version_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (policy_version_id) REFERENCES policy_versions(id) ON DELETE CASCADE,
    INDEX idx_policy_chunks_version_id (policy_version_id)
);

CREATE TABLE IF NOT EXISTS repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    github_url VARCHAR(500) NOT NULL,
    default_branch VARCHAR(100) DEFAULT 'main',
    connected_by BIGINT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (connected_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS code_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    language VARCHAR(50),
    content LONGTEXT,
    last_updated DATETIME,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    INDEX idx_code_files_repo_id (repository_id)
);

CREATE TABLE IF NOT EXISTS commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    commit_hash VARCHAR(64) NOT NULL,
    message TEXT,
    author VARCHAR(255),
    commit_date DATETIME,
    changed_files TEXT,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    INDEX idx_commits_repo_id (repository_id),
    INDEX idx_commits_hash (commit_hash)
);

CREATE TABLE IF NOT EXISTS queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL, -- POLICY | CODE | GIT_HISTORY | COMBINED | GENERAL
    answer LONGTEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS query_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL, -- POLICY | CODE | COMMIT
    source_id BIGINT,
    source_label VARCHAR(500),
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE
);
