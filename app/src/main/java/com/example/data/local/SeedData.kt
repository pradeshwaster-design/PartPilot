package com.example.data.local

import com.example.data.model.*

object SeedData {
    val locations = listOf(
        LocationEntity(
            id = 1,
            shelf = "A",
            level = "1",
            box = "B1",
            code = "A-1-B1",
            notes = "Microcontrollers, dev boards & passives"
        ),
        LocationEntity(
            id = 2,
            shelf = "A",
            level = "2",
            box = "B3",
            code = "A-2-B3",
            notes = "Sensors, displays & communication modules"
        ),
        LocationEntity(
            id = 3,
            shelf = "B",
            level = "1",
            box = "B2",
            code = "B-1-B2",
            notes = "Power, motor drivers, servos & batteries"
        )
    )

    val parts = listOf(
        PartEntity(
            id = 1,
            name = "ESP32 DevKit v1",
            group = PartGroup.MICROCONTROLLER,
            specs = "Dual-Core 240MHz, WiFi+BLE, 30 pins, CP2102",
            quantity = 3,
            minStock = 1,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 3,
            remarks = "Flashed and verified WiFi and GPIOs",
            datasheetUrl = "https://www.espressif.com/en/products/socs/esp32"
        ),
        PartEntity(
            id = 2,
            name = "Arduino Nano V3",
            group = PartGroup.MICROCONTROLLER,
            specs = "ATmega328P 16MHz, 5V, CH340 USB, 14 digital / 8 analog",
            quantity = 5,
            minStock = 2,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 5,
            remarks = "Bootloader OK, breadboard friendly"
        ),
        PartEntity(
            id = 3,
            name = "Raspberry Pi Pico",
            group = PartGroup.MICROCONTROLLER,
            specs = "RP2040 Dual ARM Cortex-M0+, 133MHz, 2MB Flash, 26 GPIO",
            quantity = 2,
            minStock = 1,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 10,
            remarks = "Pre-soldered header pins"
        ),
        PartEntity(
            id = 4,
            name = "HC-SR04 Ultrasonic Sensor",
            group = PartGroup.SENSOR,
            specs = "5V DC, 2cm - 400cm range, 15° measuring angle",
            quantity = 4,
            minStock = 2,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 2,
            remarks = "Tested echo pulse width at 5V logic"
        ),
        PartEntity(
            id = 5,
            name = "MPU6050 6-DoF Gyro/Accel",
            group = PartGroup.SENSOR,
            specs = "3-axis Gyroscope + 3-axis Accelerometer, I2C 3.3-5V, DMP",
            quantity = 2,
            minStock = 1,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 4,
            remarks = "Default I2C address 0x68. Needs calibration offset on startup."
        ),
        PartEntity(
            id = 6,
            name = "DHT22 Temperature & Humidity",
            group = PartGroup.SENSOR,
            specs = "AM2302, 3.3-6V, -40 to 80°C (±0.5°C), 0-100% RH (±2%)",
            quantity = 3,
            minStock = 1,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 6,
            remarks = "Requires 10kΩ pullup resistor on data line"
        ),
        PartEntity(
            id = 7,
            name = "TCRT5000 5-Way IR Tracker",
            group = PartGroup.SENSOR,
            specs = "5-channel analog/digital IR reflectance array, 5V",
            quantity = 2,
            minStock = 1,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 1,
            remarks = "Ideal for black line contrast detection"
        ),
        PartEntity(
            id = 8,
            name = "L298N Dual H-Bridge Driver",
            group = PartGroup.MOTOR_DRIVER,
            specs = "Dual channel, 2A peak per bridge, 5V-35V motor voltage, onboard 5V regulator",
            quantity = 2,
            minStock = 1,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 2,
            remarks = "Large aluminum heatsink mounted. 5V jumper enabled."
        ),
        PartEntity(
            id = 9,
            name = "SG90 Micro Servo 9g",
            group = PartGroup.MECHANICAL,
            specs = "4.8V-6V, 1.8kg-cm torque, 180° rotation, nylon gears",
            quantity = 6,
            minStock = 2,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 8,
            remarks = "Includes plastic horns and mounting screws"
        ),
        PartEntity(
            id = 10,
            name = "TT Gear Motor 1:48 Dual Shaft",
            group = PartGroup.MECHANICAL,
            specs = "3V-6V DC, Dual shaft, 200 RPM @ 6V, 0.8kg-cm torque",
            quantity = 4,
            minStock = 2,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 3,
            remarks = "0.1uF noise suppression ceramic caps soldered across terminals"
        ),
        PartEntity(
            id = 11,
            name = "18650 Li-ion Cell 2600mAh",
            group = PartGroup.POWER_BATTERY,
            specs = "3.7V nominal, 4.2V max charge, button top, 10A discharge",
            quantity = 4,
            minStock = 2,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 12,
            remarks = "Cycle count: 18. Voltage balanced at 4.12V."
        ),
        PartEntity(
            id = 12,
            name = "2S 18650 Battery Holder with Switch",
            group = PartGroup.POWER_BATTERY,
            specs = "7.4V series output, JST-XH balance connector, slide switch",
            quantity = 3,
            minStock = 1,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 15,
            remarks = "Heavy-duty wire leads"
        ),
        PartEntity(
            id = 13,
            name = "LM2596 DC-DC Buck Converter",
            group = PartGroup.POWER_BATTERY,
            specs = "4V-40V in, 1.23V-37V out, 3A max, multi-turn trimpot",
            quantity = 3,
            minStock = 1,
            locationId = 3,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 5,
            remarks = "Pre-tuned to steady 5.05V output"
        ),
        PartEntity(
            id = 14,
            name = "0.96 inch I2C OLED Display",
            group = PartGroup.DISPLAY,
            specs = "128x64 pixels, SSD1306, Blue/Yellow dual color, 3.3-5V VCC",
            quantity = 2,
            minStock = 1,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 7,
            remarks = "Address 0x3C verified on I2C bus scan"
        ),
        PartEntity(
            id = 15,
            name = "NRF24L01+ 2.4GHz Transceiver",
            group = PartGroup.COMM_MODULE,
            specs = "2.4GHz ISM band, 250kbps-2Mbps, 3.3V VCC ONLY (5V tolerant SPI)",
            quantity = 3,
            minStock = 2,
            locationId = 2,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 9,
            remarks = "WARNING: 3.3V VCC only! Add 10uF cap across VCC/GND for stability."
        ),
        PartEntity(
            id = 16,
            name = "220 Ohm 1/4W Resistors (Pack)",
            group = PartGroup.PASSIVE,
            specs = "Through-hole metal film, 5% tolerance, 1/4W",
            quantity = 80,
            minStock = 20,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 30,
            remarks = "Standard LED current limiting"
        ),
        PartEntity(
            id = 17,
            name = "10k Ohm Linear Potentiometer",
            group = PartGroup.PASSIVE,
            specs = "Breadboard friendly 3-pin pitch, single turn linear B10K",
            quantity = 5,
            minStock = 2,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 20,
            remarks = "Includes plastic knob"
        ),
        PartEntity(
            id = 18,
            name = "2N2222 NPN BJT Transistors",
            group = PartGroup.SEMICONDUCTOR,
            specs = "TO-92, 40V Vce, 800mA Ic, high-speed switching",
            quantity = 15,
            minStock = 5,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 25,
            remarks = "General purpose amplifier and switch"
        ),
        PartEntity(
            id = 19,
            name = "40-pin M-to-F Jumper Wires",
            group = PartGroup.CONNECTOR_WIRE,
            specs = "20cm length, 2.54mm pitch, multi-colored ribbon",
            quantity = 40,
            minStock = 10,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 14,
            remarks = "Flexible copper wire for prototyping"
        ),
        PartEntity(
            id = 20,
            name = "60W Adjustable Soldering Iron",
            group = PartGroup.TOOL_CONSUMABLE,
            specs = "200°C - 450°C dial, 110V/220V, with 5 interchangeable tips",
            quantity = 1,
            minStock = 1,
            locationId = 1,
            condition = PartCondition.WORKING,
            lastTestedAt = System.currentTimeMillis() - 86400000L * 2,
            remarks = "Clean chisel tip currently fitted, heats in 45s"
        )
    )

