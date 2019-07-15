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
                            else if (new > 50) {
                                spotifyAppRemote?.let {
                                    it.playerApi.queue("spotify:track:78lgmZwycJ3nzsdgmPPGNx")

                                }
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



        //inthecorner(heartrate)

        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                Log.d("MainActivity", "Connected! Yay!")
                spotifyAppRemote = appRemote

                spotifyAppRemote?.let {
                    // Play a playlist
                    //val trackURI = "spotify:track:1ReO26lbDWtjPdtsaWzSXS"
                    //it.playerApi.play(trackURI)
                    it.playerApi.play("spotify:track:3KkXRkHbMCARz0aVfEt68P")
                    it.playerApi.queue("spotify:track:6TqXcAFInzjp0bODyvrWEq")
                    //it.playerApi.play("spotify:track:6TqXcAFInzjp0bODyvrWEq")
                    // Subscribe to PlayerState
                    /*it.playerApi.subscribeToPlayerState().setEventCallback {
                        val track: Track = it.track
                        Log.d("MainActivity", track.name + " by " + track.artist.name)
                    }*/
                }

                //spotifyAppRemote?.let {


                    /*it.playerApi.subscribeToPlayerState().setEventCallback {
                        val track: Track = it.track
                        Log.d("MainActivity", track.name + " by " + track.artist.name)
                    }*/
                //}

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