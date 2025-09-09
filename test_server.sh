#!/bin/bash

echo "🔍 Testing P2P Signaling Server connectivity..."

# Test if server is running
echo "📡 Testing UDP port 45001..."
timeout 3 nc -u 192.168.1.38 45001 << EOF
TEST
EOF

echo "✅ Test completed"
