## Development Context

This document is intended as a technical reference for future development, debugging, feature enhancements, and AI-assisted implementation. It describes ZIPPO’s business rules, data model, API design principles, security considerations, and planned enhancements.

# ZIPPO — Technical Context

ZIPPO is a full-stack commuter fleet and route-management application for independent auto-rickshaw, commuter-van, and school-bus drivers. It helps drivers manage fixed pickup-and-drop routes, passenger attendance, trip capacity, delegation, communication, subscriptions, and monthly invoicing.

## Business Context

Drivers operate recurring morning and evening pickup/drop routes for school children and corporate employees. They need a simple mobile tool to manage routes, track attendance, communicate with parents or employees, avoid vehicle overloading, and maintain payment records.

## Core Users

- Driver: Manages routes, passengers, trips, attendance, communication, and billing.
- Delegated Driver: Executes an assigned trip but does not have access to financial data.
- Parent / Employee Contact: Receives proximity-based communication and can be contacted through native dialer or WhatsApp.
- Admin: Verifies subscription payments and manages platform-level access.

## Core Modules

### Route and Shift Management

The application supports separate morning and evening shifts.

- Morning shift: Home location is the pickup point; destination is the drop point.
- Evening shift: Destination is the pickup point; home location is the drop point.
- Passenger contract type can be `BOTH`, `MORNING`, or `EVENING`.
- Attendance is tracked independently for morning and evening shifts.

### Trip Capacity and Splitting

Each driver sets a vehicle seating capacity in the profile settings.

- If the number of active passengers exceeds capacity, the system splits the route into multiple trips.
- Drivers can manually move passengers between trips.
- The system prevents a trip from exceeding its configured capacity.
- Drivers can configure custom trip sizes to balance passenger distribution.

### Trip Delegation

A driver can delegate one or more trips to another registered driver using a unique trip assignment code.

- The original driver remains the owner of the trip.
- The delegated driver receives only operational data required to complete the trip.
- Delegated drivers cannot access ledger, invoice, subscription, or financial-report data.
- API-level permission checks restrict sensitive data access.

### Location Alerts

The Android application uses a foreground service to track location during an active trip.

- When the vehicle comes within 1 km of the next stop, the app displays a high-visibility alert.
- The driver can send a pre-filled WhatsApp message using a native deep link.
- The driver can call a parent or employee through the native dialer.
- Messages are triggered by physical proximity rather than fixed clock times.

### Financial Ledger

ZIPPO uses an invoice-based ledger model rather than storing payments directly against passenger records.

- Monthly invoices are generated for active passengers.
- One invoice is created per passenger per billing month.
- Duplicate invoice creation is prevented.
- Passenger records use a soft-delete strategy.
- Deleted passengers do not receive future invoices.
- Historical invoices remain available for reporting.
- Payments can be recorded with payment method, payment date, and UPI transaction ID/UTR number.
- Parent/household grouping allows multiple siblings to be linked to one billing profile.

### Subscription

The application is designed as a micro-SaaS product for drivers.

- New drivers receive a six-month free trial.
- After the trial, access requires a monthly subscription.
- Payments are collected through UPI QR codes.
- Admin verifies the UPI transaction ID/UTR number and activates the subscription.

## Data Model Overview

### Driver

- Name
- Mobile number
- Vehicle details
- Seating capacity
- Home and destination coordinates
- Subscription status
- Trial expiry date

### Passenger

- Name
- Parent or contact details
- Pickup and drop locations
- Contract type
- Morning and evening attendance status
- Household grouping key
- Active status

### Daily Log

- Date
- Driver ID
- Passenger attendance
- Morning status
- Evening status
- Trip assignments

### Trip Segment

- Trip number
- Driver ID
- Assigned driver ID
- Passenger list
- Pickup and drop sequence
- Capacity
- Attendance status
- Delegation status

### Invoice

- Passenger ID
- Driver ID
- Passenger name
- Billing month
- Amount
- Payment status
- Payment method
- Payment date
- Parent phone number
- UPI transaction ID/UTR number

## API Design Principles

- Use RESTful endpoints.
- Authenticate users using JWT.
- Apply role-based authorisation.
- Restrict financial data from delegated drivers.
- Use soft deletion for passenger records.
- Prevent duplicate monthly invoices.
- Validate all incoming request data.
- Store sensitive credentials only in environment variables.

## Security Considerations

- Do not expose database credentials, JWT secrets, API keys, or cloud credentials.
- Use environment variables for all secrets.
- Apply authentication and authorisation checks on every protected endpoint.
- Restrict delegated-driver access to operational trip data only.
- Validate file uploads and stored documents.
- Use HTTPS in production.
- Do not store payment credentials or full card data.

## Future Enhancements

- Holiday management module
- Subscription and renewal automation
- Supervisor dashboard
- Advanced analytics
- WebSocket-based live trip status
- Driver location tracking dashboard
- Automated payment reminders
- Route optimisation suggestions
