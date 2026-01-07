#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 InkColor;
uniform float UVScale;
uniform vec2 UVOffset;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 worldPos;

out vec4 fragColor;

void main() {
    // 1. Sample Hatching Texture
    // Use worldPos for UVs so the texture stays fixed in screen space ("shower door" effect)
    vec2 hatchUV = (worldPos.xy + UVOffset) * UVScale * 0.01;
    vec4 texColor = texture(Sampler0, hatchUV);
    
    // 2. Extract Ink Density
    float lum = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    float inkDensity = 1.0 - lum;
    inkDensity = smoothstep(0.1, 0.8, inkDensity);
    
    // 3. Apply Colors and Alpha
    // vertexColor.a comes from the Java mesh (1.0 at edge, 0.0 at center)
    // This provides the "Soft SDF" effect via interpolation.
    vec4 finalColor = InkColor;
    finalColor.a *= inkDensity * vertexColor.a * ColorModulator.a;
    
    if (finalColor.a < 0.01) {
        discard;
    }

    fragColor = finalColor;
}