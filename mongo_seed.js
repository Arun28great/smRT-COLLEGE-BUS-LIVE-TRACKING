const { MongoClient } = require('mongodb');

// Connection URI
const uri = "mongodb+srv://arshatestid_db:Abc123.%40@arshatest.exg6eut.mongodb.net/?appName=arshatest";
const dbName = "smart_college_bus";

const get_current_time = () => {
    const now = new Date();
    let hours = now.getHours();
    const minutes = now.getMinutes().toString().padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12; // the hour '0' should be '12'
    return `${hours}:${minutes} ${ampm}`;
};

const buses = [
    {
        "_id": "bus-1",
        "number": "TN-07-CS-4201",
        "driverId": "driver-1",
        "driverName": "Driver Selvam",
        "driverPhone": "+91 9772134567",
        "routeId": "route-1",
        "routeName": "Adyar-Campus Shuttle",
        "status": "RUNNING",
        "speedKmh": 45,
        "currentLat": 13.0105,
        "currentLng": 80.2402,
        "etaMinutes": 12,
        "nextStop": "Hostel Zone",
        "totalCapacity": 50,
        "activeBoarded": 32,
        "batteryPercent": 92,
        "maintenanceDue": "2026-08-15"
    },
    {
        "_id": "bus-2",
        "number": "TN-11-AA-9876",
        "driverId": "driver-2",
        "driverName": "Driver Mani",
        "driverPhone": "+91 9845566778",
        "routeId": "route-2",
        "routeName": "Tambaram Express Line",
        "status": "DELAYED",
        "speedKmh": 15,
        "currentLat": 12.9680,
        "currentLng": 80.1650,
        "etaMinutes": 28,
        "nextStop": "Guindy Kathipara",
        "totalCapacity": 60,
        "activeBoarded": 54,
        "batteryPercent": 68,
        "maintenanceDue": "2026-08-20"
    }
];

const routes = [
    {
        "_id": "route-1",
        "routeName": "Adyar-Campus Shuttle",
        "stops": [
            {"stopName": "Main Gate", "latitude": 13.0064, "longitude": 80.2443, "scheduledTime": "08:15 AM"},
            {"stopName": "Gajendra Circle", "latitude": 13.0105, "longitude": 80.2402, "scheduledTime": "08:22 AM"},
            {"stopName": "Hostel Zone", "latitude": 13.0142, "longitude": 80.2309, "scheduledTime": "08:28 AM"},
            {"stopName": "Adyar Depot Stop", "latitude": 13.0035, "longitude": 80.2520, "scheduledTime": "08:40 AM"},
            {"stopName": "Tidel Park Circle", "latitude": 12.9890, "longitude": 80.2464, "scheduledTime": "08:50 AM"}
        ],
        "pathCoordinates": [
            [13.0064, 80.2443],
            [13.0080, 80.2425],
            [13.0105, 80.2402],
            [13.0120, 80.2350],
            [13.0142, 80.2309],
            [13.0035, 80.2520],
            [12.9890, 80.2464]
        ]
    },
    {
        "_id": "route-2",
        "routeName": "Tambaram Express Line",
        "stops": [
            {"stopName": "Tambaram Station", "latitude": 12.9250, "longitude": 80.1200, "scheduledTime": "07:45 AM"},
            {"stopName": "Chromepet Bus Bay", "latitude": 12.9510, "longitude": 80.1410, "scheduledTime": "07:55 AM"},
            {"stopName": "Pallavaram Jn", "latitude": 12.9680, "longitude": 80.1650, "scheduledTime": "08:05 AM"},
            {"stopName": "Guindy Kathipara", "latitude": 13.0080, "longitude": 80.2050, "scheduledTime": "08:25 AM"},
            {"stopName": "Campus Main Block", "latitude": 13.0118, "longitude": 80.2354, "scheduledTime": "08:45 AM"}
        ],
        "pathCoordinates": [
            [12.9250, 80.1200],
            [12.9510, 80.1410],
            [12.9680, 80.1650],
            [13.0080, 80.2050],
            [13.0118, 80.2354]
        ]
    }
];

