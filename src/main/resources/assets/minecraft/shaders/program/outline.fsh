#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

uniform float quality;
uniform float radius;
uniform float fade;
uniform float fadelimit;
uniform vec3 color;
uniform vec3 line;
uniform float alpha;

uniform vec2 resolution;

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alphaValue = 0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
            alphaValue += fade > 0 ? max(0.0, (fadelimit - distance(vec2(x, y), vec2(0))) / fade) : 1;
        }
    }

    return alphaValue;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0) {
        fragColor = vec4(color, alpha);
    } else {
        fragColor = vec4(line, glowShader());
    }
}
