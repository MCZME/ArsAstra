#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 InkColor;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    if (texColor.a < 0.1) {
        discard;
    }

    // 计算亮度
    float lum = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));

    // 映射为墨水密度：暗部 -> 浓墨，亮部 -> 纸张
    float inkDensity = 1.0 - pow(lum, 0.8);
    inkDensity = smoothstep(0.2, 0.9, inkDensity);

    vec4 finalColor = InkColor;
    finalColor.a = inkDensity * texColor.a * vertexColor.a * ColorModulator.a;
    
    if (finalColor.a < 0.05) {
        discard;
    }

    fragColor = finalColor;
}
