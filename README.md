# Contract Approval System
Backend workflow system for managing contract creation and multi-stage approvals (Legal → Finance → Client).

## Requirements
Install the following before running:

* Java 21
* Maven 3.9+
* PostgreSQL 14+
* Git

Verify:
```
java -version
mvn -version
psql --version
```
#
## Clone Project
## Database Setup
## Run Application

```
mvn spring-boot:run
```

Server starts at:

```
http://localhost:8080
```

## Initial Setup Order (Important)

Run APIs in this sequence:

1. Create users
2. Create approval mapping
3. Create contract
4. Submit contract
5. Finance review
6. Client review

If mapping is not created, approval flow will fail.

## Core API Endpoints
Create Contract

```
POST /contracts
```
Submit Contract

```
POST /contracts/{id}/submit
```
Finance Review

```
POST /contracts/{id}/finance-review
```
Client Review

```
POST /contracts/{id}/client-review
```
Create Mapping (Admin)

```
POST /admin/approval-mappings
```

## Notes

* Contracts are editable only in DRAFT
* Rejections require remarks
* Status transitions are strictly validated
* Mapping must exist before submission


## Port Change (Optional)
`application.yml`
server:
  port: 9090

System is ready for local development after these steps.
