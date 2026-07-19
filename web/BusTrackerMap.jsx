import React, { useState, useEffect, useMemo } from 'react';
import { 
  GoogleMap, 
  useJsApiLoader, 
  Marker, 
  InfoWindow 
} from '@react-google-maps/api';
import { 
  Bus as BusIcon, 
  Search, 
  Filter, 
  Compass, 
  Navigation, 
  Phone, 
  User, 
  Battery, 
  AlertTriangle, 
  Clock,
  Database,
  RefreshCw,
  Sliders,
  MapPin
} from 'lucide-react';

/**
 * Premium Dark Map Styling for the Google Map
 */
const DARK_MAP_STYLE = [
  { elementType: "geometry", stylers: [{ color: "#1e293b" }] },
  { elementType: "labels.text.stroke", stylers: [{ color: "#1e293b" }] },
  { elementType: "labels.text.fill", stylers: [{ color: "#748ca3" }] },
  {
    featureType: "administrative.locality",
    elementType: "labels.text.fill",
    stylers: [{ color: "#94a3b8" }],
  },
  {
    featureType: "poi",
    elementType: "labels.text.fill",
    stylers: [{ color: "#cbd5e1" }],
  },
  {
    featureType: "poi.park",
    elementType: "geometry",
    stylers: [{ color: "#0f172a" }],
  },
  {
    featureType: "poi.park",
    elementType: "labels.text.fill",
    stylers: [{ color: "#475569" }],
  },
  {
    featureType: "road",
    elementType: "geometry",
    stylers: [{ color: "#334155" }],
  },
  {
    featureType: "road",
    elementType: "geometry.stroke",
    stylers: [{ color: "#1e293b" }],
  },
  {
    featureType: "road",
    elementType: "labels.text.fill",
    stylers: [{ color: "#94a3b8" }],
  },
  {
    featureType: "road.highway",
    elementType: "geometry",
    stylers: [{ color: "#1e293b" }],
  },
  {
    featureType: "road.highway",
    elementType: "geometry.stroke",
    stylers: [{ color: "#0f172a" }],
  },
  {
    featureType: "road.highway",
    elementType: "labels.text.fill",
    stylers: [{ color: "#f1f5f9" }],
  },
  {
    featureType: "transit",
    elementType: "geometry",
    stylers: [{ color: "#1e293b" }],
  },
  {
    featureType: "transit.station",
    elementType: "labels.text.fill",
    stylers: [{ color: "#cbd5e1" }],
  },
  {
    featureType: "water",
    elementType: "geometry",
    stylers: [{ color: "#0f172a" }],
  },
  {
    featureType: "water",
    elementType: "labels.text.fill",
    stylers: [{ color: "#475569" }],
  },
  {
    featureType: "water",
    elementType: "labels.text.stroke",
    stylers: [{ color: "#0f172a" }],
  },
];

// Map configuration
const containerStyle = {
  width: '100%',
  height: '100%'
};

// Center of college campus
const defaultCenter = {
  lat: 13.0064,
  lng: 80.2443
};

