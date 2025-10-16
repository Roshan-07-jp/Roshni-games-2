// ... existing code ...

// Testing Configuration
allprojects {
    configurations.all {
        resolutionStrategy {
            force(
                "junit:junit:4.13.2",
                "org.jetbrains.kotlin:kotlin-test:1.9.22",
                "org.jetbrains.kotlin:kotlin-test-junit:1.9.22"
            )
        }
    }
}

// ... existing code ...