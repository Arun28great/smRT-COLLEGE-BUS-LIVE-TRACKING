# Smart College Bus - Web Tracker Dashboard

This folder contains a fully customizable, premium **React component (`BusTrackerMap.jsx`)** built using the Google Maps API. It tracks the real-time GPS locations of college buses synchronized directly with your MongoDB database collections populated by the companion Android app.

---

## 🚀 Features

-   **Google Maps Integration:** Renders live, responsive marker locations with customized styled marker pins indicating bus status (Green = Active/Running, Amber = Delayed, Slate/Red = Idle/Breakdown).
-   **Premium Dark UI Theme:** Clean slate-gray aesthetics matching the main mobile companion app, maximizing visual scanning and visibility.
-   **Real-time Coordinates Updates:** Automatic updates simulation built-in; ready to swap with a fetch/sockets stream from your backend server.
-   **Multi-Attribute Filters:** Instant real-time filtering by bus number, assigned route, or driver names.
-   **Full Driver & Telemetry Panels:** Dynamic cards displaying details (battery percentage, passenger load capacity counts, next scheduled stops, and direct-call driver contact shortcuts).

---

## 🛠️ Step-by-Step Integration

### 1. Install Dependencies

Install the official React Google Maps adapter and the lightweight Lucide icon pack:

```bash
npm install @react-google-maps/api lucide-react
```

### 2. Add Component to your React/Next.js Application

Import the component and provide your Google Maps API Key:

```jsx
import React from 'react';
import BusTrackerMap from './components/BusTrackerMap';

function App() {
  return (
    <div className="w-screen h-screen">
      {/* Renders full screen live dashboard map */}
      <BusTrackerMap googleMapsApiKey="AIzaSyYourGoogleMapsApiKeyHere" />
    </div>
  );
}

export default App;
```

---

## 🔌 Connecting to Your MongoDB Atlas Backend

The Android application uploads the live coordinates directly to your MongoDB Database `smart_college_bus` in the `buses` collection. Here is an easy Express backend example to fetch these coordinates and supply them to your React client:

### Node.js Express API Endpoint (`server.js`)

```javascript
const express = require('express');
const { MongoClient } = require('mongodb');
const cors = require('cors');

const app = express();
app.use(cors());

// Configure your Atlas URI (same string as BuildConfig.MONGODB_URI)
const uri = process.env.MONGODB_URI || "mongodb+srv://<user>:<password>@cluster.mongodb.net/?retryWrites=true&w=majority";
const client = new MongoClient(uri);

app.get('/api/buses', async (req, res) => {
  try {
    await client.connect();
    const database = client.db('smart_college_bus');
    const busesCollection = database.collection('buses');
    
    // Fetch all active buses
    const buses = await busesCollection.find({}).toArray();
    res.json(buses);
  } catch (err) {
    res.status(500).json({ error: err.message });
  } finally {
    await client.close();
  }
});

app.listen(5000, () => console.log('Tracker Sync Backend running on port 5000'));
```

### Hooking up the Endpoint inside `BusTrackerMap.jsx`

Inside your `BusTrackerMap.jsx` component, replace the simulated `useEffect` or use the `Sync Atlas` button handler with the following:

```javascript
const handleSyncMongoDB = async () => {
  setIsSyncing(true);
  try {
    const response = await fetch('http://localhost:5000/api/buses');
    const data = await response.json();
    
    // Map Atlas schema document schema attributes safely to component keys
    const mappedBuses = data.map(item => ({
      id: item._id,
      number: item.number,
      driverName: item.driverName,
      driverPhone: item.driverPhone,
      routeName: item.routeName,
      status: item.status,
      speedKmh: item.speedKmh,
      currentLat: item.currentLat,
      currentLng: item.currentLng,
      etaMinutes: item.etaMinutes,
      nextStop: item.nextStop,
      totalCapacity: item.totalCapacity,
      activeBoarded: item.activeBoarded,
      batteryPercent: item.batteryPercent
    }));
    
    setBuses(mappedBuses);
  } catch (error) {
    console.error("Failed to sync from MongoDB Atlas cluster:", error);
  } finally {
    setIsSyncing(false);
  }
};
```
