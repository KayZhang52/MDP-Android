package sg.edu.ntu.scse.mdp.g39.mdpkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import android.view.View.DragShadowBuilder
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.test_layout.label_robot_direction
import kotlinx.android.synthetic.main.test_layout.map_relative_container
import kotlinx.android.synthetic.main.test_layout.obstacle
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Device
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.MessageLog
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.ObstacleView
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Protocol
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Store
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.service.BluetoothService
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.util.Cmd
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.util.MapDrawer
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.util.Parser
import java.io.*
import java.lang.ref.WeakReference
import java.text.DecimalFormat


class MainActivity : AppCompatActivity() {

    // Activity Variables
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var connectionThread: BluetoothService? = null
    private lateinit var connectedDevice: BluetoothDevice
    private lateinit var listviewDevices: ListView
    private val messageLog = MessageLog()
    private var isServer = false
    private var disconnectState = true
    private var startModeState = false
    private var fastestPathModeState = false
    private var autoModeState = true
    private val deviceList = ArrayList<Device>()
    private lateinit var timer: CountDownTimer

    // Additional GUI Compon2ents
    private lateinit var inflater: LayoutInflater

    /**
     * Controls for Devices configs
     */
    private var buttonBluetoothServerListen: Button? = null
    private var buttonScan: Button? = null

    // Controls for String configs
    private var textboxString1: EditText? = null
    private var textboxString2: EditText? = null

    // Controls for Messaging Sending
    private var textboxSendMessage: EditText? = null
    private var scrollView: ScrollView? = null
    private var messageLogView: TextView? = null
    private var currentTime = System.currentTimeMillis()

    private lateinit var sensorOrientation: OrientationEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inflater = LayoutInflater.from(this)
        setContentView(R.layout.test_layout)

        // UI Configurations
        configureToggle()

        // Request for location (BT)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )

        // Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 1)
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            startActivityForResult(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                1
            )
        }
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //without permission, attempt to request it.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        val btConnectDisconnectBroadcastFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(btConnectDisconnectReceiver, btConnectDisconnectBroadcastFilter)

        // Set up Main Activity Event Listeners
        button_direction_left.setOnClickListener(onJoystickLeft)
        button_direction_right.setOnClickListener(onJoystickRight)
        button_direction_up.setOnClickListener(onJoystickUp)
        button_refresh_phase.setOnClickListener(onRefreshState)
        switch_motion_control.setOnCheckedChangeListener(onToggleMotionControl)
        button_set_origin.setOnClickListener(onSetOrigin)
        button_set_waypoint.setOnClickListener(onSetWaypoint)
        button_start_phase.setOnClickListener(onStart)
        toggle_mode_fastest_path.setOnCheckedChangeListener(onChangeFastestPathMode)
        toggle_update_auto.setOnCheckedChangeListener(onChangeAutoMode)
        canvas_gridmap.setOnTouchListener(onSetMap)
        button_reset_map.setOnClickListener(onResetMap)
        sensorOrientation = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                handleRotation(orientation)
            }
        }
        obstacle.setOnTouchListener(onDragged)
        canvas_gridmap.setOnDragListener(onObstacleDraggedOnMap)
        joystickView.setOnMoveListener(onMove)
    }

    private fun configureToggle() {
        toggle_mode_exploration.setPadding(15, 10, 15, 10)
        toggle_mode_fastest_path.setPadding(15, 10, 15, 10)

        toggle_update_auto.setPadding(15, 5, 15, 5)
        toggle_update_manual.setPadding(15, 5, 15, 5)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null && requestCode == INTENT_EXPORT && mdfFileToExport != null) {
            Log.i(TAG, "Writing to file")
            try {
                val bos = contentResolver.openOutputStream(data.data!!)!!
                val bis = mdfFileToExport!!.inputStream().buffered()
                bis.copyTo(bos)
                bos.flush()
                bos.close()
                bis.close()
                Log.i(TAG, "File written")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private val btConnectDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {


                Log.d("bluetooth", "broadcast received")
                val action = intent.action
                val device: BluetoothDevice?
                val getCurrentConnection: String?

                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        Log.d("bluetooth", "Found")
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        Log.d(
                            TAG,
                            if (device != null && device.name != null) device.name else "No device name"
                        )
                        addDevice(device, device.name, device.address)
                    }

                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        getCurrentConnection = label_bluetooth_status.text.toString()
                        if (connectionThread != null || !disconnectState && getCurrentConnection == "Not Connected") {
                            Log.d("bt", "Connected with a device")
                            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            connectedState(device)
                            disconnectState = false

                            disableElement(buttonBluetoothServerListen)
                            disableElement(buttonScan)
                            disableElement(listviewDevices)

                            if (isPairedDevicesOnly) {
                                clearBtDeviceList()
                                isPairedDevicesOnly = false
                            }
                        }
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected with a device")
                        getCurrentConnection = label_bluetooth_status.text.toString()
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                        if (getCurrentConnection == "Connected" && device.address == connectedDevice.address) {
                            connectionThread?.cancel()

                            if (!disconnectState) {
                                if (isServer) {
                                    Log.d(TAG, "Starting Server")
                                    connectionThread = BluetoothService(streamHandler)
                                    connectionThread?.startServer(bluetoothAdapter)
                                } else {
                                    Log.d(TAG, "Starting Client")
                                    connectionThread = BluetoothService(streamHandler)
                                    connectionThread?.connectDevice(connectedDevice)
                                }
                            } else connectionThread = null
                            disconnectedState()
                        }
                    }

                    else -> Log.d(TAG, "Default case for receiver")
                }
            } catch(e:Exception){
                Log.e("bluetoothBroadcast", e.toString())
            }
        }
    }

    // add a device to the device list, recreate the DeviceAdapter class using the updated device list. 
    // reset the listview's adapter to the new DeviceAdapter instance.
    private fun addDevice(
        device: BluetoothDevice,
        deviceName: String,
        deviceHardwareAddress: String
    ) {
        var flag = true
        run toBreak@{
            deviceList.forEach {
                if (it.macAddr == deviceHardwareAddress) {
                    flag = false
                    return@toBreak
                }
            }
        }

        if (flag) {
            deviceList.add(Device(device, deviceName, deviceHardwareAddress))
            listviewDevices.invalidate()

            val state = listviewDevices.onSaveInstanceState()
            val adapter = DeviceAdapter(applicationContext, deviceList)
            listviewDevices.adapter = adapter
            listviewDevices.onRestoreInstanceState(state)
        }
    }

    private fun clearBtDeviceList() {
        deviceList.clear()
        listviewDevices.invalidate()
        val adapter = DeviceAdapter(applicationContext, deviceList)
        listviewDevices.adapter = adapter
    }

    // Stream for data
    private val streamHandler = BtStreamHandler(this)

    class BtStreamHandler(activity: MainActivity) : Handler() {
        private val ref = WeakReference(activity)

        override fun handleMessage(message: Message) {
            val activity = ref.get()
            when (message.what) {
                Protocol.MESSAGE_RECEIVE -> {
                    val buffer = message.obj as ByteArray
                    val data = String(buffer, 0, message.arg1)
                    Log.d(TAG, "Received data : $data")
                    if (activity == null) {
                        Log.e(TAG, "No activity object, Not continuing..."); return
                    }
                    activity.messageLog.addMessage(
                        sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Message.MESSAGE_RECEIVER,
                        data.trim()
                    )
                    val textArr = data.split(";")
                    textArr.forEach {
                        if (it.isEmpty()) return@forEach
                        activity.handleAction(it.trim()) // Handle Action
                    }

                    activity.messageLogView?.text = activity.messageLog.getLog()
                }

                Protocol.CONNECTION_ERROR -> {
                    Log.d(TAG, "Connection error with a device")
                    activity?.connectionThread?.cancel()
                    activity?.connectBluetoothDevice()
                }

                Protocol.MESSAGE_ERROR -> {
                    Log.d(TAG, "Error sending message to device")
                    activity?.transmissionFail()
                }

                else -> Log.d(TAG, "Just a default case")
            }
        }

        companion object {
            private const val TAG = "StreamHandler"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btConnectDisconnectReceiver)
        connectionThread?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_menu_inspector -> {
                Log.d(TAG, "Clicked on Menu Inspector")
                dialogDataInspector()
            }

            R.id.app_menu_chat -> {
                Log.d(TAG, "Clicked on Message Log")
                dialogMessageLog()
            }

            R.id.app_menu_search_device -> {
                Log.d(TAG, "Clicked on Search Device")
                dialogDevices()
            }

            R.id.app_menu_disconnect_device -> {
                Log.d(TAG, "Clicked on Disconnect Device")
                disconnectBluetoothDevice()
            }

            R.id.app_menu_reconnect_device -> {
                Log.d("BT-Main", "Clicked on Reconnect Device")
                dialogPairedDevices()
            }

            R.id.app_menu_string_config -> {
                Log.d(TAG, "Clicked on String Configurations")
                dialogConfigString()
            }

            R.id.app_menu_export_mdf -> {
                Log.d(TAG, "Clicked on Export MDF")
                dialogFileManager()
            }

            else -> Log.d(TAG, "Clicked on default case")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun connectedState(device: BluetoothDevice) {
        connectedDevice = device
        label_bluetooth_status.text = "Connected"
        label_bluetooth_status.setTextColor(Color.parseColor("#388e3c"))

        label_bluetooth_connected_device.text =
            if (connectedDevice.name != null) connectedDevice.name else "Unknown Device"
    }

    private fun disconnectedState() {
        label_bluetooth_status.text = "Not Connected"
        label_bluetooth_status.setTextColor(Color.parseColor("#d32f2f"))
        label_bluetooth_connected_device.text = ""
    }

    // Handle different UI state
    private fun startModeUI() {
        sensorOrientation.disable()
        disableElement(toggle_mode_exploration)
        disableElement(toggle_mode_fastest_path)
        disableElement(button_set_waypoint)
        disableElement(button_set_origin)
        disableElement(button_direction_left)
        disableElement(button_direction_right)
        disableElement(button_direction_up)
        disableElement(switch_motion_control)
        disableElement(button_reset_map)
        disableElement(joystickView)
        switch_motion_control.isChecked = false
    }

    private fun endModeUI() {
        enableElement(toggle_mode_exploration)
        enableElement(toggle_mode_fastest_path)
        enableElement(button_set_waypoint)
        enableElement(button_set_origin)
        enableElement(button_direction_left)
        enableElement(button_direction_right)
        enableElement(button_direction_up)
        enableElement(switch_motion_control)
        enableElement(button_reset_map)
        enableElement(joystickView)
    }

    private fun disableElement(view: View?) {
        view?.isEnabled = false
        view?.alpha = 0.7f
    }

    private fun enableElement(view: View?) {
        view?.isEnabled = true
        view?.alpha = 1f
    }

    private fun updateRobotPositionLabel() {
        label_origin_coordinateX.text = MapDrawer.Robot_X.toString()
        label_origin_coordinateY.text = MapDrawer.getRobotInvertY().toString()
        label_robot_direction.text = MapDrawer.direction
    }

    private fun updateWaypointLabel() {
        label_waypoint_coordinateX.text = MapDrawer.Way_Point_X.toString()
        label_waypoint_coordinateY.text = MapDrawer.getWayPointYInvert().toString()
    }

    // Dialog Builders
    private fun dialogConfigString() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_string_configs, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)

        // configure event listeners
        textboxString1 = dialog.findViewById(R.id.textbox_string1)
        textboxString2 = dialog.findViewById(R.id.textbox_string2)
        dialog.findViewById<Button>(R.id.button_send_string1).setOnClickListener(sendString1)
        dialog.findViewById<Button>(R.id.button_send_string2).setOnClickListener(sendString2)
        dialog.findViewById<Button>(R.id.button_save_string_config)
            .setOnClickListener(saveStringConfig)

        dialogBuilder.create()
        dialogBuilder.show()
        setStringConfig(textboxString1, textboxString2)
    }

    private fun dialogMessageLog() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_message, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)

        dialog.findViewById<Button>(R.id.button_send_message).setOnClickListener(sendMessage)
        textboxSendMessage = dialog.findViewById(R.id.textbox_send_message)
        scrollView = dialog.findViewById(R.id.msg_scroll_view)
        messageLogView = dialog.findViewById(R.id.message_log)
        messageLogView?.movementMethod = ScrollingMovementMethod()
        messageLogView?.text = messageLog.getLog()
        messageLogView?.setTextIsSelectable(true)
        scrollView?.post {
            Log.d(TAG, "Attempting to full scroll down"); scrollView?.fullScroll(
            ScrollView.FOCUS_DOWN
        )
        }
        dialogBuilder.show()
    }

    private lateinit var mdfStringFolderAdapter: StringAdapter
    private fun getMdfFolder(): File {
        val dir = File(this.filesDir, "mdf")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    @Throws(FileNotFoundException::class)
    private fun saveMdfToFile() {
        val dir = getMdfFolder()
        val f = File(dir, "mdf-${System.currentTimeMillis()}.txt")
        val pw = PrintWriter(BufferedOutputStream(FileOutputStream(f)))
        pw.println(Parser.mdfPayload)
        pw.println(Parser.hexMDF)
        pw.println(Parser.hexExplored)
        pw.println("{ ${Parser.hexImage}}")
        pw.flush()
        pw.close()
    }

    private fun dialogFileManager() {
        val dir = getMdfFolder()
        val fileList = dir.list() ?: arrayOf()
        val dialog = inflater.inflate(R.layout.dialog_mdf_manager, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)

        val lv = dialog.findViewById<ListView>(R.id.mdf_list)
        mdfStringFolderAdapter = StringAdapter(applicationContext, fileList)
        lv.adapter = mdfStringFolderAdapter
        lv.setOnItemClickListener { _, _, position, _ ->
            val fileName = mdfStringFolderAdapter.getItem(position)
            mdfFileToExport = File(getMdfFolder(), fileName)
            val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(exportIntent, INTENT_EXPORT)
        }
        dialog.findViewById<Button>(R.id.button_clear_all).setOnClickListener {
            // Delete everything from list
            dir.deleteRecursively()
            mdfStringFolderAdapter = StringAdapter(applicationContext, arrayOf())
            lv.adapter = mdfStringFolderAdapter
            Toast.makeText(it.context, "Cleared all MDF Strings", Toast.LENGTH_SHORT).show()
        }
        dialogBuilder.show()
    }

    private fun dialogDataInspector() {
        // Save to file
        try {
            saveMdfToFile()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot save file, lets just move on :shifty_looking_eyes:")
        }

        // View configs
        val dialog = inflater.inflate(R.layout.dialog_data_inspector, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)

        val labelMdf1Content = dialog.findViewById<TextView>(R.id.label_mdf1_content)
        val labelMdf2Content = dialog.findViewById<TextView>(R.id.label_mdf2_content)
        val labelImageContent = dialog.findViewById<TextView>(R.id.label_image_content)
        labelMdf1Content.text = "0x${Parser.hexMDF}"
        labelMdf1Content.setTextIsSelectable(true)
        labelMdf2Content.text = "0x${Parser.hexExplored}"
        labelMdf2Content.setTextIsSelectable(true)
        labelImageContent.text = "{${Parser.hexImage}}"
        labelImageContent.setTextIsSelectable(true)

        dialogBuilder.show()
    }

    private fun dialogDevices() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_devices, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)
        listviewDevices = dialog.findViewById<View>(R.id.listView_devices) as ListView
        val adapter = DeviceAdapter(applicationContext, deviceList)
        listviewDevices.adapter = adapter
        buttonBluetoothServerListen = dialog.findViewById(R.id.button_bluetooth_server_listen)
        buttonScan = dialog.findViewById(R.id.button_scan)

        if (connectionThread != null) {
            disableElement(listviewDevices)
            disableElement(buttonBluetoothServerListen)
            disableElement(buttonScan)
        }

        // Configure event listener
        dialog.findViewById<Button>(R.id.button_scan).setOnClickListener(scanDevice)
        dialog.findViewById<Button>(R.id.button_bluetooth_server_listen)
            .setOnClickListener(startBluetoothServer)
        listviewDevices.onItemClickListener = connectDevice

        isPairedDevicesOnly = false
        dialogBuilder.show()
    }

    private var isPairedDevicesOnly = false
    private fun dialogPairedDevices() {
        // View configs
        val dialog = inflater.inflate(R.layout.dialog_devices, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialog)
        listviewDevices = dialog.findViewById(R.id.listView_devices)
        val pairedDevices = bluetoothAdapter.bondedDevices
        pairedDevices.forEach { addDevice(it, it.name, it.address) }

        val adapter = DeviceAdapter(applicationContext, deviceList)
        listviewDevices.adapter = adapter
        buttonBluetoothServerListen = dialog.findViewById(R.id.button_bluetooth_server_listen)
        dialog.findViewById<TextView>(R.id.btconn_instructions).text =
            "Make sure device is nearby before using this feature. Also disconnect current connections"
        dialog.findViewById<TextView>(R.id.label_dialog_bluetooth_title).text =
            "Reconnect Bluetooth Connection"
        dialog.findViewById<Button>(R.id.button_scan).visibility = View.GONE

        if (connectionThread != null) {
            disableElement(listviewDevices)
            disableElement(buttonBluetoothServerListen)
        }

        // Configure event listeners
        dialog.findViewById<Button>(R.id.button_bluetooth_server_listen)
            .setOnClickListener(startBluetoothServer)
        listviewDevices.onItemClickListener = connectDevice

        isPairedDevicesOnly = true
        dialogBuilder.show()
    }

    private fun showDialogObstacle(obs:ObstacleView){
        Log.d("obstacle", "initialize")
        val dialogView = inflater.inflate(R.layout.dialog_obstacle, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialogView)
        val textInputImg = dialogView.findViewById<EditText>(R.id.textbox_image)
        val textInputImgFace = dialogView.findViewById<EditText>(R.id.textbox_image_direction)
        val dialog:AlertDialog = dialogBuilder.create()
        dialogView.findViewById<Button>(R.id.button_update_obstacle_config).setOnClickListener(View.OnClickListener{
            obs.setImage(textInputImg.text.toString().first())
            obs.setImageFace(textInputImgFace.text.toString().first())
            dialog.dismiss()
        })
        dialog.show()
        Log.d("obstacle", "end")
    }

    // Event Listeners
    private val onJoystickLeft = View.OnClickListener {
        sendStringToBtConnection(commandWrap(Cmd.DIRECTION_LEFT))
        MapDrawer.moveLeft()
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val onJoystickRight = View.OnClickListener {
        sendStringToBtConnection(commandWrap(Cmd.DIRECTION_RIGHT))
        MapDrawer.moveRight()
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val onJoystickUp = View.OnClickListener {
        val xAxis = MapDrawer.Robot_X
        val yAxis = MapDrawer.Robot_Y

        MapDrawer.moveUp()
        if (!(xAxis == MapDrawer.Robot_X && yAxis == MapDrawer.Robot_Y)) sendStringToBtConnection(
            commandWrap(Cmd.DIRECTION_UP)
        )
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val onStart = View.OnClickListener {
        if (startModeState) {
            startModeState = false
            button_start_phase.text = "Start"
            sendStringToBtConnection(commandWrap(Cmd.STOP))
            timer.cancel()
            endModeUI()
        } else {
            startModeState = true
            button_start_phase.text = "Stop"
            if (fastestPathModeState) sendStringToBtConnection(commandWrap(Cmd.FASTEST_PATH_START))
            else sendStringToBtConnection(commandWrap(Cmd.EXPLORATION_START))

            timer = object : CountDownTimer(30000000, 1000) {
                override fun onTick(l: Long) {
                    val timePassed = 30000000 - l
                    var seconds = timePassed / 1000
                    val minutes = seconds / 60
                    seconds %= 60
                    val timeFormatter = DecimalFormat("00")
                    val time =
                        "${timeFormatter.format(minutes)} m ${timeFormatter.format(seconds)} s"
                    label_time_elapsed.text = time
                }

                override fun onFinish() {}
            }.start()
            startModeUI()
        }
    }
    private val onSetOrigin = View.OnClickListener {
        if (!MapDrawer.selectStartPoint && !MapDrawer.selectWayPoint) {
            button_set_origin.text = "Confirm Origin"
            disableElement(button_set_waypoint)
            disableElement(button_direction_left)
            disableElement(button_direction_right)
            disableElement(button_direction_up)
            sensorOrientation.disable()
            switch_motion_control.isChecked = false
            disableElement(switch_motion_control)
            disableElement(button_start_phase)
            disableElement(button_reset_map)
            MapDrawer.setSelectStartPoint()
            canvas_gridmap.invalidate()
        } else if (MapDrawer.selectStartPoint) {
            button_set_origin.text = "Set Origin"
            enableElement(button_set_waypoint)
            enableElement(button_direction_left)
            enableElement(button_direction_right)
            enableElement(button_direction_up)
            enableElement(switch_motion_control)
            enableElement(button_start_phase)
            enableElement(button_reset_map)
            val msg =
                ";{$FROMANDROID\"com\":\"startingPoint\",\"startingPoint\":[${MapDrawer.Start_Point_X},${MapDrawer.getStartPointYInvert()},${MapDrawer.getRotationDir()}]}"
            sendStringToBtConnection(msg)

            MapDrawer.setSelectStartPoint()
            MapDrawer.updateStartPoint()
            canvas_gridmap.invalidate()
        }
    }
    private val onSetWaypoint = View.OnClickListener {
        if (!MapDrawer.selectStartPoint && !MapDrawer.selectWayPoint) {
            button_set_waypoint.text = "Confirm Waypoint"
            disableElement(button_set_origin)
            disableElement(button_direction_left)
            disableElement(button_direction_right)
            disableElement(button_direction_up)
            sensorOrientation.disable()
            switch_motion_control.isChecked = false
            disableElement(switch_motion_control)
            disableElement(button_start_phase)
            disableElement(button_reset_map)
            MapDrawer.setSelectWayPoint()
            canvas_gridmap.invalidate()
        } else if (MapDrawer.selectWayPoint) {
            button_set_waypoint.text = "Set Waypoint"
            enableElement(button_set_origin)
            enableElement(button_direction_left)
            enableElement(button_direction_right)
            enableElement(button_direction_up)
            enableElement(switch_motion_control)
            enableElement(button_start_phase)
            enableElement(button_reset_map)
            val msg =
                ";{$FROMANDROID\"com\":\"wayPoint\",\"wayPoint\":[${MapDrawer.Way_Point_X},${MapDrawer.getWayPointYInvert()}]}"
            sendStringToBtConnection(msg)

            MapDrawer.setSelectWayPoint()
            canvas_gridmap.invalidate()
        }
    }
    private val onToggleMotionControl =
        CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                sensorOrientation.enable()
                disableElement(button_direction_left)
                disableElement(button_direction_right)
                disableElement(button_direction_up)
                disableElement(joystickView)
            } else {
                sensorOrientation.disable()
                enableElement(button_direction_left)
                enableElement(button_direction_right)
                enableElement(button_direction_up)
                enableElement(joystickView)
            }
        }
    private val onSetMap = View.OnTouchListener { _, motionEvent ->
        if (motionEvent != null) {
            Log.d(
                "map", "x: " + motionEvent.x + ", y: " + motionEvent
                    .y
            )
            if (motionEvent.action == MotionEvent.ACTION_DOWN && (MapDrawer.selectStartPoint || MapDrawer.selectWayPoint)) {
                val xAxis = (motionEvent.x / MapDrawer.gridDimensions).toInt()
                val yAxis = (motionEvent.y / MapDrawer.gridDimensions).toInt()

                if (MapDrawer.validMidpoint(xAxis, yAxis)) {
                    MapDrawer.updateSelection(xAxis, yAxis)
                    canvas_gridmap.invalidate()
                }
                updateRobotPositionLabel()
                updateWaypointLabel()
            }
        }
        false
    }
    private val onRefreshState = View.OnClickListener {
        canvas_gridmap.invalidate()
        updateRobotPositionLabel()
    }
    private val onChangeFastestPathMode =
        CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            fastestPathModeState = b
            label_time_elapsed.text = "00 m 00 s"
            Log.d(TAG, "Fastest Path Mode : $fastestPathModeState")
        }
    private val onChangeAutoMode =
        CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            autoModeState = b
            Log.d(TAG, "Auto Mode : $autoModeState")
        }
    private val onResetMap = View.OnClickListener {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle("Map Reset")
            setMessage("Do you want to reset the map?")
            setNegativeButton("YES") { dialogInterface, _ ->
                MapDrawer.resetMap()
                image_content.setImageResource(R.drawable.img_0)
                canvas_gridmap.invalidate()
                dialogInterface.dismiss()
                sendStringToBtConnection(commandWrap(Cmd.CLEAR)) // Send Clear
            }
            setPositiveButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
        }.create()
        dialog.show()
    }

    // Event Listeners for Dialog Builders
    private val sendString1 = View.OnClickListener {
        val data = textboxString1?.text.toString()
        Log.d(TAG, "Data Sent (String 1) : $data")
        sendStringToBtConnection(data)
    }
    private val sendString2 = View.OnClickListener {
        val data = textboxString2?.text.toString()
        Log.d(TAG, "Data Sent (String 2) : $data")
        sendStringToBtConnection(data)
    }
    private val saveStringConfig =
        View.OnClickListener { saveStringConfig(textboxString1, textboxString2) }
    private val sendMessage = View.OnClickListener {
        val data = textboxSendMessage?.text.toString()
        Log.d(TAG, "Message Sent : $data")
        messageLog.addMessage(sg.edu.ntu.scse.mdp.g39.mdpkotlin.entity.Message.MESSAGE_SENDER, data)
        messageLogView?.text = messageLog.getLog()
        scrollView?.post {
            Log.d(TAG, "Attempting to full scroll down"); scrollView?.fullScroll(
            ScrollView.FOCUS_DOWN
        )
        }
        sendStringToBtConnection(data)
        textboxSendMessage?.setText("")
    }
    private val scanDevice = View.OnClickListener {
        if (buttonScan?.text == "Scan Devices") {
            disableElement(buttonBluetoothServerListen)
            buttonScan?.text = "Stop Scan"
            clearBtDeviceList()
            val i = bluetoothAdapter.startDiscovery()
            Log.d("bluetooth", "startdiscovery = "+i.toString())
        } else if (buttonScan?.text == "Stop Scan") {
            enableElement(buttonBluetoothServerListen)
            buttonScan?.text = "Scan Devices"
            bluetoothAdapter.cancelDiscovery()
        }
    }

    private val connectDevice = AdapterView.OnItemClickListener { _, _, i, _ ->
        val item = deviceList[i]
        val device = item.device

        Log.d(TAG, "Connect")
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

        connectedDevice = device
        connectBluetoothDevice()
        isServer = false
    }
    private val startBluetoothServer = View.OnClickListener {
        if (buttonBluetoothServerListen?.text == "Stop Bluetooth Server") {
            buttonBluetoothServerListen?.text = "Start Bluetooth Server"
            enableElement(listviewDevices)
            enableElement(buttonScan)
        } else if (buttonBluetoothServerListen?.text == "Start Bluetooth Server") {
            buttonBluetoothServerListen?.text = "Stop Bluetooth Server"
            disableElement(listviewDevices)
            disableElement(buttonScan)

            connectionThread = BluetoothService(streamHandler)
            connectionThread?.startServer(bluetoothAdapter)
            isServer = true
        }
    }


    private fun disconnectBluetoothDevice() {
        connectionThread?.cancel()
        connectionThread = null
        disconnectedState()
        disconnectState = true
    }

    private fun connectBluetoothDevice() {
        connectionThread = BluetoothService(streamHandler)
        connectionThread?.connectDevice(connectedDevice)
    }

    private fun sendStringToBtConnection(data: String) {
        if (connectionThread != null) connectionThread?.write(data)
        else notConnected()
    }

    // Help functions for storing data in Shared Preferences
    private fun setStringConfig(field_1: EditText?, field_2: EditText?) {
        if (field_1 == null || field_2 == null) {
            Log.e(TAG, "Uninitialized field, exiting")
            return
        }
        val sharedPref = applicationContext.getSharedPreferences(
            Store.SHARED_PREFERENCE_KEY,
            Context.MODE_PRIVATE
        )

        sharedPref?.let {
            val string1 = it.getString(Store.STRING_1, "") ?: ""
            val string2 = it.getString(Store.STRING_2, "") ?: ""

            if (string1.isNotEmpty() && string2.isNotEmpty()) {
                Log.d(TAG, "Store : $string1")
                Log.d(TAG, "Store : $string2")
                field_1.setText(string1)
                field_2.setText(string2)
            }
        }
    }

    private fun saveStringConfig(field_1: EditText?, field_2: EditText?) {
        if (field_1 == null || field_2 == null) {
            Log.e(TAG, "Field not initialized, exiting")
            return
        }
        val sharedPref = applicationContext.getSharedPreferences(
            Store.SHARED_PREFERENCE_KEY,
            Context.MODE_PRIVATE
        )
        val editor = sharedPref.edit()

        editor.putString(Store.STRING_1, field_1.text.toString())
        editor.putString(Store.STRING_2, field_2.text.toString())
        editor.apply() // Not using commit as that is synchronous
        savedString()
    }

    // Helper functions for Toasts
    private fun savedString() {
        Toast.makeText(applicationContext, "Strings have been saved", Toast.LENGTH_SHORT).show()
    }

    private fun notConnected() {
        Toast.makeText(applicationContext, "Not connected with any devices", Toast.LENGTH_SHORT)
            .show()
    }

    private fun transmissionFail() {
        Toast.makeText(applicationContext, "Error sending message to device", Toast.LENGTH_SHORT)
            .show()
    }

    // Helper functions for rotation
    private fun handleRotation(degree: Int) {
        Log.d(TAG, degree.toString())
        val time = System.currentTimeMillis()

        if (degree in 80..100 && (time - currentTime) >= 250) {
            MapDrawer.moveRight()
            sendStringToBtConnection(commandWrap(Cmd.DIRECTION_RIGHT))
        } else if (degree in 260..280 && (time - currentTime) >= 100) {
            MapDrawer.moveLeft()
            sendStringToBtConnection(commandWrap(Cmd.DIRECTION_LEFT))
        } else if (degree in 170..190 && (time - currentTime) >= 100) {
            MapDrawer.moveUp()
            sendStringToBtConnection(commandWrap(Cmd.DIRECTION_UP))
        } else return
        updateRobotPositionLabel()
        canvas_gridmap.invalidate()
        currentTime = System.currentTimeMillis()
    }

    // Helper functions to handle received message
    private fun handleAction(payload: String) {
        Log.d("Action", "Parsing $payload")
        val parse = Parser(payload)

        val isStatus = parse.setStatus()
        if (isStatus) {
            handleUpdateStatus(parse.robotStatus)
            return
        }

        if (!parse.validPayload) return

        handleUpdatePosition(parse.robotX, parse.robotY, parse.robotDir)

        parse.processImage()
        handleUpdateImage(parse.lastImageID)
        MapDrawer.setGrid(parse.exploredMap)
    }

    private fun handleUpdateImage(imgID: String) {
        image_content.setImageResource(
            when (imgID) {
                "0" -> R.drawable.img_0
                "1" -> R.drawable.img_1
                "2" -> R.drawable.img_2
                "3" -> R.drawable.img_3
                "4" -> R.drawable.img_4
                "5" -> R.drawable.img_5
                "6" -> R.drawable.img_6
                "7" -> R.drawable.img_7
                "8" -> R.drawable.img_8
                "9" -> R.drawable.img_9
                "10" -> R.drawable.img_10
                "11" -> R.drawable.img_11
                "12" -> R.drawable.img_12
                "13" -> R.drawable.img_13
                "14" -> R.drawable.img_14
                "15" -> R.drawable.img_15
                else -> R.drawable.img_0
            }
        )
    }

    private fun handleUpdatePosition(x_axis: Int, y_axis: Int, dir: String) {
        try {
            MapDrawer.updateCoordinates(x_axis, y_axis, dir)

            if (autoModeState) canvas_gridmap.invalidate()
        } catch (typeEx: ClassCastException) {
            Log.d(TAG, "Unable to cast data into int")
        }
        updateRobotPositionLabel()
    }

    private fun handleUpdateStatus(data: String) {
        Log.d(TAG, "Status Update : $data")
        label_status_details.text = data
    }


    private fun commandWrap(cmd: String): String {
        return ";{$FROMANDROID\"com\":\"${cmd}\"}"
    }


    private val onDragged = View.OnTouchListener { view, motionEvent ->
        view.performClick()
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val data = ClipData.newPlainText("", "")
            val shadowBuilder = DragShadowBuilder(view)
            view.startDrag(data, shadowBuilder, view, 0)
            true
        } else {
            false
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private val onObstacleDraggedOnMap = View.OnDragListener { view, dragEvent ->
        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                try {
                    val obstacle = dragEvent.localState as View
                    val id = (obstacle.parent as View).id
                    val r = (view.parent as ViewGroup).parent as LinearLayout

                    val offsetViewBounds = Rect()
                    //returns the visible bounds
                    obstacle.getDrawingRect(offsetViewBounds)
                    // calculates the relative coordinates to the parent
                    r.offsetDescendantRectToMyCoords(obstacle, offsetViewBounds)

                    val relativeTop = offsetViewBounds.top
                    val relativeLeft = offsetViewBounds.left
                    val xAxis = (relativeLeft / MapDrawer.gridDimensions).toInt()
                    val yAxis = (relativeTop / MapDrawer.gridDimensions).toInt()
                    val bool = MapDrawer.removeObstacle(xAxis + 1, yAxis + 1)
                    view.invalidate()
                } catch (e: Exception) {
                    Log.e("map", e.toString())
                }
            }


            DragEvent.ACTION_DRAG_ENTERED -> {
//                        Log.d("map", "Action is DragEvent.ACTION_DRAG_ENTERED");
            }


            DragEvent.ACTION_DRAG_EXITED -> {
                val obstacle = dragEvent.localState as View
                (obstacle.parent as ViewGroup).removeView(obstacle)
            }


            DragEvent.ACTION_DRAG_LOCATION -> {

            }

            DragEvent.ACTION_DRAG_ENDED -> {

            }


            DragEvent.ACTION_DROP -> {
                val xAxis = (dragEvent.x / MapDrawer.gridDimensions).toInt()
                val yAxis = (dragEvent.y / MapDrawer.gridDimensions).toInt()

                if (MapDrawer.validObstacle(xAxis, yAxis)) {
                    MapDrawer.setObstacle(xAxis, yAxis)
                    val obstacleView = ObstacleView(view.context)
                    obstacleView.width = MapDrawer.gridDimensions * 3
                    obstacleView.height = MapDrawer.gridDimensions * 3
                    obstacleView.setImage('5')
                    obstacleView.setBackgroundColor(resources.getColor(android.R.color.white))
                    val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
                    params.leftMargin = (xAxis - 1) * MapDrawer.gridDimensions
                    params.topMargin = (yAxis - 1) * MapDrawer.gridDimensions
                    showDialogObstacle(obstacleView)
                    obstacleView.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                        Log.d("obstacle", motionEvent.action.toString())
                        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                            val data = ClipData.newPlainText("", "")
                            val shadowBuilder = DragShadowBuilder(
                                view
                            )
                            view.startDrag(data, shadowBuilder, view, 0)
                            view.visibility = View.INVISIBLE
                            true
                        } else {
                            false
                        }
                    }
                    try {
                        map_relative_container.addView(obstacleView, params)
                    } catch (e: Exception) {
                        Log.e("map", e.toString())
                    }

                }
