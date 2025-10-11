#!/bin/bash
# SafeRoom Server Process Killer
# Bu script tüm Java işlemlerini değil, sadece SafeRoom server işlemlerini öldürür

echo "🔍 Searching for SafeRoom server processes..."

# SafeRoomServer ile ilgili tüm Java işlemlerini bul
PIDS=$(ps aux | grep '[j]ava.*SafeRoomServer' | awk '{print $2}')

if [ -z "$PIDS" ]; then
    echo "✅ No SafeRoom server processes found"
    exit 0
fi

echo "🎯 Found SafeRoom server processes:"
ps aux | grep '[j]ava.*SafeRoomServer'

echo ""
echo "🔪 Killing processes..."
for PID in $PIDS; do
    echo "  Killing PID: $PID"
    kill -9 $PID 2>/dev/null || echo "  ⚠️  Process $PID already terminated"
done

echo ""
echo "✅ All SafeRoom server processes terminated"

# Port kontrolü
echo ""
echo "📊 Port status:"
echo "  Port 443 (gRPC):"
sudo netstat -tulpn | grep ':443 ' || echo "    ✅ Port 443 is free"
echo "  Port 45000 (P2P Signaling):"
sudo netstat -tulpn | grep ':45000 ' || echo "    ✅ Port 45000 is free"
