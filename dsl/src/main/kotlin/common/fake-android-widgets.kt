@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate")

package common

import android.content.Context

open class View(val context: Context?) {
    var backgroundColor_: Int = 0

    fun setBackgroundColor(i: Int) {
        println("LOG: ${javaClass.simpleName}.setBackgroundColor($i)")
        backgroundColor_ = i
    }
}

open class ViewGroup(context: Context?) : View(context) {

    val children = ArrayList<View>()

    fun getChildAt(index: Int): View = children[index]

    fun removeViewAt(i: Int) {
        println("LOG: ${javaClass.simpleName}.removeViewAt($i)")
        children.removeAt(i)
    }

    fun addView(v: View) {
        println("LOG: ${javaClass.simpleName}.addView(::${v.javaClass.simpleName})")
        children.add(v)
    }

    fun addView(v: View, i: Int) {
        println("LOG: ${javaClass.simpleName}.addView(::${v.javaClass.simpleName}, $i)")
        children.add(i, v)
    }
}

class LinearLayout(context: Context?) : ViewGroup(context) {
    val `@class` = "LinearLayout"
}

class TextView(context: Context?) : View(context) {

    val `@class` = "TextView"
    var textColor_: Int = 0
    var text_: String = ""
    var textSize_ = 0f

    fun setTextColor(color: Int) {
        println("LOG: ${javaClass.simpleName}.setTextColor($color)")
        textColor_ = color
    }

    fun setText(s: String) {
        println("LOG: ${javaClass.simpleName}.setText(\"$s\")")
        text_ = s
    }

    fun setTextSize(size: Float) {
        println("LOG: ${javaClass.simpleName}.setTextSize($size)")
        textSize_ = size
    }
}
