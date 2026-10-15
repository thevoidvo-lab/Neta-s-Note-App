package com.example.notesapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.example.notesapp.data.NoteElement

class CanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    interface CanvasListener {
        fun onElementUpdated(element: NoteElement, commit: Boolean)
        fun onElementTapped(element: NoteElement)
        fun onElementDoubleTapped(element: NoteElement)
        fun onElementLongPressed(element: NoteElement, view: View)
    }

    var listener: CanvasListener? = null
    private val elementViews = mutableMapOf<String, ElementView>()

    private val gridPaint = Paint().apply {
        color = Color.argb(40, 255, 255, 255)
        strokeWidth = 1f
    }

    private var canvasScale = 1f
    private var canvasTranslateX = 0f
    private var canvasTranslateY = 0f

    init {
        setWillNotDraw(false)
    }

    fun renderAll(elements: List<NoteElement>) {
        // hapus view yang elemennya udah gak ada
        val currentIds = elements.map { it.id }.toSet()
        val toRemove = elementViews.keys.filter { it !in currentIds }
        toRemove.forEach {
            elementViews[it]?.let { v -> removeView(v) }
            elementViews.remove(it)
        }

        for (el in elements.sortedBy { it.zIndex }) {
            val existing = elementViews[el.id]
            if (existing == null) {
                val ev = ElementView(context, el, object : ElementView.Callbacks {
                    override fun onElementChanged(element: NoteElement, commit: Boolean) {
                        listener?.onElementUpdated(element, commit)
                    }
                    override fun onElementTapped(element: NoteElement) {
                        listener?.onElementTapped(element)
                    }
                    override fun onElementDoubleTapped(element: NoteElement) {
                        listener?.onElementDoubleTapped(element)
                    }
                    override fun onElementLongPressed(element: NoteElement, view: View) {
                        listener?.onElementLongPressed(element, view)
                    }
                })
                elementViews[el.id] = ev
                addView(ev)
                ev.bringToFront()
            } else {
                existing.element = el
                existing.refreshContent()
                bringChildToFront(existing)
            }
        }
    }

    fun setGridEnabled(enabled: Boolean) {
        SnapHelper.gridEnabled = enabled
        invalidate()
    }

    fun setSnapEnabled(enabled: Boolean) {
        SnapHelper.snapEnabled = enabled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (SnapHelper.gridEnabled) {
            var x = 0f
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                x += SnapHelper.GRID_SIZE
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += SnapHelper.GRID_SIZE
            }
        }
    }

    // pan 2 jari sederhana buat geser seluruh canvas (simple translation, bukan true multi-touch zoom-pan matrix)
    private var lastPanX = 0f
    private var lastPanY = 0f
    private var panning = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    panning = true
                    lastPanX = (event.getX(0) + event.getX(1)) / 2
                    lastPanY = (event.getY(0) + event.getY(1)) / 2
                }
                MotionEvent.ACTION_MOVE -> {
                    if (panning) {
                        val cx = (event.getX(0) + event.getX(1)) / 2
                        val cy = (event.getY(0) + event.getY(1)) / 2
                        val dx = cx - lastPanX
                        val dy = cy - lastPanY
                        translationX += dx
                        translationY += dy
                        lastPanX = cx
                        lastPanY = cy
                    }
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                    panning = false
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun resetPan() {
        translationX = 0f
        translationY = 0f
    }
}
