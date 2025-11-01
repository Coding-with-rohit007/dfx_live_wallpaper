package com.example.dfxwallpaper

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper
import android.content.res.Resources
import java.util.*
import kotlin.math.*

class DFXWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return DFXEngine()
    }

    inner class DFXEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var running = false
        private var surfaceW = 1
        private var surfaceH = 1
        private val frameDelay = (1000L / 60L)

        private val bgPaint = Paint().apply { color = Color.argb(32, 0, 0, 0) }
        private val matrixPaint = Paint().apply {
            color = Color.rgb(0,255,127)
            style = Paint.Style.FILL
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        private val sparkPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.FILL
        }
        private val textPaint = Paint().apply {
            color = Color.argb(220, 0, 255, 127)
            textSize = 40f * Resources.getSystem().displayMetrics.density
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        private val random = Random()
        private val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#\$%^&*()<>[]{}\\/|"
        private var fontSizePx = (16f * Resources.getSystem().displayMetrics.density)
        private var columns = 1
        private var drops = IntArray(1)

        private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Int)
        private val sparks = mutableListOf<Spark>()

        private var maskBmp: Bitmap? = null
        private var maskScale = 1f
        private var maskAngle = 0f

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (running) handler.postDelayed(this, frameDelay)
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            try {
                val res = applicationContext.resources
                val id = res.getIdentifier("mask", "drawable", applicationContext.packageName)
                if (id != 0) maskBmp = BitmapFactory.decodeResource(res, id)
            } catch (e: Exception) {
                maskBmp = null
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            surfaceW = width
            surfaceH = height
            fontSizePx = max(12f, (width / 60f))
            matrixPaint.textSize = fontSizePx
            columns = max(2, (width / fontSizePx).toInt())
            drops = IntArray(columns) { random.nextInt(100) }
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            running = true
            handler.post(drawRunnable)
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            running = false
            handler.removeCallbacks(drawRunnable)
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            if (visible) handler.post(drawRunnable) else handler.removeCallbacks(drawRunnable)
        }

        private fun makeSpark(x: Float, y: Float) {
            val vx = (random.nextFloat() - 0.5f) * 2f
            val vy = (random.nextFloat() - 2.2f) * 2f
            val life = 40 + random.nextInt(70)
            sparks.add(Spark(x, y, vx, vy, life))
            if (sparks.size > 120) sparks.removeAt(0)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.argb(32, 0, 0, 0))

                    matrixPaint.color = Color.rgb(0, 255, 127)
                    matrixPaint.textSize = fontSizePx
                    for (i in 0 until drops.size) {
                        val c = letters[random.nextInt(letters.length)].toString()
                        val x = i * fontSizePx
                        val y = drops[i] * fontSizePx
                        canvas.drawText(c, x, y, matrixPaint)
                        if (y > surfaceH && random.nextFloat() > 0.975f) drops[i] = 0
                        drops[i]++
                    }

                    val it = sparks.listIterator()
                    while (it.hasNext()) {
                        val s = it.next()
                        val alpha = (s.life / 100f).coerceIn(0f,1f)
                        sparkPaint.alpha = (255 * alpha).toInt()
                        canvas.drawRect(s.x, s.y, s.x + 3f, s.y + 3f, sparkPaint)
                        s.x += s.vx; s.y += s.vy; s.vy += 0.06f; s.life--
                        if (s.life <= 0) it.remove()
                    }

                    maskBmp?.let { bmp ->
                        maskAngle += 0.6f
                        maskScale = 1f + sin(System.currentTimeMillis() * 0.0015).toFloat() * 0.01f
                        val bmpW = bmp.width * maskScale
                        val bmpH = bmp.height * maskScale
                        val left = (surfaceW * 0.6f)
                        val top = (surfaceH * 0.12f)
                        val matrix = Matrix()
                        matrix.postTranslate(-bmp.width/2f, -bmp.height/2f)
                        matrix.postRotate(maskAngle)
                        matrix.postScale(maskScale, maskScale)
                        matrix.postTranslate(left + bmpW/2f, top + bmpH/2f)
                        val paint = Paint().apply { isFilterBitmap = true; alpha = 220 }
                        canvas.drawBitmap(bmp, matrix, paint)
                    }

                    val now = Calendar.getInstance()
                    val hh = String.format("%02d", now.get(Calendar.HOUR_OF_DAY))
                    val mm = String.format("%02d", now.get(Calendar.MINUTE))
                    val ss = String.format("%02d", now.get(Calendar.SECOND))
                    val timeText = "$hh:$mm:$ss"
                    val dayNames = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
                    val dateText = "${now.get(Calendar.DAY_OF_MONTH)}/${now.get(Calendar.MONTH)+1}/${now.get(Calendar.YEAR)} — ${dayNames[now.get(Calendar.DAY_OF_WEEK)-1]}"

                    val rectW = max(textPaint.measureText(timeText), textPaint.measureText(dateText)) + 28f
                    val rectH = textPaint.textSize * 2.2f
                    val rectLeft = surfaceW - rectW - 20f
                    val rectTop = surfaceH - rectH - 20f
                    val bgR = Paint().apply { color = Color.argb(96, 0, 0, 0); style = Paint.Style.FILL }
                    val r = RectF(rectLeft, rectTop, rectLeft + rectW, rectTop + rectH)
                    canvas.drawRoundRect(r, 10f, 10f, bgR)

                    textPaint.textSize = (18f * Resources.getSystem().displayMetrics.density)
                    textPaint.color = Color.argb(230, 0, 255, 127)
                    canvas.drawText(timeText, rectLeft + 12f, rectTop + textPaint.textSize, textPaint)
                    textPaint.textSize = (12f * Resources.getSystem().displayMetrics.density)
                    textPaint.color = Color.argb(160, 154, 160, 166)
                    canvas.drawText(dateText, rectLeft + 12f, rectTop + textPaint.textSize*1.9f, textPaint)

                    if (random.nextFloat() < 0.06f) {
                        val sx = random.nextInt(surfaceW).toFloat()
                        val sy = random.nextInt((surfaceH*0.6).toInt()).toFloat()
                        makeSpark(sx, sy)
                    }
                }
            } catch (e: Exception) {
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
