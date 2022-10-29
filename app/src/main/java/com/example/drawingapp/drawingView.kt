package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class drawingView(Context: Context, attrs: AttributeSet) : View(Context, attrs) {
    private var drawPath : CustomPath? = null
    private var canvasBitmap : Bitmap? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var brushSize: Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null
    private val undoPaths = ArrayList<CustomPath>()
    private val mPaths = ArrayList<CustomPath>()
    private var index = 0

    init {
        setUpDrawing()
    }

    fun onClickUndo(){
        if (mPaths.size>0) {
            undoPaths.add(mPaths.removeAt(mPaths.size - 1))
            invalidate()
        }
    }

    fun onClickRedo(){
        if (undoPaths.size>0) {
            mPaths.add(undoPaths[index])
            index = index+1
            invalidate()
        }
    }

    private fun setUpDrawing() {
        drawPaint = Paint()
        drawPath = CustomPath(color, brushSize)

        drawPaint?.color = color

        drawPaint?.style = Paint.Style.STROKE
        drawPaint?.strokeJoin = Paint.Join.ROUND
        drawPaint?.strokeCap = Paint.Cap.ROUND
       // brushSize = 20.toFloat()

        canvasPaint = Paint(Paint.DITHER_FLAG)

    }

    override fun onSizeChanged(w: Int, h: Int, wprev: Int, hprev: Int) {
        super.onSizeChanged(w, h, wprev, hprev)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(canvasBitmap!!,0f,0f,drawPaint)

        for (p in mPaths) {
            drawPaint?.strokeWidth = p.brushThickness
            drawPaint?.color = p.color
            canvas?.drawPath(p, drawPaint!!)
        }

        if (!drawPath!!.isEmpty) {
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color
            canvas?.drawPath(drawPath!!, drawPaint!!)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = color
                drawPath!!.brushThickness = brushSize

                drawPath!!.reset() // Clear any lines and curves from the path, making it empty.
                drawPath!!.moveTo(
                    touchX!!,
                    touchY!!
                )
            }
            MotionEvent.ACTION_MOVE ->{
                drawPath!!.lineTo(
                    touchX!!,
                    touchY!!
                )
            }
            MotionEvent.ACTION_UP->{
                mPaths.add(drawPath!!)
                drawPath = CustomPath(color, brushSize)
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics
        )
        drawPaint!!.strokeWidth = brushSize
    }

    fun setColor(newColor:String){
        color = Color.parseColor(newColor)
        drawPaint?.color = color
    }


    internal inner class CustomPath(var color:Int,var brushThickness:Float):Path()






}