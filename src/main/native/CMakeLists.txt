cmake_minimum_required(VERSION 3.10)
project(safe_room_native_encoder)

# ═══════════════════════════════════════════════════════════════════════════════
# FIX: Skip find_package(JNI) to avoid CMake 4.2 Windows path parsing bug
# CMake 4.2's FindJNI.cmake has issues with Windows paths containing backslashes
# (e.g., \P in "Program Files" is interpreted as escape sequence)
# Instead, we use the JNI include paths passed from build.gradle
# ═══════════════════════════════════════════════════════════════════════════════
if(DEFINED JAVA_INCLUDE_PATH AND DEFINED JAVA_INCLUDE_PATH2)
    message(STATUS "Using manually specified JNI paths (bypassing FindJNI)")
    message(STATUS "  JAVA_INCLUDE_PATH:  ${JAVA_INCLUDE_PATH}")
    message(STATUS "  JAVA_INCLUDE_PATH2: ${JAVA_INCLUDE_PATH2}")
    set(JNI_INCLUDE_DIRS "${JAVA_INCLUDE_PATH}" "${JAVA_INCLUDE_PATH2}")
else()
    # Fallback to find_package for systems where it works (Linux, older CMake)
    find_package(JNI REQUIRED)
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${JNI_INCLUDE_DIRS})

# Source files
set(SOURCES
    NativeVideoEncoder.cpp
)

# Create shared library
add_library(native_encoder SHARED ${SOURCES})

# Platform-specific settings
if (WIN32)
    # Windows specific (DLL)
    target_compile_definitions(native_encoder PRIVATE COMPILING_DLL)
elseif (APPLE)
    # macOS specific (dylib)
    target_link_libraries(native_encoder ${JNI_LIBRARIES})
elseif (UNIX)
    # Linux specific (so)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
endif()

message(STATUS "JNI Include Dirs: ${JNI_INCLUDE_DIRS}")
message(STATUS "Build target: native_encoder (Shared Library)")
