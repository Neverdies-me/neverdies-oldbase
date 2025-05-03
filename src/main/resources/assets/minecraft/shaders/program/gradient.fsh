#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

uniform float quality;
uniform float radius;
uniform vec3 color;
uniform float fade;
uniform float fadelimit;
uniform float speed;
uniform float colorDistance;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;

uniform float alpha;

uniform vec2 resolution;
uniform float time;

vec3 wave(vec2 pos) {
    return mix(primaryColor.rgb, secondaryColor.rgb, sin((distance(vec2(0), pos) - time * speed) / colorDistance) * 0.5 + 0.5);
}
float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alphaValue = 0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0) {
                alphaValue += fade > 0 ? max(0.0, (fadelimit - distance(vec2(x, y), vec2(0))) / fade) : 1;
            }
        }
    }

    return alphaValue;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0) {
        vec2 pos = gl_FragCoord.xy;
        fragColor = vec4(wave(pos), alpha);
    } else {
        float alphaOutline = glowShader();
        fragColor = vec4(color.rgb, alphaOutline);
    }
}