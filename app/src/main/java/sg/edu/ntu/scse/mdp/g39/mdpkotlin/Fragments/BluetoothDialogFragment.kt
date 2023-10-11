package sg.edu.ntu.scse.mdp.g39.mdpkotlin.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.DeviceAdapter
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.MainViewModel
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.R
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.databinding.FragmentBluetoothDialogBinding
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.disableElement
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.enableElement
import sg.edu.ntu.scse.mdp.g39.mdpkotlin.service.BluetoothService

class BluetoothDialogFragment: DialogFragment(R.layout.fragment_bluetooth_dialog) {
    private val viewModel: MainViewModel by activityViewModels<MainViewModel>()
    private val bluetoothService: BluetoothService? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentBluetoothDialogBinding.inflate(inflater)
        return binding.root

        val deviceListView = binding.listViewDevices
        val adapter = DeviceAdapter(this.context, DeviceList) //TODO: find out how to pass data from main activity to fragment
        deviceListView.adapter = adapter

        val buttonScan:View = binding.buttonScan

        if (bluetoothService != null) {
            disableElement(deviceListView)
            disableElement(buttonScan)
        }

        // Configure event listener
        buttonScan.setOnClickListener(scanDevice)
        listviewDevices.onItemClickListener = connectDevice

        isPairedDevicesOnly = false
        dialogBuilder.show()
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

}