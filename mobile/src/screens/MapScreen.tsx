import React, { useEffect, useRef, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { WebView } from "react-native-webview";
import * as Location from "expo-location";
import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../api/client";
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { RootStackParamList } from "../navigation/AppNavigator";

const KAKAO_JS_KEY =
  process.env.EXPO_PUBLIC_KAKAO_JS_KEY ?? "9841405e090263146cdb4a323bd57f92";

type MapDebugMessage = {
  type: "debug" | "error" | "sdkLoadError" | "ready" | "roomSelected";
  event?: string;
  msg?: string;
  src?: string;
  line?: number;
  origin?: string;
  href?: string;
  hasKakao?: boolean;
  roomId?: string;
};

function buildInitialHtml(lat: number, lng: number): string {
  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <style>
    * { margin:0; padding:0; box-sizing:border-box; }
    html, body { width:100%; height:100%; overflow:hidden; }
    #map { width:100%; height:100vh; }
  </style>
  <script>
    function postToApp(payload) {
      try {
        if (window.ReactNativeWebView) {
          window.ReactNativeWebView.postMessage(JSON.stringify(payload));
        }
      } catch (err) {}
    }
    window.onerror = function(msg, src, line) {
      postToApp({type:'error', event:'window.onerror', msg:String(msg), src:src, line:line, origin:location.origin, href:location.href, hasKakao:!!window.kakao});
    };
    function escHtml(s) {
      return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
    var _map = null;
    var _markers = [];
    function initMap() {
      postToApp({type:'debug', event:'initMapCalled', origin:location.origin, href:location.href, hasKakao:!!window.kakao});
      try {
        if (!window.kakao || !kakao.maps) {
          postToApp({type:'error', event:'kakaoMissing', msg:'Kakao Maps SDK is not available after script load', origin:location.origin, href:location.href, hasKakao:!!window.kakao});
          return;
        }
        kakao.maps.load(function() {
          try {
            var container = document.getElementById('map');
            var options = { center: new kakao.maps.LatLng(${lat}, ${lng}), level: 5 };
            _map = new kakao.maps.Map(container, options);
            new kakao.maps.Marker({
              position: new kakao.maps.LatLng(${lat}, ${lng}),
              map: _map
            });
            postToApp({type:'ready', event:'mapReady', origin:location.origin, href:location.href, hasKakao:!!window.kakao});
          } catch (err) {
            postToApp({type:'error', event:'mapCreateFailed', msg:String(err && err.message ? err.message : err), origin:location.origin, href:location.href, hasKakao:!!window.kakao});
          }
        });
      } catch (err) {
        postToApp({type:'error', event:'initMapFailed', msg:String(err && err.message ? err.message : err), origin:location.origin, href:location.href, hasKakao:!!window.kakao});
      }
    }
    function updateMarkers(rooms) {
      if (!_map) {
        postToApp({type:'debug', event:'updateMarkersBeforeMapReady', origin:location.origin, href:location.href, hasKakao:!!window.kakao});
        return;
      }
      _markers.forEach(function(m) { m.setMap(null); });
      _markers = [];
      rooms.forEach(function(room) {
        var marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(room.lat, room.lng),
          map: _map,
          title: room.name
        });
        _markers.push(marker);
        var infowindow = new kakao.maps.InfoWindow({
          content: '<div style="padding:6px 10px;font-size:13px;">'+escHtml(room.name)+'<br/><small>'+escHtml(room.title)+'</small></div>'
        });
        kakao.maps.event.addListener(marker, 'click', (function(rid, iw) {
          return function() {
            iw.open(_map, marker);
            window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'roomSelected', roomId: rid }));
          };
        })(room.id, infowindow));
      });
    }
    document.addEventListener('message', function(e) {
      try {
        var msg = JSON.parse(e.data);
        if (msg.type === 'updateMarkers') updateMarkers(msg.rooms);
      } catch(err) {}
    });
    window.addEventListener('message', function(e) {
      try {
        var msg = JSON.parse(e.data);
        if (msg.type === 'updateMarkers') updateMarkers(msg.rooms);
      } catch(err) {}
    });
  </script>
</head>
<body>
  <div id="map"></div>
  <script
    src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false"
    onload="postToApp({type:'debug', event:'sdkScriptLoaded', origin:location.origin, href:location.href, hasKakao:!!window.kakao}); initMap();"
    onerror="postToApp({type:'sdkLoadError', event:'sdkScriptLoadError', origin:location.origin, href:location.href, hasKakao:!!window.kakao})">
  </script>
