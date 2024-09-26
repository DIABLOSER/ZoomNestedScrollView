package com.mdplus.library

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/**
 *作者：daboluo on 2024/8/13 09:25
 *Email:daboluo719@gmail.com
 * 下拉放大
 */
class ZoomNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var myCallback: MyCallback? = null

    // 用于记录下拉位置
    private var y = 0f

    // zoomView 原本的宽高
    private var zoomViewWidth = 0
    private var zoomViewHeight = 0

    // 是否正在放大
    private var mScaling = false

    // 放大的 view，默认为第一个子 view
    private var zoomView: View? = null

    // 滑动放大系数，系数越大，滑动时放大程度越大
    private var mScaleRatio = 0.5f

    // 最大的放大倍数
    private var mScaleTimes = 2.2f

    // 回弹时间系数，系数越小，回弹越快
    private var mReplyRatio = 0.4f

    fun setZoomView(zoomView: View) {
        this.zoomView = zoomView
    }

    fun setScaleRatio(scaleRatio: Float) {
        this.mScaleRatio = scaleRatio
    }

    fun setScaleTimes(scaleTimes: Float) {
        this.mScaleTimes = scaleTimes
    }

    fun setReplyRatio(replyRatio: Float) {
        this.mReplyRatio = replyRatio
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // 不可过度滚动，否则上移后下拉会出现部分空白的情况
        overScrollMode = OVER_SCROLL_NEVER
        // 获得默认第一个 view
        if (getChildAt(0) != null && getChildAt(0) is ViewGroup && zoomView == null) {
            val vg = getChildAt(0) as ViewGroup
            if (vg.childCount > 0) {
                zoomView = vg.getChildAt(0)
            }
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (zoomViewWidth <= 0 || zoomViewHeight <= 0) {
            zoomView?.let {
                zoomViewWidth = it.measuredWidth
                zoomViewHeight = it.measuredHeight
            }
        }
        if (zoomView == null || zoomViewWidth <= 0 || zoomViewHeight <= 0) {
            return super.onTouchEvent(ev)
        }

        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!mScaling) {
                    if (scrollY == 0) {
                        y = ev.y // 滑动到顶部时，记录位置
                    } else {
                        return super.onTouchEvent(ev)
                    }
                }
                val distance = ((ev.y - y) * mScaleRatio).toInt()
                if (distance < 0) return super.onTouchEvent(ev) // 若往下滑动
                mScaling = true
                setZoom(distance.toFloat())
                return true
            }
            MotionEvent.ACTION_UP -> {
                mScaling = false
                myCallback?.refresh()
                replyView()
            }
        }
        return super.onTouchEvent(ev)
    }

    /**
     * 放大 view
     */
    private fun setZoom(s: Float) {
        val scaleTimes = (zoomViewWidth + s) / zoomViewWidth.toFloat()
        // 如超过最大放大倍数，直接返回
        if (scaleTimes > mScaleTimes) return

        zoomView?.let {
            val layoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = (zoomViewWidth + s).toInt()
            layoutParams.height = (zoomViewHeight * ((zoomViewWidth + s) / zoomViewWidth)).toInt()
            // 设置控件水平居中
            layoutParams.setMargins(-(layoutParams.width - zoomViewWidth) / 2, 0, 0, 0)
            it.layoutParams = layoutParams
        }
    }

    /**
     * 回弹
     */
    private fun replyView() {
        zoomView?.let {
            val distance = it.measuredWidth - zoomViewWidth
            // 设置动画
            val anim = ObjectAnimator.ofFloat(distance.toFloat(), 0.0F).setDuration((distance * mReplyRatio).toLong())
            anim.addUpdateListener { animation ->
                setZoom(animation.animatedValue as Float)
            }
            anim.start()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollListener?.onScroll(l, t, oldl, oldt)
    }

    private var onScrollListener: OnScrollListener? = null

    fun setOnScrollListener(onScrollListener: OnScrollListener) {
        this.onScrollListener = onScrollListener
    }

    /**
     * 滑动监听接口
     */
    interface OnScrollListener {
        fun onScroll(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
    }

    interface MyCallback {
        fun refresh()
    }

    fun setMyCallback(myCallback: MyCallback) {
        this.myCallback = myCallback
    }
}