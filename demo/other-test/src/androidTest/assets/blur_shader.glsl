// 顶点着色器
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;

void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}

// 片段着色器 - 高斯模糊
precision mediump float;
uniform sampler2D uTexture;
uniform vec2 uTextureSize;
uniform float uBlurRadius;
varying vec2 vTexCoord;

void main() {
    vec4 color = vec4(0.0);
    float total = 0.0;
    
    // 高斯核权重
    float weights[5];
    weights[0] = 0.2270270270;
    weights[1] = 0.1945945946;
    weights[2] = 0.1216216216;
    weights[3] = 0.0540540541;
    weights[4] = 0.0162162162;
    
    // 水平模糊
    for (int i = -4; i <= 4; i++) {
        float weight = weights[abs(i)];
        vec2 offset = vec2(float(i) * uBlurRadius / uTextureSize.x, 0.0);
        color += texture2D(uTexture, vTexCoord + offset) * weight;
        total += weight;
    }
    
    gl_FragColor = color / total;
}