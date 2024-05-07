package nfc.auto.off

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.nfc.NfcAdapter
import android.os.IBinder
import android.widget.Button
import android.widget.Toast
class MainActivity : AppCompatActivity() {

    private lateinit var serviceIntent: Intent
    private lateinit var btnToggleNFC: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceIntent = Intent(this, HeightSensorService::class.java)
        btnToggleNFC = findViewById(R.id.btnToggleNFC)
        btnToggleNFC.setOnClickListener {
            if (isServiceRunning(HeightSensorService::class.java)) {
                stopService(serviceIntent)
                btnToggleNFC.text = getString(R.string.turn_on_nfc)
            } else {
                startService(serviceIntent)
                btnToggleNFC.text = getString(R.string.turn_off_nfc)
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}


class HeightSensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var nfcAdapter: NfcAdapter

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // NFC 어댑터 초기화
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]
            if (z > 9.0 || z < -9.0) {
                // Height is more than 3 floors, turn off NFC
                turnOffNFC(applicationContext)
                Toast.makeText(this, "NFC turned off due to height change.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun turnOffNFC(context: Context) {
        if (nfcAdapter.isEnabled) {
            // NFC를 끔
            nfcAdapter.disableForegroundDispatch(context as Activity)
        }
    }

    private fun turnOnNFC(context: Context) {
        val intent = Intent(context, context.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val filters = arrayOf<IntentFilter>()
        val techLists = arrayOf<Array<String>>()

        if (!nfcAdapter.isEnabled) {
            nfcAdapter.enableForegroundDispatch(context as Activity, pendingIntent, filters, techLists)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TOGGLE_NFC) {
            val enableNFC = intent.getBooleanExtra(EXTRA_NFC_ENABLED, false)
            if (enableNFC) {
                turnOnNFC(applicationContext)
            } else {
                turnOffNFC(applicationContext)
            }
        }
        return START_STICKY
    }

    companion object {
        const val ACTION_TOGGLE_NFC = "nfc.auto.off.action.TOGGLE_NFC"
        const val EXTRA_NFC_ENABLED = "nfc.auto.off.extra.NFC_ENABLED"
    }
}
