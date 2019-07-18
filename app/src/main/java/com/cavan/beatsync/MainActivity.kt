package com.cavan.beatsync

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import java.io.IOException
import java.util.*
import java.util.function.Function
import kotlin.collections.ArrayList
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    var heartrate: Int = 0


    //first heartrate zone 50-99 beats per minute
    var zone1_pos = 0
    val zone1 = listOf("spotify:track:2tznHmp70DxMyr2XhWLOW0",
                        "spotify:track:5fGWdNGGnvKrrHL6U7c1Vp",
                        "spotify:track:35QAUfIbfIXT3p3cWhaKxZ",
                        "spotify:track:51UtgWS4z1eMPuLQOzPtNH",
                        "spotify:track:5JmJVj3qLsCnBsQ8IC9XLf")


    //second heartrate zone 100-129 beats per minute
    var zone2_pos = 0
    val zone2 = listOf("spotify:track:46efwIlSpvdURIubZCs3jC",
                        "spotify:track:2ZTOEvJeCpaHkWMMKc0ewp",
                        "spotify:track:30LGMhbzgN7poYNYvfTEsi",
                        "spotify:track:6RBMpENxbx74lTdR5SBcaF",
                        "spotify:track:1Y2ExJJ9Dmb9po8K0ybSj3")


    //third heartrate zone 130+ beats per minute
    var zone3_pos = 0
    val zone3 = listOf("spotify:track:63wsuMhok6GgcBRd2strGk",
                        "spotify:track:6GXlXAfXR7C6u1VjR3VMsm",
                        "spotify:track:6TFGNgCyhgHKNn046iG6fa",
                        "spotify:track:6NvRxjfYkkT2SpirAlmsjH",
                        "spotify:track:0HPMKvONbUTomRuuG1LScC")


    //observes when heatrate variable changes and runs function in response
    var heartratespotify: Int by Delegates.observable(0) {
            prop, old, new ->
            if (new > -1) {
                var trackDuration: Long = 0
                var trackPosition: Long = 0
                spotifyAppRemote?.let {

                    it.playerApi.playerState.setResultCallback {
                        trackDuration = it.track.duration
                        trackPosition = it.playbackPosition
                        //Log.d("Position", trackPosition.toString())
                        var timeLeft = trackDuration - trackPosition
                        Log.d("Time Left", timeLeft.toString())
                        if (timeLeft <= 15000) {
                            if (new < 50)
                                spotifyAppRemote?.let {
                                    it.playerApi.queue("spotify:track:3VEFybccRTeWSZRkJxDuNR")

                                }
                            else if (new >= 50 && new < 100) {
                                spotifyAppRemote?.let {
                                    it.playerApi.queue(zone1[zone1_pos])

                                }
                                zone1_pos = (zone1_pos + 1) % zone1.size
                            }
                            else if (new >= 100 && new < 130) {
                                spotifyAppRemote?.let {
                                    it.playerApi.queue(zone2[zone2_pos])

                                }
                                zone2_pos = (zone2_pos + 1) % zone2.size
                            }
                            else if (new >= 130) {
                                spotifyAppRemote?.let {
                                    it.playerApi.queue(zone3[zone3_pos])

                                }
                                zone3_pos = (zone3_pos + 1) % zone3.size
                            }
                        }
                    }
                }
            }




    }

    private var m_bluetoothSocket: BluetoothSocket? = null

    private lateinit var m_pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BT = 1

    private val beagleboneMAC = "58:7A:62:31:08:A7"
    private val clientId = "c3257a577e7f4dee944fbc310a605c39"
    private val redirectUri = "https://example.com/callback/"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    //Class that handles bluetooth and sets the heartrate

    inner class BluetoothClient(device: BluetoothDevice): Thread() {
        var uuid: UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee")
        
        private val socket = device.createRfcommSocketToServiceRecord(uuid)
        override fun run() {
            Log.i("client", "Connecting")
            this.socket.connect()
            Log.i("client", "Sending")

            //val outputStream = this.socket.outputStream

            val inputStream = this.socket.inputStream

            try {
                while (true) {
                    val available = inputStream.available()
                    if (available > 0) {
                        val bytes = ByteArray(available)
                        inputStream.read(bytes, 0, available)
                        val beat = String(bytes)
                        heartrate = beat.toInt()
                        heartratespotify = heartrate
                        Log.i("client", "Received")
                        Log.i("client", beat)
                        Log.i("MainActivity", heartrate.toString())
                    }
                }
            } catch(e: Exception) {
                Log.e("client", "Cannot send", e)
            } finally {
                //outputStream.close()
                inputStream.close()
                this.socket.close()
            }
        }
    }

    override fun onStart() {
        super.onStart()


        //create the bluetooth device

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth Unsupported")
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        val device = bluetoothAdapter!!.getRemoteDevice(beagleboneMAC)


        //try putting in spotify connection
        BluetoothClient(device).start()



        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                Log.d("MainActivity", "Connected! Yay!")
                spotifyAppRemote = appRemote

                spotifyAppRemote?.let {
                    //TODO: replace inital song with initial playlist selection
                    it.playerApi.play("spotify:track:3KkXRkHbMCARz0aVfEt68P")
                }

            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", throwable.message, throwable)
                // Something went wrong when attempting to connect! Handle errors here
            }
        })



    }


    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            Log.d("MainActivity", "Entered disconnected")
            SpotifyAppRemote.disconnect(it)
        }
    }

}