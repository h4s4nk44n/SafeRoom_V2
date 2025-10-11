#!/bin/bash
# SafeRoom Server Process Killer
# Bu script tüm SafeRoom server işlemlerini öldürür ve portları serbest bırakır

echo "🔍 Searching for SafeRoom server processes..."

# SafeRoomServer ile ilgili tüm Java işlemlerini bul
PIDS=$(ps aux | grep '[j]ava.*SafeRoomServer' | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "✅ No SafeRoom server processes found"
else
    echo "🎯 Found SafeRoom server processes:"
    ps aux | grep '[j]ava.*SafeRoomServer'
    
    echo ""
    echo "🔪 Killing processes..."
    for PID in $PIDS; do
        echo "  Killing PID: $PID"
        kill -9 $PID 2>/dev/null || echo "  ⚠️  Process $PID already terminated"
    done
    
    # Biraz bekle ki işlemler tamamen kapansın
    sleep 1
fi

# Port 443'ü kullanan işlemleri öldür (gRPC)
echo ""
echo "🔍 Checking port 443 (gRPC)..."
PORT_443_PID=$(sudo lsof -t -i:443)
if [ ! -z "$PORT_443_PID" ]; then
    echo "  ⚠️  Port 443 is still in use by PID: $PORT_443_PID"
    echo "  🔪 Killing process on port 443..."
    sudo kill -9 $PORT_443_PID 2>/dev/null
    sleep 1
else
    echo "  ✅ Port 443 is free"
fi

# Port 45000'i kullanan işlemleri öldür (P2P Signaling)
echo ""
echo "🔍 Checking port 45000 (P2P Signaling)..."
PORT_45000_PID=$(sudo lsof -t -i:45000)
if [ ! -z "$PORT_45000_PID" ]; then
    echo "  ⚠️  Port 45000 is still in use by PID: $PORT_45000_PID"
    echo "  🔪 Killing process on port 45000..."
    sudo kill -9 $PORT_45000_PID 2>/dev/null
    sleep 1
else
    echo "  ✅ Port 45000 is free"
fi

# Final kontrol
echo ""
echo "📊 Final port status:"
echo "  Port 443 (gRPC):"
sudo lsof -i:443 || echo "    ✅ Port 443 is free"
echo "  Port 45000 (P2P Signaling):"
sudo lsof -i:45000 || echo "    ✅ Port 45000 is free"

echo ""
echo "✅ Cleanup completed - Safe to restart server"
