#version 150

uniform vec4 InkColor;
uniform float Time;
uniform vec2 UVOffset;

in vec4 vertexColor;
in vec2 worldPos;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    // 使用位置和偏移量计算采样点
    vec2 p = (worldPos + UVOffset) * 0.01;
    
    // 动态分量
    float t = Time * 0.05;
    
    // 计算 FBM 噪声
    float n = fbm(p + vec2(t, t * 0.5));
    float n2 = fbm(p * 1.5 - vec2(t * 0.2, t));
    
    // 混合两层噪声，产生复杂质感
    float intensity = mix(n, n2, 0.5);
    
    // 增强对比度，产生云絮状边缘
    intensity = smoothstep(0.2, 0.8, intensity);
    
    // Alpha 控制
    float alpha = mix(0.1, 0.45, intensity);
    
    fragColor = vec4(InkColor.rgb, InkColor.a * alpha * vertexColor.a);
}
