#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uState;
uniform vec2 uTexel;
uniform vec2 uResolution;
uniform float uTime;
uniform float uLevel;
uniform float uDepth;
uniform float uMatter;

float heightAt(vec2 uv) {
    vec2 state = texture(uState, uv).rg;
    return smoothstep(0.05, 0.62, state.g - state.r * 0.18);
}

void main() {
    vec2 aspectUv = vUv;
    float screenAspect = uResolution.x / max(uResolution.y, 1.0);
    if (screenAspect > 1.0) {
        aspectUv.x = (aspectUv.x - 0.5) * screenAspect + 0.5;
    } else {
        aspectUv.y = (aspectUv.y - 0.5) / screenAspect + 0.5;
    }

    float h = heightAt(aspectUv);
    float hx = heightAt(aspectUv + vec2(uTexel.x, 0.0)) - heightAt(aspectUv - vec2(uTexel.x, 0.0));
    float hy = heightAt(aspectUv + vec2(0.0, uTexel.y)) - heightAt(aspectUv - vec2(0.0, uTexel.y));
    vec3 normal = normalize(vec3(-hx * (12.0 + uDepth * 42.0), -hy * (12.0 + uDepth * 42.0), 1.0));
    vec3 light = normalize(vec3(-0.45, 0.55, 0.72));
    float diffuse = max(dot(normal, light), 0.0);
    float rim = pow(1.0 - max(normal.z, 0.0), 2.4);

    float dotGrid = 1.0 - smoothstep(0.02, 0.42, length(fract(gl_FragCoord.xy / 2.25) - 0.5));
    float body = smoothstep(0.04, 0.28, h);
    float edge = smoothstep(0.02, 0.18, h) - smoothstep(0.38, 0.76, h);

    vec3 ink = vec3(0.84 + uMatter * 0.12);
    vec3 color = ink * body * (0.12 + diffuse * 0.88);
    color += vec3(0.32) * rim * body;
    color *= mix(0.72, 1.08, dotGrid * (0.35 + edge));
    color += vec3(0.16) * edge * (0.2 + uLevel);

    float vignette = 1.0 - smoothstep(0.28, 0.92, distance(vUv, vec2(0.5)));
    color *= vignette;

    fragColor = vec4(color, 1.0);
}
