package com.ssoftwares.doorunlock.models

data class SmartMeterData(
    val kWh: Double,
    val vR: Double,
    val vY: Double,
    val vB: Double,
    val iR: Double,
    val iY: Double,
    val iB: Double,
    val vRy: Double,
    val vYb: Double,
    val vBr: Double,
    val pfR: Double,
    val pfY: Double,
    val pfB: Double,
)


