# TecSpace – University Student Portal

Spring Boot + MongoDB backend, React (Vite) frontend.

## 1. What you need installed
| Tool | Version |
|------|---------|
| Java JDK | 17 or newer |
| Maven | 3.9+ (or use the Maven built into IntelliJ/Eclipse) |
| MongoDB Community Server | 6 or newer, running on `localhost:27017` |
| Node.js | 18 or newer |
| Postman | optional, for API testing |

## 2. Start MongoDB
Windows: MongoDB runs as a service after install. Mac: `brew services start mongodb-community`.
Linux: `sudo systemctl start mongod`.
The `tecspace` database and all collections are created automatically.

## 3. Run the backend
```bash
cd backend
mvn spring-boot:run
```
It starts at http://localhost:8080. On first start it creates an admin account:

- Email: `admin@tecspace.edu`
- Password: `Admin@12345`

Change these (and `tecspace.jwt.secret`) in `backend/src/main/resources/application.properties`
before you deploy anywhere real.

IntelliJ users: open the `backend` folder as a Maven project, enable Lombok annotation processing
(Settings > Build > Compiler > Annotation Processors), then run `TecSpaceApplication`.

## 4. Run the frontend
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. If your backend runs on a different address, edit `frontend/.env`.

## 5. Try it
1. Sign up a student at `/signup`, then check Fees, Grades and Library card.
2. Log out, log in as the admin, open **Students > Manage**, set fees and enter grades.
3. Log back in as the student to see the changes. CGPA is calculated by the server.

## 6. Postman testing
Base URL: `http://localhost:8080/api`

| Request | Body / headers |
|---------|----------------|
| `POST /auth/signup` | `{ "universityEmail":"alex@univ.edu","password":"StrongPassword123!","firstName":"Alex","lastName":"Smith","mobileNumber":"+19876543210","faculty":"BSIT","session":"FALL" }` |
| `POST /auth/login` | `{ "universityEmail":"alex@univ.edu","password":"StrongPassword123!" }` → copy `token` |
| `GET /student/financials` | Header `Authorization: Bearer <token>` |
| `POST /student/payment-sessions` | Bearer token. Creates a QR for the full remaining balance (no body) |
| `GET /student/payment-sessions/active` | Bearer token. Current live QR, or 204 |
| `GET /student/payment-sessions/{token}` | Bearer token. Poll status: PENDING, SCANNED, COMPLETED, EXPIRED |
| `POST /pay/{token}/scan` | No login. What the scanned QR opens |
| `POST /pay/{token}/confirm` | No login. Demo payer confirms |
| `POST /admin/financials/{studentId}/cash` | Admin token + `{ "amount": 500 }` |
| `GET /student/grades` | Bearer token |
| `POST /student/library-card/apply` | Bearer token |
| `GET /student/library-card` | Bearer token |
| `GET /admin/students` | Admin bearer token (login as admin) – copy a student `id` |
| `PUT /admin/financials/{studentId}` | Admin token + `{ "totalFee": 12000, "paidFee": 8000 }` |
| `PUT /admin/grades/{studentId}` | Admin token + body below |

```json
{
  "trimesters": [
    {
      "trimesterName": "Trimester 1 - Fall 2026",
      "courses": [
        { "courseCode": "BSIT101", "courseName": "Web Development", "grade": "A", "creditHours": 3 },
        { "courseCode": "MTH102", "courseName": "Discrete Math", "grade": "A-", "creditHours": 3 }
      ]
    }
  ]
}
```

## 7. How payments work
- Students **cannot type or edit amounts**. They press *Generate QR*; the server fixes the amount at the full remaining balance.
- When the QR is scanned its card shows **"QR scanned - waiting for the payment to complete"**, then the balance updates when the payment completes.
- Cash: the admin opens **Students > Manage > Record a cash payment**.
- Demo: the scan page `/pay/<token>` stands in for the payer's banking app (open the link under the QR in another tab).
  `tecspace.payment.simulate-confirm=true` allows this. For real money set it to `false` and confirm payments from your provider's webhook.
- Testing with a real phone: run the portal via your PC's IP (e.g. `http://192.168.1.20:5173`), set `VITE_API_URL=http://192.168.1.20:8080/api` in `frontend/.env`, and add that origin to `tecspace.cors.allowed-origins` (comma separated).

## 8. Project layout
```
tecspace/
├── backend/   (model, repository, dto, security, service, controller, config, exception)
└── frontend/  (src/pages, src/components, AuthContext, api.js)
```

## 9. Notes
- QR payment confirmation is a simulation. For real money, verify the payment with your provider before recording it.
- Public signup can only create students. Admins are created by the seeder or directly in MongoDB.
- Passwords are hashed with BCrypt; tokens expire after 24 hours.