const users = [
    {
        "_id": "student-1",
        "name": "Arun Kumar",
        "email": "student@college.edu",
        "role": "STUDENT",
        "phoneNumber": "+91 9443210987",
        "assignedBusId": "bus-1",
        "regNo": "CS2026402",
        "profilePhoto": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop",
        "password": "123456"
    },
    {
        "_id": "parent-1",
        "name": "Ramanathan K.",
        "email": "parent@college.edu",
        "role": "PARENT",
        "phoneNumber": "+91 9840123456",
        "assignedBusId": "bus-1",
        "studentName": "Arun Kumar",
        "profilePhoto": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop",
        "password": "123456"
    },
    {
        "_id": "driver-1",
        "name": "Driver Selvam",
        "email": "driver@college.edu",
        "role": "DRIVER",
        "phoneNumber": "+91 9772134567",
        "assignedBusId": "bus-1",
        "profilePhoto": "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop",
        "password": "123456"
    },
    {
        "_id": "admin-1",
        "name": "Transport Admin Sridhar",
        "email": "admin@college.edu",
        "role": "ADMIN",
        "phoneNumber": "+91 9003214567",
        "profilePhoto": "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop",
        "password": "123456"
    }
];

const notifications = [
    {
        "title": "Bus Trip Started",
        "body": "Bus TN-07-CS-4201 has left the first stop Main Gate on route Adyar-Campus Shuttle.",
        "timestamp": get_current_time(),
        "category": "STARTED"
    },
    {
        "title": "ETA Update",
        "body": "Your bus has changed speed. Current ETA to Campus: 12 mins.",
        "timestamp": get_current_time(),
        "category": "NEAR_STOP"
    },
    {
        "title": "Route Delay",
        "body": "Bus TN-11-AA-9876 is experiencing a minor 10 mins delay due to high traffic.",
        "timestamp": get_current_time(),
        "category": "DELAYED"
    }
];

const emergency_alerts = [
    {
        "_id": "sos-1",
        "busId": "bus-2",
        "busNumber": "TN-11-AA-9876",
        "driverName": "Driver Mani",
        "latitude": 12.9680,
        "longitude": 80.1650,
        "message": "Heavy traffic congestion & flat tire alert reported near Pallavaram Jn.",
        "timestamp": "08:10 AM",
        "isResolved": false
    }
];

async function seed() {
    console.log("Connecting to MongoDB Atlas...");
    const client = new MongoClient(uri);

    try {
        await client.connect();
        console.log("✅ Successfully connected to MongoDB Atlas!");
        const db = client.db(dbName);

        // Seed Users
        console.log("\n[1/5] Seeding 'users' collection...");
        const usersCol = db.collection('users');
        for (const user of users) {
            const existing = await usersCol.findOne({ email: user.email });
            if (!existing) {
                await usersCol.insertOne(user);
                console.log(`  + Added user: ${user.email} (${user.role})`);
            } else {
                await usersCol.updateOne(
                    { _id: existing._id },
                    { $set: { password: user.password, role: user.role, phoneNumber: user.phoneNumber, name: user.name } }
                );
                console.log(`  ~ Synced existing user: ${user.email}`);
            }
        }

        // Seed Buses
        console.log("\n[2/5] Seeding 'buses' collection...");
        const busesCol = db.collection('buses');
        for (const bus of buses) {
            await busesCol.updateOne({ _id: bus._id }, { $set: bus }, { upsert: true });
            console.log(`  ~ Synced bus: ${bus.number}`);
        }

        // Seed Routes
        console.log("\n[3/5] Seeding 'routes' collection...");
        const routesCol = db.collection('routes');
        for (const route of routes) {
            await routesCol.updateOne({ _id: route._id }, { $set: route }, { upsert: true });
            console.log(`  ~ Synced route: ${route.routeName}`);
        }

        // Seed Notifications
        console.log("\n[4/5] Seeding 'notifications' collection...");
        const notifyCol = db.collection('notifications');
        const count = await notifyCol.countDocuments({});
        if (count === 0) {
            await notifyCol.insertMany(notifications);
            console.log(`  + Added ${notifications.length} notifications.`);
        } else {
            console.log("  ~ Notifications already seeded.");
        }

        // Seed Emergency Alerts
        console.log("\n[5/5] Seeding 'emergency_alerts' collection...");
        const sosCol = db.collection('emergency_alerts');
        for (const alert of emergency_alerts) {
            await sosCol.updateOne({ _id: alert._id }, { $set: alert }, { upsert: true });
            console.log(`  ~ Synced emergency alert: ${alert._id}`);
        }

        console.log("\n🎉 MongoDB Database successfully created, pushed, and verified!");
        console.log("Database: smart_college_bus");
        console.log("Collections: users, buses, routes, notifications, emergency_alerts");
    } catch (err) {
        console.error("❌ Seeding failed with error:", err);
    } finally {
        await client.close();
    }
}

seed();
