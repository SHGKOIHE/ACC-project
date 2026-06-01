import React, { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { WebView } from 'react-native-webview';
import * as Location from 'expo-location';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/AppNavigator';

const KAKAO_JS_KEY = process.env.EXPO_PUBLIC_KAKAO_JS_KEY ?? '9841405e090263146cdb4a323bd57f92';

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
    window.onerror = function(msg, src, line) {
      if(window.ReactNativeWebView) {
        window.ReactNativeWebView.postMessage(JSON.stringify({type:'error', msg:msg, src:src, line:line}));
      }
    };
    function escHtml(s) {
      return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
    var _map = null;
    var _markers = [];
    function initMap() {
      kakao.maps.load(function() {
        var container = document.getElementById('map');
        var options = { center: new kakao.maps.LatLng(${lat}, ${lng}), level: 5 };
        _map = new kakao.maps.Map(container, options);
        new kakao.maps.Marker({
          position: new kakao.maps.LatLng(${lat}, ${lng}),
          map: _map
        });
        window.ReactNativeWebView.postMessage(JSON.stringify({type:'ready'}));
      });
    }
    function updateMarkers(rooms) {
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
    onload="initMap()">
  </script>
</body>
</html>`;
}

export function MapScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const [coords, setCoords] = useState({ lat: 37.5665, lng: 126.978 });
  const [mapReady, setMapReady] = useState(false);
  const webRef = useRef<WebView>(null);
  const htmlRef = useRef(buildInitialHtml(37.5665, 126.978));

  const { data } = useQuery({
    queryKey: ['rooms'],
    queryFn: () => apiClient.get('/api/rooms'),
    refetchInterval: 30000,
  });
  const rooms: any[] = (data as any)?.data ?? [];
  const roomsWithLocation = rooms.filter((r) => r.latitude && r.longitude);

  useEffect(() => {
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      const loc = await Location.getCurrentPositionAsync({});
      setCoords({ lat: loc.coords.latitude, lng: loc.coords.longitude });
    })();
  }, []);

  // Push marker updates via postMessage instead of reloading the WebView
  useEffect(() => {
    if (!mapReady || !webRef.current) return;
    const payload = JSON.stringify({
      type: 'updateMarkers',
      rooms: roomsWithLocation.map((r) => ({
        id: r.id,
        lat: r.latitude,
        lng: r.longitude,
        name: r.restaurantName,
        title: r.title,
      })),
    });
    webRef.current.injectJavaScript(`
      try { updateMarkers(${JSON.stringify(roomsWithLocation.map((r) => ({
        id: r.id,
        lat: r.latitude,
        lng: r.longitude,
        name: r.restaurantName,
        title: r.title,
      })))}); } catch(e) {}
      true;
    `);
  }, [mapReady, JSON.stringify(roomsWithLocation)]);

  function handleMessage(event: any) {
    try {
      const msg = JSON.parse(event.nativeEvent.data);
      if (msg.type === 'ready') {
        setMapReady(true);
      } else if (msg.type === 'roomSelected') {
        navigation.navigate('RoomDetail', { roomId: msg.roomId });
      } else if (msg.type === 'error') {
        console.warn('Kakao Maps JS error:', msg.msg, msg.src, msg.line);
      }
    } catch {}
  }

  return (
    <View style={styles.container}>
      <WebView
        ref={webRef}
        source={{ html: htmlRef.current, baseUrl: 'http://localhost:3000' }}
        style={styles.map}
        onMessage={handleMessage}
        javaScriptEnabled
        domStorageEnabled
        originWhitelist={['*']}
        mixedContentMode="always"
        setSupportMultipleWindows={false}
      />
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
    position: 'absolute',
    top: 12,
    right: 12,
    backgroundColor: 'rgba(0,0,0,0.6)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  badgeText: { color: '#fff', fontSize: 13, fontWeight: '600' },
});
