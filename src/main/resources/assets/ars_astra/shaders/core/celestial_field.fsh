#version 150

uniform vec4 EffectColor;
uniform float Time;

in vec4 vertexColor;
in vec2 localPos;

out vec4 fragColor;

// 简单的伪随机哈希
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// 2D 噪声
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

// 分形布朗运动 (FBM) - 用于模拟纸张纤维和墨水扩散
float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p = rot * p * 2.0;
        a *= 0.5;
    }
    return v;
}

void main() {
    // 基础几何
    float dist = length(localPos);
    float angle = atan(localPos.y, localPos.x);
    
    // 全局裁剪
    if (dist > 1.0) discard;

    // 基础噪声场 (Paper Grain)
    float paperGrain = fbm(localPos * 10.0 + 100.0);

    // --- 1. 主圆环 (Hand-drawn Ink Ring) ---
    // 扭曲半径，模拟手抖
    float distortedDist = dist + noise(vec2(angle * 10.0, 0.0)) * 0.015;
    // 基础环位置 0.85
    float ringWidth = 0.02 + paperGrain * 0.01; // 线宽不均匀
    float mainRing = 1.0 - smoothstep(ringWidth, ringWidth + 0.01, abs(distortedDist - 0.85));
    // 墨水深浅变化
    mainRing *= 0.6 + 0.4 * fbm(vec2(angle * 20.0, Time * 0.1));

    // --- 2. 动态刻度 (Rotating Ticks) ---
    // 外圈 0.92 ~ 0.98
    float tickRingDist = abs(dist - 0.95);
    float tickRing = 1.0 - smoothstep(0.02, 0.025, tickRingDist);
    // 刻度切割 (每 10 度一个刻度，缓慢旋转)
    float tickMask = step(0.6, sin(angle * 40.0 + Time * 0.2)); 
    // 偶尔缺损 (Broken ticks)
    float tickBroken = step(0.3, noise(vec2(angle * 5.0, 1.0)));
    float ticks = tickRing * tickMask * tickBroken * 0.5;

    // --- 3. 次级内圈 (Secondary Inner Ring) ---
    // 在 0.55 处画一个细环
    float innerRingDist = abs(dist - 0.55 + noise(vec2(angle * 8.0, 0.0)) * 0.01);
    float innerRing = 1.0 - smoothstep(0.005, 0.015, innerRingDist);
    innerRing *= 0.4 + 0.3 * sin(angle * 10.0 + Time * 0.5); // 动态深浅

    // --- 4. 内部晕染 (Inner Wash) ---
    // 模拟中心墨水沉积，加深强度
    float wash = smoothstep(0.85, 0.0, dist) * 0.35;
    // 叠加噪声，不均匀
    wash *= 0.8 + 0.4 * paperGrain;

    // --- 颜色合成 ---
    // 墨色常量 0x2F2F2F -> (47, 47, 47) / 255
    vec3 inkRGB = vec3(0.1843, 0.1843, 0.1843);
    
    // 外层：主圆环 + 刻度 (统一为墨色)
    float outerAlpha = mainRing + ticks;
    
    // 内层：内部细环 + 中心晕染 (使用药水效果颜色)
    float innerAlpha = (innerRing + wash) * EffectColor.a;
    
    // 合并 Alpha
    float totalAlpha = (outerAlpha + innerAlpha) * vertexColor.a;
    
    // 颜色加权合并
    // 如果没有任何 alpha，rgb 不重要；否则按比例混合
    vec3 finalRGB = mix(EffectColor.rgb, inkRGB, outerAlpha / (outerAlpha + innerAlpha + 0.0001));
    
    fragColor = vec4(finalRGB, totalAlpha);
}