    val projects = listOf(
        ProjectEntity(
            id = 1,
            name = "Line Follower Robot v2",
            description = "High-speed line tracker with PID control and dual TT motors for college robotics competition",
            status = ProjectStatus.ACTIVE,
            budgetCap = 45.0,
            notes = "Using TCRT5000 array on analog inputs A0-A4. Need to tune KD and KP constants.",
            createdAt = System.currentTimeMillis() - 86400000L * 7
        ),
        ProjectEntity(
            id = 2,
            name = "IoT Smart Weather Station",
            description = "Solar-assisted WiFi weather telemetry station logging temperature, humidity & barometric stats",
            status = ProjectStatus.ACTIVE,
            budgetCap = 35.0,
            notes = "Deep sleep cycle every 10 minutes. Transmits to home MQTT broker.",
            createdAt = System.currentTimeMillis() - 86400000L * 4
        )
    )

    val projectParts = listOf(
        // Line Follower Robot v2
        ProjectPartEntity(
            id = 1,
            projectId = 1,
            partId = 2, // Arduino Nano V3
            name = "Arduino Nano V3",
            qtyNeeded = 1,
            estUnitPrice = 4.5,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.TESTED
        ),
        ProjectPartEntity(
            id = 2,
            projectId = 1,
            partId = 8, // L298N Dual H-Bridge Driver
            name = "L298N Dual H-Bridge Driver",
            qtyNeeded = 1,
            estUnitPrice = 3.8,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.WIRED
        ),
        ProjectPartEntity(
            id = 3,
            projectId = 1,
            partId = 7, // TCRT5000 5-Way IR Tracker
            name = "TCRT5000 5-Way IR Tracker",
            qtyNeeded = 1,
            estUnitPrice = 5.2,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.TESTED
        ),
        ProjectPartEntity(
            id = 4,
            projectId = 1,
            partId = 10, // TT Gear Motor 1:48
            name = "TT Gear Motor 1:48 Dual Shaft",
            qtyNeeded = 2,
            estUnitPrice = 4.0,
            decision = PartDecision.KEEP,
            reservedQty = 2,
            checklistState = ChecklistState.WIRED
        ),
        ProjectPartEntity(
            id = 5,
            projectId = 1,
            partId = 12, // 2S 18650 Battery Holder
            name = "2S 18650 Battery Holder with Switch",
            qtyNeeded = 1,
            estUnitPrice = 2.5,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.PARTS_READY
        ),
        ProjectPartEntity(
            id = 6,
            projectId = 1,
            partId = 11, // 18650 Li-ion Cell
            name = "18650 Li-ion Cell 2600mAh",
            qtyNeeded = 2,
            estUnitPrice = 8.0,
            decision = PartDecision.KEEP,
            reservedQty = 2,
            checklistState = ChecklistState.PARTS_READY
        ),
        ProjectPartEntity(
            id = 7,
            projectId = 1,
            partId = null, // unowned
            name = "2WD Smart Robot Chassis with Wheels",
            qtyNeeded = 1,
            estUnitPrice = 9.5,
            decision = PartDecision.KEEP,
            reservedQty = 0,
            checklistState = ChecklistState.PARTS_READY
        ),

        // IoT Smart Weather Station
        ProjectPartEntity(
            id = 8,
            projectId = 2,
            partId = 1, // ESP32 DevKit v1
            name = "ESP32 DevKit v1",
            qtyNeeded = 1,
            estUnitPrice = 6.0,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.CODED
        ),
        ProjectPartEntity(
            id = 9,
            projectId = 2,
            partId = 6, // DHT22 Temperature & Humidity
            name = "DHT22 Temperature & Humidity",
            qtyNeeded = 1,
            estUnitPrice = 4.5,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.TESTED
        ),
        ProjectPartEntity(
            id = 10,
            projectId = 2,
            partId = 14, // 0.96 inch I2C OLED Display
            name = "0.96 inch I2C OLED Display",
            qtyNeeded = 1,
            estUnitPrice = 3.5,
            decision = PartDecision.KEEP,
            reservedQty = 1,
            checklistState = ChecklistState.TESTED
        ),
        ProjectPartEntity(
            id = 11,
            projectId = 2,
            partId = null, // unowned
            name = "5V 1W Solar Panel & TP4056 Charger",
            qtyNeeded = 1,
            estUnitPrice = 7.0,
            decision = PartDecision.KEEP,
            reservedQty = 0,
            checklistState = ChecklistState.PARTS_READY
        )
    )

