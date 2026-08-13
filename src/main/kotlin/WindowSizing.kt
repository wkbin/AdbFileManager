import kotlin.math.roundToInt

internal data class InitialWindowSize(val width: Int, val height: Int)

internal fun calculateInitialWindowSize(
    availableWidth: Int,
    availableHeight: Int
): InitialWindowSize {
    require(availableWidth > 0 && availableHeight > 0)

    val maximumWidth = (availableWidth * 0.94).roundToInt()
    val maximumHeight = (availableHeight * 0.94).roundToInt()
    val preferredWidth = (availableWidth * 0.82).roundToInt()
    val preferredHeight = (availableHeight * 0.84).roundToInt()

    return InitialWindowSize(
        width = preferredWidth.coerceIn(minOf(960, maximumWidth), maximumWidth),
        height = preferredHeight.coerceIn(minOf(640, maximumHeight), maximumHeight)
    )
}
