$ErrorActionPreference = 'Stop'
if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    throw 'Gradle is required. Use Gradle 8.x or import the project into IntelliJ IDEA.'
}
gradle build --no-daemon