    val partEvents = listOf(
        PartEventEntity(
            id = 1,
            partId = 1,
            type = EventType.ADDED,
            note = "Initial stock purchased from electronics maker store",
            qtyDelta = 3,
            createdAt = System.currentTimeMillis() - 86400000L * 14
        ),
        PartEventEntity(
            id = 2,
            partId = 1,
            type = EventType.USED,
            note = "Reserved 1 unit for IoT Smart Weather Station",
            qtyDelta = 0,
            projectId = 2,
            createdAt = System.currentTimeMillis() - 86400000L * 4
        ),
        PartEventEntity(
            id = 3,
            partId = 2,
            type = EventType.ADDED,
            note = "5x Nano pack added to Box A-1-B1",
            qtyDelta = 5,
            createdAt = System.currentTimeMillis() - 86400000L * 10
        ),
        PartEventEntity(
            id = 4,
            partId = 2,
            type = EventType.USED,
            note = "Allocated to Line Follower Robot v2",
            qtyDelta = 0,
            projectId = 1,
            createdAt = System.currentTimeMillis() - 86400000L * 7
        ),
        PartEventEntity(
            id = 5,
            partId = 5,
            type = EventType.TESTED,
            note = "Tested gyro drift with Arduino I2C scanner, zero errors",
            qtyDelta = 0,
            createdAt = System.currentTimeMillis() - 86400000L * 4
        )
    )

    val cartItems = listOf(
        CartItemEntity(
            id = 1,
            projectId = 1,
            name = "2WD Smart Robot Chassis with Wheels",
            qty = 1,
            unitPrice = 9.5,
            shop = "Robotics Hobby Store",
            status = CartItemStatus.IN_CART
        ),
        CartItemEntity(
            id = 2,
            projectId = 2,
            name = "5V 1W Solar Panel & TP4056 Charger",
            qty = 1,
            unitPrice = 7.0,
            shop = "Electronics Bazaar",
            status = CartItemStatus.IN_CART
        )
    )
}
