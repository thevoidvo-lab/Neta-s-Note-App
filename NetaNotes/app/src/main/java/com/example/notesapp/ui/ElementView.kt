package com.example.notesapp.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.notesapp.data.NoteElement
import com.example.notesapp.util.FontManager
import kotlin.math.atan2
import kotlin.math.sqrt

class ElementView(
    context: Context,
    var element: NoteElement,
    private val callbacks: Callbacks
) : FrameLayout(context) {

    interface Callbacks {
        fun onElementChanged(element: NoteElement, commit: Boolean)
        fun onElementTapped(element: NoteElement)
        fun onElementDoubleTapped(element: NoteElement)
        fun onElementLongPressed(element: NoteElement, view: View)
    }

    private var contentView: View
    private var startX = 0f
    private var startY = 0f
    private var startRawX = 0f
    private var startRawY = 0f
    private var startScale = 1f
    private var startRotation = 0f
    private var activePointerId = -1
    private var isTransforming = false

    private val scaleDetector: ScaleGestureDetector
    private var pivotAngle = 0f
    private var lastTouchAngle = 0f
    private var twoFingerActive = false

    init {
        contentView = buildContentView()
        addView(contentView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        applyLayoutFromElement()

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val newScale = (element.scale * detector.scaleFactor).coerceIn(0.2f, 5f)
                element = element.copy(scale = newScale)
                applyTransformOnly()
                return true
            }
        })

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                callbacks.onElementTapped(element)
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                callbacks.onElementDoubleTapped(element)
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                callbacks.onElementLongPressed(element, this@ElementView)
            }
        })

        setOnTouchListener { _, event ->
            if (element.locked) return@setOnTouchListener false
            gestureDetector.onTouchEvent(event)
            scaleDetector.onTouchEvent(event)
            handleDragAndRotate(event)
            true
        }
    }

    private fun buildContentView(): View {
        return when (element.type) {
            "sticker", "image" -> {
                val iv = ImageView(context)
                iv.scaleType = ImageView.ScaleType.FIT_CENTER
                try {
                    Glide.with(context).load(java.io.File(element.content)).into(iv)
                } catch (e: Exception) {
                    iv.setBackgroundColor(Color.LTGRAY)
                }
                iv
            }
            "shape" -> {
                val v = View(context)
                v.setBackgroundColor(parseColorSafe(element.color))
                v
            }
            else -> {
                val tv = TextView(context)
                tv.text = element.content
                tv.setTextColor(parseColorSafe(element.color))
                tv.textSize = element.fontSize ?: 16f
                var style = Typeface.NORMAL
                if (element.bold && element.italic) style = Typeface.BOLD_ITALIC
                else if (element.bold) style = Typeface.BOLD
                else if (element.italic) style = Typeface.ITALIC
                val base = FontManager.loadTypeface(context, element.fontFamily)
                tv.setTypeface(base, style)
                tv.paintFlags = if (element.underline) {
                    tv.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                } else {
                    tv.paintFlags
                }
                tv.gravity = when (element.alignment) {
                    "center" -> android.view.Gravity.CENTER
                    "right" -> android.view.Gravity.END
                    else -> android.view.Gravity.START
                }
                tv
            }
        }
    }

    private fun parseColorSafe(color: String?): Int {
        return try {
            Color.parseColor(color ?: "#FFFFFF")
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    fun refreshContent() {
        removeView(contentView)
        contentView = buildContentView()
        addView(contentView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        applyLayoutFromElement()
    }

    private fun applyLayoutFromElement() {
        val lp = ViewGroup.MarginLayoutParams(element.width.toInt(), element.height.toInt())
        lp.leftMargin = element.x.toInt()
        lp.topMargin = element.y.toInt()
        this.layoutParams = lp
        rotation = element.rotation
        scaleX = element.scale
        scaleY = element.scale
        visibility = if (element.hidden) View.GONE else View.VISIBLE
    }

    private fun applyTransformOnly() {
        rotation = element.rotation
        scaleX = element.scale
        scaleY = element.scale
        callbacks.onElementChanged(element, false)
    }

    private fun handleDragAndRotate(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                startX = (layoutParams as ViewGroup.MarginLayoutParams).leftMargin.toFloat()
                startY = (layoutParams as ViewGroup.MarginLayoutParams).topMargin.toFloat()
                startRawX = event.rawX
                startRawY = event.rawY
                isTransforming = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    twoFingerActive = true
                    lastTouchAngle = angleBetweenFingers(event)
                    startRotation = element.rotation
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && twoFingerActive) {
                    val currentAngle = angleBetweenFingers(event)
                    val delta = currentAngle - lastTouchAngle
                    element = element.copy(rotation = startRotation + delta)
                    applyTransformOnly()
                } else if (event.pointerCount == 1) {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) isTransforming = true
                    var newX = startX + dx
                    var newY = startY + dy
                    val snapped = SnapHelper.snapPoint(newX, newY)
                    newX = snapped.first
                    newY = snapped.second

                    val lp = layoutParams as ViewGroup.MarginLayoutParams
                    lp.leftMargin = newX.toInt()
                    lp.topMargin = newY.toInt()
                    layoutParams = lp

                    element = element.copy(x = newX, y = newY)
                    callbacks.onElementChanged(element, false)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) twoFingerActive = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                twoFingerActive = false
                callbacks.onElementChanged(element, true)
            }
        }
    }

    private fun angleBetweenFingers(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)
        return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    }
}
