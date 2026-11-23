$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Test-Path "$env:JAVA_HOME/bin/javac.exe")) {
        $candidate = Get-ChildItem "$env:USERPROFILE/.jabba/jdk" -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '17' -and (Test-Path "$($_.FullName)/bin/javac.exe") } |
            Select-Object -First 1
        if ($candidate) { $env:JAVA_HOME = $candidate.FullName }
    }
    if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
        foreach ($sdk in "$env:LOCALAPPDATA/Android/Sdk", "$env:LOCALAPPDATA/GoldCcSdk") {
            if (Test-Path "$sdk/platforms/android-35/android.jar") { $env:ANDROID_HOME = $sdk; break }
        }
    }
    & .\gradlew.bat :app:assembleDebug @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
