package sg.edu.ntu.scse.mdp.g39.mdpkotlin.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Protocol
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothService(private val BTHandler: Handler) {

    private var connection: BluetoothConnection? = null
    private var stream: BluetoothStream? = null

    @Synchronized
    fun connectDevice(device: BluetoothDevice) {
        connection = BluetoothConnection(device)
        connection!!.start()
    }

    @Synchronized
    fun startStream(socket: BluetoothSocket) {
        stream = BluetoothStream(socket)
        stream!!.start()
    }

    @Synchronized
    fun cancel() {
        connection?.cancel()
        stream?.cancel()
    }

    @Synchronized
    fun write(data: String) {
        Log.d(TAG, "Sends : $data")
        stream?.write(data.toByteArray())
    }

    /**
     * Class used for establishing connection between 2 devices
     * Device will act as the Bluetooth Client
     */
    private inner class BluetoothConnection(device: BluetoothDevice) : Thread() {
        private var socket: BluetoothSocket? = null
        init {
            socket = try {
                device.createInsecureRfcommSocketToServiceRecord(device_uuid)
            } catch (e: IOException) {
                Log.d(TAG, "Failed to create socket")
                BTHandler.obtainMessage(Protocol.CONNECTION_ERROR)
                null
            }
        }

        override fun run() {
            socket?.let {
                try {
                    it.connect()
                } catch (connectEx: IOException) {
                    Log.d(TAG, "Failed to establish connection: ${connectEx.toString()}")
                    try {
                        it.close()
                    } catch (closeEx: IOException) {
                        Log.d(TAG, "Failed to close socket")
                        BTHandler.obtainMessage(Protocol.CONNECTION_ERROR)
                    }
                    return
                }
                startStream(it)
            }
        }

        fun cancel() { try { this.socket?.close() } catch (closeEz: IOException) { Log.d(TAG, "Failed to close socket") } }
    }

    /**
     * Class used to handle bi-directional data transfer
     * Used regardless whether its a Bluetooth Client or Server
     */
    private inner class BluetoothStream(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private var bufferStream: ByteArray = ByteArray(1024)

        override fun run() {
            bufferStream = ByteArray(1024)
            var byteSize: Int
            while (true) {
                try {
                    byteSize = this.inputStream.read(bufferStream)
                    val message = BTHandler.obtainMessage(Protocol.MESSAGE_RECEIVE, byteSize, -1, bufferStream)
                    message.sendToTarget()
                } catch (readEx: IOException) {
                    Log.d(TAG, "Failed to read from stream")
                    BTHandler.obtainMessage(Protocol.CONNECTION_ERROR)
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                Log.d(TAG, "Writing here")
                this.outputStream.write(bytes)
                this.outputStream.flush()
            } catch (writeEx: IOException) {
                Log.d(TAG, "Failed to write to receiver")
                val reportError = BTHandler.obtainMessage(Protocol.MESSAGE_ERROR)
                val bundle = Bundle().apply { putString("toast", "Failed to send data to other device") }
                reportError.data = bundle
                BTHandler.sendMessage(reportError)
            }
        }

        fun cancel() { try { this.socket.close() } catch (closeEx: IOException) { Log.d(TAG, "Failed to close stream") } }
    }

    companion object {
        private const val TAG = "BluetoothService"
        private val device_uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee")
        private const val device_name = "MDPGroup39Android"
    }
}