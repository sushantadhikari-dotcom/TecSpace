# TecSpace – University Student Portal

A full-stack university management portal built with a **Spring Boot + MongoDB** backend and a **React (Vite)** frontend.

---

## 🛠️ Prerequisites

Ensure you have the following software installed before setting up the project:

| Tool | Recommended Version | Note |
| :--- | :--- | :--- |
| **Java JDK** | `17` or newer | Backend runtime |
| **Maven** | `3.9+` | Or use the built-in Maven wrapper in IntelliJ/Eclipse |
| **MongoDB Community Server** | `6.0` or newer | Running on default port `27017` |
| **Node.js** | `18` or newer | Frontend runtime & package manager |
| **Postman** | *Optional* | Recommended for API testing |

---

## 🚀 Quick Start & Setup

### 1. Start MongoDB
* **Windows:** Runs automatically as a service post-installation.
* **macOS:** `brew services start mongodb-community`
* **Linux:** `sudo systemctl start mongod`

> **Note:** The `tecspace` database and all required collections are created automatically on first run.

---

### 2. Run the Backend

```bash
cd backend
mvn spring-boot:run