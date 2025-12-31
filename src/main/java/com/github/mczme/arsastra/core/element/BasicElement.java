package com.github.mczme.arsastra.core.element;

import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

/**
 * 代表一个“基础要素”。
 * 它对应一个固定的方向矢量。
 */
public record BasicElement(Vector2f vector) implements Element {
    @Override
    public StarChartPath getPath(float strength) {
        Vector2f start = new Vector2f(0, 0);
        Vector2f end = new Vector2f(this.vector).mul(strength);
        return new LinearStarChartPath(start, end);
    }
}
