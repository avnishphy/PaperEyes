package android.graphics
class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
 fun width() = right - left
 fun height() = bottom - top
}
