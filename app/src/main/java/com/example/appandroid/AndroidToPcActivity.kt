package com.example.appandroid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import android.widget.Button
class AndroidToPcActivity : AppCompatActivity() {
    private lateinit var tvSockets: TextView
    private lateinit var btnStart: Button
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_androidtopc)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvSockets = findViewById(R.id.tvSockets)
        btnStart = findViewById(R.id.btnStart)
        handler = Handler(Looper.getMainLooper())

        btnStart.setOnClickListener {
            val runnableClient = Runnable { startClient() }
            val threadClient = Thread(runnableClient)
            threadClient.start()
        }
    }
    fun startClient() {
        val context = ZMQ.context(1)
        val socket = ZContext().createSocket(SocketType.REQ)
        socket.connect("tcp://192.168.1.102:5556")

        val request = "Hello from Android!"
        socket.send(request.toByteArray(ZMQ.CHARSET), 0)

        val reply = socket.recv(0)
        val response = String(reply, ZMQ.CHARSET)

        handler.post {
            tvSockets.append("Клиент получил: $response\n")
        }

        socket.close()
        context.close()
    }

    override fun onResume() {
        super.onResume()
    }
}