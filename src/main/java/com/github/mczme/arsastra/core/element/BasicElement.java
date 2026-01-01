package com.github.mczme.arsastra.core.element;

import com.github.mczme.arsastra.core.starchart.path.LinearStarChartPath;
import com.github.mczme.arsastra.core.starchart.path.StarChartPath;
import org.joml.Vector2f;

public class BasicElement implements Element {
    private final Vector2f vector;

    public BasicElement(Vector2f vector) {
        this.vector = vector;
    }

    @Override
    public StarChartPath getPath(float strength) {
        return new LinearStarChartPath(new Vector2f(vector), new Vector2f(vector).mul(strength));
    }

    @Override
    public Vector2f getVector() {
        return vector;
    }
}