//                Log.d("map", "xaxis: $xAxis, yaxis: $yAxis")
                view.invalidate()
            }
        }
        return@OnDragListener true

    }

    private val onMove = JoystickView.OnMoveListener { angle, _ ->
        if (angle in 46..134) {
            val xAxis = MapDrawer.Robot_X
            val yAxis = MapDrawer.Robot_Y

            MapDrawer.moveUp()
            if (!(xAxis == MapDrawer.Robot_X && yAxis == MapDrawer.Robot_Y)) sendStringToBtConnection(
                commandWrap(
                    Cmd.DIRECTION_UP
                )
            )
            canvas_gridmap.invalidate()
            updateRobotPositionLabel()
        } else if ((angle in 1..45) || (angle in 316..359)) {
            sendStringToBtConnection(commandWrap(Cmd.DIRECTION_RIGHT))
            MapDrawer.moveRight()
            canvas_gridmap.invalidate()
            updateRobotPositionLabel()
        } else if (angle in 135..254) {
            sendStringToBtConnection(commandWrap(Cmd.DIRECTION_LEFT))
            MapDrawer.moveLeft()
            canvas_gridmap.invalidate()
            updateRobotPositionLabel()
        }
    }

    companion object {
        private const val TAG = "Main"
        const val FROMANDROID = "\"from\":\"Android\","

        // File Management
        private var mdfFileToExport: File? = null
        private const val INTENT_EXPORT = 7
    }
}