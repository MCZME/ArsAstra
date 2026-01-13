#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    
    // 计算边缘侵蚀 (Erosion)
    // V: 0 -> 1, Center: 0.5
    float dist = abs(texCoord0.y - 0.5) * 2.0; // 0 (Center) to 1 (Edge)
    
    // 使用 UV 坐标生成高频噪声，模拟纸张颗粒或笔触边缘
    // 乘数越大，噪点越细
    float noise = random(floor(texCoord0 * vec2(100.0, 20.0))); 
    
    // 动态阈值：开始淡化的位置
    // 范围 [0.4, 0.8]
    float threshold = 0.4 + noise * 0.4;
    
    // 计算淡化强度 (1.0 = 实色, 0.0 = 透明/白色)
    // 使用 smoothstep 进行平滑过渡，产生羽化效果
    // 过渡宽度 0.2
    float alpha = 1.0 - smoothstep(threshold, threshold + 0.2, dist);
    
    // 核心颜色 (纹理 * 顶点色)
    vec3 coreColor = texColor.rgb * vertexColor.rgb * ColorModulator.rgb;
    
    // 最终颜色混合
    // 在正片叠底模式下，白色代表透明。
    // 所以我们根据 alpha 在 coreColor 和 白色 之间插值。
    vec3 finalRGB = mix(vec3(1.0), coreColor, alpha);
    
    fragColor = vec4(finalRGB, 1.0);
}
