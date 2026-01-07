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
    // 使用世界坐标计算 UV，以实现全局一致的平铺效果
    vec2 hatchUV = (worldPos.xy + UVOffset) * UVScale * 0.01;
    
    // 采样排线纹理
    vec4 texColor = texture(Sampler0, hatchUV);
    
    // 假设纹理是白底黑线
    // 1.0 - lum 得到墨水浓度
    float lum = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    float inkDensity = 1.0 - lum;
    
    // 增强对比度
    inkDensity = smoothstep(0.1, 0.8, inkDensity);
    
    vec4 finalColor = InkColor;
    // 墨水色叠加顶点颜色 Alpha 和 Modulator Alpha
    finalColor.a *= inkDensity * vertexColor.a * ColorModulator.a;
    
    if (finalColor.a < 0.01) {
        discard;
    }

    fragColor = finalColor;
}