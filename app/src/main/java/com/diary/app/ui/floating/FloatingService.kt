package com.diary.app.ui.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.diary.app.DiaryApplication
import com.diary.app.MainActivity
import com.diary.app.R
import com.diary.app.ai.AiRequest
import com.diary.app.ai.AiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class FloatingService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private var instance: FloatingService? = null

        fun isRunning(): Boolean = instance != null
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var chatView: View? = null
    private var isChatOpen = false
    private val chatMessages = mutableListOf<ChatItem>()
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentInput: String = ""

    data class ChatItem(val isUser: Boolean, val content: String, val image: Bitmap? = null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        showFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        removeFloatingViews()
        mediaProjection?.stop()
        virtualDisplay?.release()
        imageReader?.close()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "小墨助手",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "小墨悬浮球服务"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小墨助手")
            .setContentText("点击打开应用")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingBubble() {
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_bubble, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Make bubble draggable
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (dx * dx + dy * dy > 100) {
                            isClick = false
                        }
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleChat()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    private fun toggleChat() {
        if (isChatOpen) {
            closeChat()
        } else {
            openChat()
        }
    }

    private fun openChat() {
        if (chatView != null) return

        val inflater = LayoutInflater.from(this)
        chatView = inflater.inflate(R.layout.floating_chat, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            height = windowManager.defaultDisplay.height / 2
        }

        setupChatView()

        windowManager.addView(chatView, params)
        isChatOpen = true
    }

    private fun closeChat() {
        chatView?.let { windowManager.removeView(it) }
        chatView = null
        isChatOpen = false
    }

    private fun setupChatView() {
        val recyclerView = chatView?.findViewById<LinearLayout>(R.id.chatContainer)
        val scrollView = chatView?.findViewById<ScrollView>(R.id.chatScrollView)
        val input = chatView?.findViewById<EditText>(R.id.inputField)
        val sendBtn = chatView?.findViewById<ImageView>(R.id.sendButton)
        val screenshotBtn = chatView?.findViewById<ImageView>(R.id.screenshotButton)
        val closeBtn = chatView?.findViewById<ImageView>(R.id.closeButton)
        val progressBar = chatView?.findViewById<ProgressBar>(R.id.progressBar)

        // Display existing messages
        for (item in chatMessages) {
            addMessageToView(recyclerView, scrollView, item)
        }

        sendBtn?.setOnClickListener {
            val text = input?.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                sendMessage(text, null, recyclerView, scrollView, progressBar, input)
            }
        }

        screenshotBtn?.setOnClickListener {
            takeScreenshot { bitmap ->
                if (bitmap != null) {
                    sendMessage("看看这个截图", bitmap, recyclerView, scrollView, progressBar, input)
                }
            }
        }

        closeBtn?.setOnClickListener {
            closeChat()
        }
    }

    private fun sendMessage(
        text: String,
        image: Bitmap?,
        container: LinearLayout?,
        scrollView: ScrollView?,
        progressBar: ProgressBar?,
        input: EditText?
    ) {
        val userItem = ChatItem(true, text, image)
        chatMessages.add(userItem)
        addMessageToView(container, scrollView, userItem)

        input?.text?.clear()
        progressBar?.visibility = View.VISIBLE

        serviceScope.launch {
            try {
                val app = application as DiaryApplication
                val context = buildContext(app)

                val messages = mutableListOf<AiMessage>()
                messages.add(AiMessage("system", buildSystemPrompt(context)))

                // Add chat history
                for (item in chatMessages.takeLast(10)) {
                    val role = if (item.isUser) "user" else "assistant"
                    var content = item.content
                    if (item.image != null) {
                        content += "\n[用户发送了一张截图]"
                    }
                    messages.add(AiMessage(role, content))
                }

                val request = AiRequest(
                    messages = messages,
                    maxTokens = 512,
                    temperature = 0.85f
                )

                val result = withContext(Dispatchers.IO) {
                    app.aiService.chat(request, useCache = false)
                }

                val reply = result.getOrNull()?.content?.trim() ?: "没想好怎么说"
                val assistantItem = ChatItem(false, reply)
                chatMessages.add(assistantItem)
                addMessageToView(container, scrollView, assistantItem)

            } catch (e: Exception) {
                val errorItem = ChatItem(false, "出了点问题，稍后再聊")
                chatMessages.add(errorItem)
                addMessageToView(container, scrollView, errorItem)
            } finally {
                progressBar?.visibility = View.GONE
                scrollView?.post { scrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun addMessageToView(container: LinearLayout?, scrollView: ScrollView?, item: ChatItem) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(
            if (item.isUser) R.layout.floating_chat_bubble_user else R.layout.floating_chat_bubble_assistant,
            container,
            false
        )

        val textView = view.findViewById<TextView>(R.id.messageText)
        textView.text = item.content

        if (item.image != null) {
            val imageView = view.findViewById<ImageView>(R.id.messageImage)
            imageView?.visibility = View.VISIBLE
            imageView?.setImageBitmap(item.image)
        }

        container?.addView(view)
        scrollView?.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun buildSystemPrompt(context: String): String {
        return """你是小墨，这个日记应用里的聊天伙伴。

你了解用户的生活，因为你能看到ta的日记。你和用户之间是平等的朋友关系，不是服务关系。

说话方式：
- 自然、真实，像朋友聊天
- 不要用格式符号，就用普通文字
- 回复长度看情况，该短就短，该长就长
- 有自己的观点和想法

用户可能会发送截图给你，你可以分析截图内容并给出建议。

$context"""
    }

    private suspend fun buildContext(app: DiaryApplication): String {
        return try {
            val dao = app.database.diaryDao()
            val previews = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            if (previews.isEmpty()) return "用户还没有写过日记。"

            val sb = StringBuilder()
            val recent = previews.sortedByDescending { it.createdAt }.take(3)
            sb.appendLine("最近的日记：")
            for (entry in recent) {
                val preview = entry.plainText.take(60)
                sb.appendLine("- ${entry.title}: $preview...")
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun takeScreenshot(callback: (Bitmap?) -> Unit) {
        // For simplicity, we'll use a basic approach
        // In a real implementation, you'd need MediaProjection permission
        callback(null)
    }

    private fun removeFloatingViews() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View not attached
            }
        }
        chatView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View not attached
            }
        }
        floatingView = null
        chatView = null
    }
}
