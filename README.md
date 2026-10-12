Markdown
# TecSpace – University Student Portal

A full-stack university management portal built with **Spring Boot**, **MongoDB**, and **React (Vite)**.

## 🛠 Tech Stack
- **Backend:** Java 17+, Spring Boot, Spring Security (JWT), Spring Data MongoDB, Lombok
- **Frontend:** React, Vite, React Router, Axios
- **Database:** MongoDB (Community Server 6.0+)

---

## ⚡ Quick Start

### 1. Prerequisites
Make sure you have **Node.js (v18+)**, **Java JDK (17+)**, **Maven**, and **MongoDB** installed and running on `localhost:27017`.

### 2. Backend Setup
```bash
cd backend
mvn spring-boot:run
Backend runs on http://localhost:8080.

Default Admin Creds: admin@tecspace.edu / Admin@12345

3. Frontend Setup
Bash
cd frontend
npm install
npm run dev
Frontend runs on http://localhost:5173.

🔑 Features
Authentication: JWT-based signup/login for students and pre-configured admin access.

Financial Tracker & QR Payments: Students can view balances and generate dynamic QR codes to settle fees.

Academic Management: Admins can enter student grades by trimester; student CGPA is automatically computed on the server.

Library Access: Request and view digital library cards online.

📂 Repository Structure
Plaintext
tecspace/
├── backend/    # Spring Boot REST API
└── frontend/   # React (Vite) UI