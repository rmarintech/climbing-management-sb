K6 COMMANDS — CLIMBING MANAGEMENT
======================================
winget install k6.k6
choco install k6

k6 run performance/baseline.js


jcmd 10736 JFR.start name=performance settings=profile duration=45s filename=performance.jfrjcmd 10736 JFR.start name=performance settings=profile duration=45s filename=performance.jfr