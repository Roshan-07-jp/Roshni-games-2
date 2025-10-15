#!/bin/bash
# jules-android-setup.sh - Android Environment for Jules Pro

echo "🔧 Setting up Android development environment for Roshni Games..."

# Set Android environment variables
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0

# Set Java environment
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin

# Create Android SDK directory
mkdir -p $ANDROID_HOME

# Download Android command line tools
cd $ANDROID_HOME
wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip -q commandlinetools-linux-9477386_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
rm -rf commandlinetools-linux-9477386_latest.zip

# Install required Android SDK components
yes | sdkmanager --licenses
sdkmanager "platform-tools"
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "extras;android;m2repository"
sdkmanager "extras;google;m2repository"

# Install Java 17 if not available
sudo apt-get update -qq
sudo apt-get install -y openjdk-17-jdk

# Verify installations
echo "📱 Verifying Android SDK setup..."
java -version
echo "Android SDK location: $ANDROID_HOME"
ls -la $ANDROID_HOME/

# Make gradlew executable
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    echo "✅ Made gradlew executable"
fi

# Install project dependencies
if [ -f "./build.gradle.kts" ] || [ -f "./build.gradle" ]; then
    echo "📦 Installing project dependencies..."
    ./gradlew dependencies --no-daemon
    echo "✅ Dependencies installed successfully"
fi

# Verify Kotlin compilation
if [ -f "./gradlew" ]; then
    echo "🔍 Testing Kotlin compilation..."
    ./gradlew compileDebugKotlin --no-daemon
    echo "✅ Kotlin compilation verified"
fi

echo "🎉 Android environment setup complete for Roshni Games!"
echo "Ready for Kotlin + Jetpack Compose + Material 3 development"