</body>
</html>`;
}

export function MapScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [coords, setCoords] = useState({ lat: 37.5665, lng: 126.978 });
  const [mapReady, setMapReady] = useState(false);
  const [mapError, setMapError] = useState<string | null>(null);
  const [debugInfo, setDebugInfo] = useState<string | null>(null);
  const webRef = useRef<WebView>(null);
  const htmlRef = useRef(buildInitialHtml(37.5665, 126.978));

  const { data } = useQuery({
    queryKey: ["rooms"],
    queryFn: () => apiClient.get("/api/rooms"),
    refetchInterval: 30000,
  });
  const rooms: any[] = (data as any)?.data ?? [];
  const roomsWithLocation = rooms.filter((r) => r.latitude && r.longitude);

  useEffect(() => {
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") return;
      const loc = await Location.getCurrentPositionAsync({});
      setCoords({ lat: loc.coords.latitude, lng: loc.coords.longitude });
    })();
  }, []);

  // Push marker updates via postMessage instead of reloading the WebView
  useEffect(() => {
    if (!mapReady || !webRef.current) return;
    const payload = JSON.stringify({
      type: "updateMarkers",
      rooms: roomsWithLocation.map((r) => ({
        id: r.id,
        lat: r.latitude,
        lng: r.longitude,
        name: r.restaurantName,
        title: r.title,
      })),
    });
    webRef.current.injectJavaScript(`
      try { updateMarkers(${JSON.stringify(
        roomsWithLocation.map((r) => ({
          id: r.id,
          lat: r.latitude,
          lng: r.longitude,
          name: r.restaurantName,
          title: r.title,
        })),
      )}); } catch(e) {}
      true;
    `);
  }, [mapReady, JSON.stringify(roomsWithLocation)]);

  function handleMessage(event: any) {
    try {
      const msg = JSON.parse(event.nativeEvent.data) as MapDebugMessage;
      if (msg.type === "ready") {
        setMapReady(true);
        setMapError(null);
        setDebugInfo(
          `${msg.event ?? "ready"} @ ${msg.origin ?? "unknown-origin"}`,
        );
      } else if (msg.type === "roomSelected" && msg.roomId) {
        navigation.navigate("RoomDetail", { roomId: msg.roomId });
      } else if (msg.type === "debug") {
        const info = `${msg.event ?? "debug"} @ ${msg.origin ?? "unknown-origin"} kakao=${String(msg.hasKakao)}`;
        setDebugInfo(info);
        console.warn("Kakao Maps debug:", msg);
      } else if (msg.type === "sdkLoadError") {
        const info = `Kakao SDK script load failed @ ${msg.origin ?? "unknown-origin"}`;
        setMapError(info);
        setDebugInfo(info);
        console.warn("Kakao Maps SDK load error:", msg);
      } else if (msg.type === "error") {
        const info = `${msg.event ?? "Kakao Maps JS error"}: ${msg.msg ?? "unknown error"}`;
        setMapError(info);
        setDebugInfo(
          `${info} @ ${msg.origin ?? "unknown-origin"} kakao=${String(msg.hasKakao)}`,
        );
        console.warn("Kakao Maps JS error:", msg);
      }
    } catch (err) {
      console.warn(
        "Invalid Kakao Maps WebView message:",
        event.nativeEvent.data,
        err,
      );
    }
  }

  return (
    <View style={styles.container}>
      <WebView
        ref={webRef}
        source={{ html: htmlRef.current, baseUrl: "http://localhost:3000" }}
        style={styles.map}
        onMessage={handleMessage}
        javaScriptEnabled
        domStorageEnabled
        originWhitelist={["*"]}
        mixedContentMode="always"
        setSupportMultipleWindows={false}
        onError={(event) => {
          const description =
            event.nativeEvent.description ?? "WebView load failed";
          setMapError(description);
          console.warn("Kakao Maps WebView error:", event.nativeEvent);
        }}
        onHttpError={(event) => {
          const description = `WebView HTTP ${event.nativeEvent.statusCode}`;
          setMapError(description);
          console.warn("Kakao Maps WebView HTTP error:", event.nativeEvent);
        }}
      />
      {(mapError || debugInfo) && (
        <View
          style={[styles.debugPanel, mapError ? styles.errorPanel : undefined]}
        >
          <Text style={styles.debugText}>{mapError ?? debugInfo}</Text>
        </View>
      )}
      <View style={styles.badge}>
        <Text style={styles.badgeText}>방 {roomsWithLocation.length}개</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  map: { flex: 1 },
  badge: {
    position: "absolute",
    top: 12,
    right: 12,
    backgroundColor: "rgba(0,0,0,0.6)",
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  badgeText: { color: "#fff", fontSize: 13, fontWeight: "600" },
  debugPanel: {
    position: "absolute",
    left: 12,
    right: 12,
    bottom: 16,
    backgroundColor: "rgba(0,0,0,0.65)",
    borderRadius: 8,
    padding: 10,
  },
  errorPanel: { backgroundColor: "rgba(190,40,30,0.85)" },
  debugText: { color: "#fff", fontSize: 12, lineHeight: 16 },
});