export default function BusTrackerMap({ googleMapsApiKey }) {
  const { isLoaded } = useJsApiLoader({
    id: 'google-map-script',
    googleMapsApiKey: googleMapsApiKey || "YOUR_GOOGLE_MAPS_API_KEY"
  });

  // Bus states
  const [buses, setBuses] = useState([
    {
      id: "bus-1",
      number: "TN-07-CS-4201",
      driverName: "Driver Selvam",
      driverPhone: "+91 9772134567",
      routeName: "Adyar-Campus Shuttle",
      status: "RUNNING",
      speedKmh: 45,
      currentLat: 13.0105,
      currentLng: 80.2402,
      etaMinutes: 12,
      nextStop: "Hostel Zone",
      totalCapacity: 50,
      activeBoarded: 32,
      batteryPercent: 92
    },
    {
      id: "bus-2",
      number: "TN-11-AA-9876",
      driverName: "Driver Mani",
      driverPhone: "+91 9845566778",
      routeName: "Tambaram Express Line",
      status: "DELAYED",
      speedKmh: 15,
      currentLat: 12.9680,
      currentLng: 80.1650,
      etaMinutes: 28,
      nextStop: "Guindy Kathipara",
      totalCapacity: 60,
      activeBoarded: 54,
      batteryPercent: 68
    }
  ]);

  const [selectedBus, setSelectedBus] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [map, setMap] = useState(null);
  const [isSyncing, setIsSyncing] = useState(false);

  // Live Simulation updates
  useEffect(() => {
    const interval = setInterval(() => {
      setBuses(prevBuses => 
        prevBuses.map(bus => {
          if (bus.status === "RUNNING") {
            const latDelta = (Math.random() - 0.5) * 0.0015;
            const lngDelta = (Math.random() - 0.5) * 0.0015;
            return {
              ...bus,
              currentLat: bus.currentLat + latDelta,
              currentLng: bus.currentLng + lngDelta,
              speedKmh: Math.floor(35 + Math.random() * 20),
              etaMinutes: Math.max(1, bus.etaMinutes - (Math.random() > 0.7 ? 1 : 0))
            };
          }
          return bus;
        })
      );
    }, 4000);

    return () => clearInterval(interval);
  }, []);

  // Fetch / Sync with MongoDB Atlas manually
  const handleSyncMongoDB = async () => {
    setIsSyncing(true);
    // Simulate fetching latest bus coordinates from MongoDB Collection
    setTimeout(() => {
      setIsSyncing(false);
    }, 1200);
  };

  // Filtered buses
  const filteredBuses = useMemo(() => {
    return buses.filter(bus => {
      const matchesSearch = bus.number.toLowerCase().includes(searchQuery.toLowerCase()) || 
                            bus.routeName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                            bus.driverName.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesStatus = filterStatus === "ALL" || bus.status === filterStatus;
      return matchesSearch && matchesStatus;
    });
  }, [buses, searchQuery, filterStatus]);

  const onLoad = React.useCallback(function callback(mapInstance) {
    setMap(mapInstance);
  }, []);

  const onUnmount = React.useCallback(function callback(mapInstance) {
    setMap(null);
  }, []);

  const handleSelectBus = (bus) => {
    setSelectedBus(bus);
    if (map) {
      map.panTo({ lat: bus.currentLat, lng: bus.currentLng });
      map.setZoom(15);
    }
  };

  // Helper colors
  const getStatusColor = (status) => {
    switch (status) {
      case 'RUNNING': return 'bg-emerald-500 text-emerald-50';
      case 'DELAYED': return 'bg-amber-500 text-amber-50';
      case 'BREAKDOWN': return 'bg-rose-500 text-rose-50';
      default: return 'bg-slate-500 text-slate-50';
    }
  };

  const getMarkerIcon = (status) => {
    // Custom SVG paths for Google Map Pin based on status
    const color = status === "RUNNING" ? "#10b981" : status === "DELAYED" ? "#f59e0b" : "#64748b";
    return {
      path: "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z",
      fillColor: color,
      fillOpacity: 1,
      strokeColor: "#ffffff",
      strokeWeight: 1.5,
      scale: 1.8,
      anchor: isLoaded ? new window.google.maps.Point(12, 24) : null
    };
  };

  return (
    <div className="flex flex-col lg:flex-row h-screen w-full bg-slate-950 text-slate-100 font-sans overflow-hidden">
      
      {/* LEFT SIDEBAR - List and Filters */}
      <div className="w-full lg:w-96 flex flex-col border-b lg:border-b-0 lg:border-r border-slate-800 bg-slate-900 z-10">
        
        {/* Header */}
        <div className="p-4 border-b border-slate-800">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-2">
              <BusIcon className="h-6 w-6 text-sky-400" />
              <h1 className="text-lg font-bold tracking-tight">Smart Bus Tracker</h1>
            </div>
            <span className="text-xs font-semibold bg-sky-950/50 text-sky-400 px-2 py-1 rounded-full border border-sky-800/30 flex items-center space-x-1">
              <Database className="h-3 w-3 mr-1 animate-pulse" />
              MongoDB Connected
            </span>
          </div>
          <p className="text-xs text-slate-400">Real-time GPS Monitoring & Analytics</p>
        </div>

        {/* Sync Controls */}
        <div className="px-4 py-2 bg-slate-900 border-b border-slate-800 flex justify-between items-center text-xs">
          <span className="text-slate-400">MongoDB Sync Mode</span>
          <button 
            onClick={handleSyncMongoDB} 
            disabled={isSyncing}
            className="flex items-center space-x-1 text-sky-400 hover:text-sky-300 transition-colors bg-sky-950/20 px-2.5 py-1 rounded border border-sky-800/30"
          >
            <RefreshCw className={`h-3 w-3 ${isSyncing ? 'animate-spin' : ''}`} />
            <span>{isSyncing ? 'Syncing...' : 'Sync Atlas'}</span>
          </button>
        </div>

        {/* Search */}
        <div className="p-4 space-y-3 border-b border-slate-800">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search bus, route, or driver..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-4 py-2 text-sm focus:outline-none focus:border-sky-500 text-slate-100 placeholder:text-slate-500 transition-all"
            />
          </div>

          {/* Quick Filters */}
          <div className="flex items-center space-x-1">
            <Filter className="h-3.5 w-3.5 text-slate-400" />
            <span className="text-xs text-slate-400 mr-2">Status:</span>
            {["ALL", "RUNNING", "DELAYED"].map((status) => (
              <button
                key={status}
                onClick={() => setFilterStatus(status)}
                className={`text-xs px-2.5 py-1 rounded-full font-medium transition-colors ${
                  filterStatus === status 
                    ? 'bg-sky-500 text-white' 
                    : 'bg-slate-800 text-slate-400 hover:bg-slate-700'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
        </div>

        {/* Bus List */}
        <div className="flex-1 overflow-y-auto divide-y divide-slate-800/65 custom-scrollbar">
          {filteredBuses.length === 0 ? (
            <div className="p-8 text-center text-slate-500">
              <Sliders className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p className="text-sm">No active buses found</p>
            </div>
          ) : (
            filteredBuses.map((bus) => (
              <div
                key={bus.id}
                onClick={() => handleSelectBus(bus)}
                className={`p-4 cursor-pointer hover:bg-slate-800/50 transition-all ${
                  selectedBus?.id === bus.id ? 'bg-slate-800 border-l-4 border-sky-500' : ''
                }`}
              >
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h3 className="font-bold text-slate-100 text-sm tracking-wide">{bus.number}</h3>
                    <p className="text-xs text-slate-400 font-medium">{bus.routeName}</p>
                  </div>
                  <span className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase tracking-wider ${getStatusColor(bus.status)}`}>
                    {bus.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-y-1.5 text-xs text-slate-400 mt-2">
                  <div className="flex items-center space-x-1">
                    <Navigation className="h-3 w-3 text-sky-400 shrink-0" />
                    <span className="truncate">{bus.nextStop}</span>
                  </div>
                  <div className="flex items-center space-x-1 justify-end">
                    <Clock className="h-3 w-3 text-emerald-400 shrink-0" />
                    <span>ETA {bus.etaMinutes} mins</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* RIGHT AREA - Google Maps View */}
      <div className="flex-1 h-2/3 lg:h-full relative bg-slate-900">
        {isLoaded ? (
          <GoogleMap
            mapContainerStyle={containerStyle}
            center={defaultCenter}
            zoom={12}
            onLoad={onLoad}
            onUnmount={onUnmount}
            options={{
              styles: DARK_MAP_STYLE,
              disableDefaultUI: false,
              zoomControl: true,
              mapTypeControl: false,
              streetViewControl: false,
              fullscreenControl: true
            }}
          >
            {buses.map((bus) => (
              <Marker
                key={bus.id}
                position={{ lat: bus.currentLat, lng: bus.currentLng }}
                icon={getMarkerIcon(bus.status)}
                onClick={() => handleSelectBus(bus)}
                title={bus.number}
              />
            ))}

            {selectedBus && (
              <InfoWindow
                position={{ lat: selectedBus.currentLat, lng: selectedBus.currentLng }}
                onCloseClick={() => setSelectedBus(null)}
              >
                <div className="p-2 text-slate-900 max-w-xs font-sans">
                  <div className="flex justify-between items-center mb-1">
                    <h4 className="font-bold text-sm text-slate-950">{selectedBus.number}</h4>
                    <span className="text-[10px] bg-slate-200 px-1.5 py-0.5 rounded font-bold text-slate-800">
                      {selectedBus.status}
                    </span>
                  </div>
                  <p className="text-xs text-slate-600 mb-1">{selectedBus.routeName}</p>
                  <p className="text-xs font-medium text-sky-600">Next: {selectedBus.nextStop}</p>
                  <div className="mt-2 pt-2 border-t border-slate-100 flex justify-between text-xs text-slate-500">
                    <span>Speed: {selectedBus.speedKmh} km/h</span>
                    <span>ETA: {selectedBus.etaMinutes}m</span>
                  </div>
                </div>
              </InfoWindow>
            )}
          </GoogleMap>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-slate-400 bg-slate-900 space-y-4">
            <Compass className="h-12 w-12 text-sky-500 animate-spin" />
            <p className="text-sm">Loading Google Maps API...</p>
          </div>
        )}

        {/* FLOATING DETAILED METRICS PANEL (When Bus Selected) */}
        {selectedBus && (
          <div className="absolute bottom-4 left-4 right-4 lg:left-auto lg:right-4 lg:w-96 bg-slate-900/95 backdrop-blur-md rounded-xl border border-slate-800 p-5 shadow-2xl text-xs space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-300">
            <div className="flex justify-between items-start">
              <div>
                <span className="text-xs text-sky-400 font-semibold tracking-wider uppercase">Active Telemetry</span>
                <h2 className="text-base font-extrabold text-slate-100 mt-0.5">{selectedBus.number}</h2>
                <p className="text-slate-400 font-medium">{selectedBus.routeName}</p>
              </div>
              <button 
                onClick={() => setSelectedBus(null)}
                className="text-slate-500 hover:text-slate-300 transition-colors p-1"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="bg-slate-950/60 rounded-lg p-2.5 border border-slate-800/50">
                <p className="text-[10px] text-slate-500 font-bold uppercase">Speed</p>
                <p className="text-sm font-extrabold text-sky-400 mt-1">{selectedBus.speedKmh} <span className="text-[10px] text-slate-400">km/h</span></p>
              </div>
              <div className="bg-slate-950/60 rounded-lg p-2.5 border border-slate-800/50">
                <p className="text-[10px] text-slate-500 font-bold uppercase">Capacity</p>
                <p className="text-sm font-extrabold text-amber-400 mt-1">{selectedBus.activeBoarded}/{selectedBus.totalCapacity}</p>
              </div>
              <div className="bg-slate-950/60 rounded-lg p-2.5 border border-slate-800/50">
                <p className="text-[10px] text-slate-500 font-bold uppercase">Power</p>
                <p className="text-sm font-extrabold text-emerald-400 mt-1 flex items-center justify-center">
                  <Battery className="h-3 w-3 mr-1" />
                  {selectedBus.batteryPercent}%
                </p>
              </div>
            </div>

            <div className="space-y-2 pt-1 border-t border-slate-800">
              <div className="flex items-center justify-between text-slate-400">
                <div className="flex items-center space-x-2">
                  <User className="h-3.5 w-3.5 text-slate-500" />
                  <span>Driver</span>
                </div>
                <span className="font-semibold text-slate-200">{selectedBus.driverName}</span>
              </div>
              <div className="flex items-center justify-between text-slate-400">
                <div className="flex items-center space-x-2">
                  <Phone className="h-3.5 w-3.5 text-slate-500" />
                  <span>Contact</span>
                </div>
                <span className="font-semibold text-slate-200 hover:text-sky-400 transition-colors cursor-pointer">{selectedBus.driverPhone}</span>
              </div>
              <div className="flex items-center justify-between text-slate-400">
                <div className="flex items-center space-x-2">
                  <MapPin className="h-3.5 w-3.5 text-slate-500" />
                  <span>Next Stop</span>
                </div>
                <span className="font-semibold text-sky-400">{selectedBus.nextStop}</span>
              </div>
            </div>
          </div>
        )}
      </div>

    </div>
  );
}
