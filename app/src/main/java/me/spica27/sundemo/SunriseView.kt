package me.spica27.sundemo

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import kotlin.math.abs

class SunriseView : View {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


  // 完整运动轨迹
  private val allPath = Path()

  // 地平线以上的部分
  private val topPath = Path()

  // 地平线以下的部分[用于绘制阴影]
  private val topPath2 = Path()

  private val moveAnim = ObjectAnimator.ofFloat(this, "progress", -1.0f, 2.0f).apply {
    duration = 1000
  }

  private var currentProgress = 0f

  @Suppress("unused")
  fun to(@FloatRange(-1.0, 2.0) progress: Float) {
    moveAnim.cancel()
    moveAnim.duration = (abs(progress - currentProgress) / 3f * 800).toLong()
    moveAnim.setFloatValues(currentProgress, progress)
    moveAnim.start()
  }

  // -1~0: 日出前
  // 0~1: 日出中
  // 1~2: 日落后
  fun setProgress(@FloatRange(-1.0, 2.0) progress: Float) {
    currentProgress = progress
    postInvalidateOnAnimation()
  }

  // 用于绘制地平线以下部分
  private val pathPaint1 = Paint().apply {
    color = Color.BLACK
    style = Paint.Style.STROKE
    strokeWidth = 24f
  }

  // 用于绘制地平线以上部分
  private val pathPaint2 = Paint().apply {
    color = Color.BLACK
    style = Paint.Style.STROKE
    strokeWidth = 24f
  }


  private val sunPaint = Paint().apply {
    color = Color.YELLOW
    style = Paint.Style.FILL
  }

  private var lineY = 0f

  private val pathMeasure = PathMeasure()

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)


    // 设置颜色渐变
    pathPaint1.shader = LinearGradient(
      0f, 0f, w * 1f, 0f,
      intArrayOf(
        Color.parseColor("#597ef7"), Color.parseColor("#1d39c4"),
        Color.parseColor("#597ef7")
      ),
      floatArrayOf(0f, 0.5f, 1f),
      Shader.TileMode.CLAMP
    )
    pathPaint2.shader = LinearGradient(
      0f, 0f, w * 1f, 0f,
      intArrayOf(
        Color.parseColor("#fadb14"), Color.parseColor("#fa8c16"),
        Color.parseColor("#fadb14")
      ),
      floatArrayOf(0f, 0.5f, 1f),
      Shader.TileMode.CLAMP
    )


    // 重新计算路径
    allPath.reset()
    allPath.moveTo(0f, h * 1f - pathPaint1.strokeWidth - h / 8f)
    allPath.cubicTo(
      w / 4f + w / 6f, h - h / 6f,
      w / 2f - w / 5f, 0 + h / 6f,
      w / 2f, 0 + h / 6f
    )
    allPath.cubicTo(
      w / 2f + w / 5f, 0 + h / 6f,
      w - (w / 4f + w / 6f), h - h / 6f,
      w * 1f, h * 1f - pathPaint1.strokeWidth - h / 8f
    )

    pathMeasure.setPath(allPath, false)

    // 确定地平线锚点的位置
    val floatArray = FloatArray(2)
    pathMeasure.getPosTan(pathMeasure.length / 6, floatArray, null)
    // 左边的锚点
    val leftPoint = PointF(floatArray[0], floatArray[1])
    pathMeasure.getPosTan(pathMeasure.length / 6 * 5, floatArray, null)
    // 右边的锚点
    val rightPoint = PointF(floatArray[0], floatArray[1])
    // 地平线Y坐标
    lineY = (leftPoint.y + rightPoint.y) / 2
    // 截取地平线以上的部分
    topPath.reset()
    pathMeasure.getSegment(pathMeasure.length / 6, pathMeasure.length / 6 * 5, topPath, true)
    topPath2.reset()
    pathMeasure.getSegment(pathMeasure.length / 6, pathMeasure.length / 6 * 5, topPath2, true)
    topPath2.close()

    sunLightPaint.shader = LinearGradient(
      0f, 0f, 0f, h * 1f,
      intArrayOf(
        Color.parseColor("#ffffb8"),
        Color.TRANSPARENT
      ),
      floatArrayOf(0f, 1f),
      Shader.TileMode.CLAMP
    )
  }

  private val sunLightPaint = Paint().apply {
    color = Color.YELLOW
    style = Paint.Style.FILL
  }

  private val sunPointXY = FloatArray(2)

  private val xf = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)



    // 绘制地平线
    sunPaint.color = ContextCompat.getColor(context, R.color.line_color)
    sunPaint.strokeWidth = 12f
    canvas.drawLine(0f, lineY, width * 1f, lineY, sunPaint)

    // 前1/4为日出前段
    if (currentProgress < 0) {
      // 日出前段
      val progress = currentProgress - (-1) / 1f
      pathMeasure.getPosTan(pathMeasure.length / 6 * progress, sunPointXY, null)
    } else if (currentProgress < 1) {
      // 中间段
      val progress = currentProgress / 1f
      pathMeasure.getPosTan(pathMeasure.length / 6 + pathMeasure.length / 2 * progress, sunPointXY, null)

      // 绘制阴影
      val id = canvas.saveLayer(0f, 0f, width * 1f, height * 1f, null)
      sunPaint.color = Color.BLACK
      canvas.drawPath(topPath2, sunLightPaint)
      sunLightPaint.xfermode = xf
      sunPaint.color = Color.YELLOW
      canvas.drawRect(sunPointXY[0], 0f, width * 1f, height * 1f, sunLightPaint)
      canvas.restoreToCount(id)

      sunLightPaint.xfermode = null
    } else {
      // 日落后段
      val progress = currentProgress - 1 / 1f
      pathMeasure.getPosTan(pathMeasure.length / 6 * 5 + pathMeasure.length / 6 * progress, sunPointXY, null)


      // 绘制阴影
      val id = canvas.saveLayer(0f, 0f, width * 1f, height * 1f, null)
      sunPaint.color = Color.BLACK
      canvas.drawPath(topPath2, sunLightPaint)
      sunLightPaint.xfermode = xf
      sunPaint.color = Color.YELLOW
      canvas.drawRect(sunPointXY[0], 0f, width * 1f, height * 1f, sunLightPaint)
      canvas.restoreToCount(id)

      sunLightPaint.xfermode = null

    }

    // 绘制背景
    canvas.drawPath(allPath, pathPaint1)

    // 绘制地平线以上的部分
    canvas.drawPath(topPath, pathPaint2)

    sunPaint.color = Color.parseColor("#fafafa")
    canvas.drawCircle(sunPointXY[0], sunPointXY[1], 55f, sunPaint)
    sunPaint.color = ContextCompat.getColor(context, R.color.yellow)
    canvas.drawCircle(sunPointXY[0], sunPointXY[1], 40f, sunPaint)
  }


}