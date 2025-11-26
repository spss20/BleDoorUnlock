package com.ssoftwares.doorunlock.views

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParseException
import com.ssoftwares.doorunlock.R
import com.ssoftwares.doorunlock.databinding.ActivitySmartMeterBinding
import com.ssoftwares.doorunlock.models.SmartMeterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random

class SmartMeterActivity : AppCompatActivity() {

    private var kwh = 816074050
    private var frequency = 49.01

    private lateinit var binding: ActivitySmartMeterBinding;

    private val TAG = "BluetoothClient"
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private val MESSAGE_TO_SEND = "Hi"

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private lateinit var handler: Handler
    private var deviceMac: String? = null
    private var packetBuilder: ByteArrayOutputStream = ByteArrayOutputStream()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySmartMeterBinding.inflate(layoutInflater)
        setContentView(binding.main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        binding.logCat.movementMethod = ScrollingMovementMethod()


        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        handler = Handler(Looper.getMainLooper())

        deviceMac = intent.getStringExtra("device")
        val uuid = intent.getStringExtra("uuid")
        Toast.makeText(this@SmartMeterActivity, "UUID: $uuid", Toast.LENGTH_LONG)
            .show()
        if (deviceMac == null) {
            Toast.makeText(this@SmartMeterActivity, "ERROR: MAC ID is null", Toast.LENGTH_LONG)
                .show()
        } else {
            binding.deviceMac.text = "Attempting to Connect to $deviceMac"
            connectToDevice()
            lifecycleScope.launch {
                while (true) {
                    binding.frequency.text = formatNumber(frequency);
                    frequency += 0.01
                    delay(10 * 60 * 1000)
                }
            }
        }

        binding.exit.setOnClickListener { finish() }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        Thread {
            try {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceMac)
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID)

                bluetoothAdapter.cancelDiscovery()
                bluetoothSocket.connect()

                runOnUiThread {
                    binding.deviceMac.text = "Connected to Device: $deviceMac"
                }

                outputStream = bluetoothSocket.outputStream
                inputStream = bluetoothSocket.inputStream
                startSendingData()
                startReceivingData()
            } catch (e: IOException) {

                runOnUiThread {
                    binding.deviceMac.text = "Connection Failed: $deviceMac"
                }
                Log.e(TAG, "Error Surya: ${e.message}")
                e.printStackTrace()
            }
        }.start()

    }


    private fun startSendingData() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    outputStream?.write(MESSAGE_TO_SEND.toByteArray())
                    outputStream?.flush()
//                    runOnUiThread { binding.logCat.append("Sent: $MESSAGE_TO_SEND\n") }
                    Log.d(TAG, "Sent message: $MESSAGE_TO_SEND")
                } catch (e: IOException) {
                    Log.e(TAG, "Error sending message: ${e.message}")
                }

                // Repeat every 10 seconds
                handler.postDelayed(this, 5000)
            }
        }, 2000) // Start after 10 seconds
    }

    private fun startReceivingData() {
        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    val data = buffer.copyOfRange(0, bytes);
//                    runOnUiThread { binding.logCat.append("Received: ${String(data)}\n") }

                    if (data[0] == 0x7B.toByte()) {
                        if (data[data.size - 1] == 0x7D.toByte()) {
                            //STX and ETX Received
                            parseData(data)
                            continue
                        }

                        //Only STX
                        packetBuilder.reset()
                        packetBuilder.write(data)
                    } else if (data[data.size - 1] == 0x7D.toByte()) {
                        //ETX Received
                        packetBuilder.write(data)
                        parseData(packetBuilder.toByteArray())
//                        packetBuilder.reset()
                    } else {
                        packetBuilder.write(data)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Connection lost: ${e.message}")
                    runOnUiThread {
                        binding.deviceMac.text = "Disconnected: $deviceMac"
                    }
                    break
                }
            }
        }.start()
    }

    private fun parseData(buffer: ByteArray) {
        val receivedMessage = String(buffer)
        Log.d(TAG, "Received message: $receivedMessage")
        Log.d(TAG, "Received length: ${receivedMessage.length}")

        try {
            val jsonObject = JSONObject(receivedMessage.trim())
            if (jsonObject.has("KWh")) {
                val smartMeterData = SmartMeterData(
                    kWh = jsonObject.getDouble("KWh"),
                    vR = jsonObject.getDouble("V_R"),
                    vY = jsonObject.getDouble("V_Y"),
                    vB = jsonObject.getDouble("V_B"),
                    iR = jsonObject.getDouble("I_R"),
                    iY = jsonObject.getDouble("I_Y"),
                    iB = jsonObject.getDouble("I_B"),
                    vRy = jsonObject.getDouble("V_RY"),
                    vYb = jsonObject.getDouble("V_YB"),
                    vBr = jsonObject.getDouble("V_BR"),
                    pfR = jsonObject.getDouble("PF_R"),
                    pfY = jsonObject.getDouble("PF_Y"),
                    pfB = jsonObject.getDouble("PF_B")
                )
                runOnUiThread {
                    updateUiData(data = smartMeterData)
                }
            }

        } catch (e: Exception) {
            runOnUiThread {
                binding.deviceMac.text = "Error in parsing $receivedMessage"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }
    }

    private fun generateRandomValues() {
        kwh = generateKwh(kwh)

        val smartMeterData = SmartMeterData(
            kwh.toDouble(),
            fluctuate(240.0, 5.0),
            fluctuate(240.0, 5.0),
            fluctuate(240.0, 5.0),
            fluctuate(24.0, 2.0),
            fluctuate(24.0, 2.0),
            fluctuate(24.0, 2.0),
            fluctuate(400.0, 10.0),
            fluctuate(400.0, 10.0),
            fluctuate(400.0, 10.0),
            fluctuate(1.0, 0.1),
            fluctuate(1.0, 0.1),
            fluctuate(1.0, 0.1)
        )

        updateUiData(smartMeterData)
    }

    private fun updateUiData(data: SmartMeterData) {
        binding.kwh.text = formatNumber(data.kWh)

        binding.voltageR.text = formatNumber(data.vR)
        binding.voltageY.text = formatNumber(data.vY)
        binding.voltageB.text = formatNumber(data.vB)

        binding.ry.text = formatNumber(data.vRy)
        binding.yb.text = formatNumber(data.vYb)
        binding.br.text = formatNumber(data.vBr)

        binding.currentR.text = formatNumber(data.iR)
        binding.currentY.text = formatNumber(data.iY)
        binding.currentB.text = formatNumber(data.iB)

        binding.powerR.text = formatNumber(data.pfR)
        binding.powerY.text = formatNumber(data.pfY)
        binding.powerB.text = formatNumber(data.pfB)
    }

    private fun generateKwh(previousKwh: Int): Int {
        val increase = Random.nextInt(50, 150)
        return previousKwh + increase
    }

    private fun fluctuate(baseValue: Double, fluctuationRange: Double): Double {
        // Fluctuate the value up or down within the specified range
        val fluctuation = Random.nextDouble(-fluctuationRange, fluctuationRange)
        return baseValue + fluctuation
    }

    fun rand(a: Int, b: Int): Int {
        return Random.nextInt(a, b + 1)
    }

    private fun formatNumber(value: Double): String {
        return String.format("%.2f", value)
    }
}