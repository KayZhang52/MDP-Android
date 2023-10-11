package sg.edu.ntu.scse.mdp.g39.mdpkotlin.model

import android.bluetooth.BluetoothDevice

data class BluetoothModel(
    var connectedDevice: BluetoothDevice?,
    var deviceList: ArrayList<BluetoothDevice>
) {

}