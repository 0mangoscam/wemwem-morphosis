#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uState;
uniform vec2 uTexel;
uniform float uTime;
uniform float uLevel;
uniform float uBass;
uniform float uMid;
uniform float uTreble;
uniform float uPeak;
uniform float uMatter;
uniform float uMutation;
uniform float uDecay;
uniform float uMemory;
uniform float uSensitivity;
uniform vec2 uTouch;
uniform float uTouchActive;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float circle(vec2 uv, vec2 center, float radius) {
    return 1.0 - smoothstep(radius, radius + 0.012, distance(uv, center));
}

void main() {
    vec2 c = texture(uState, vUv).rg;
    vec2 n = texture(uState, vUv + vec2(0.0, uTexel.y)).rg;
    vec2 s = texture(uState, vUv - vec2(0.0, uTexel.y)).rg;
    vec2 e = texture(uState, vUv + vec2(uTexel.x, 0.0)).rg;
    vec2 w = texture(uState, vUv - vec2(uTexel.x, 0.0)).rg;
    vec2 ne = texture(uState, vUv + uTexel).rg;
    vec2 nw = texture(uState, vUv + vec2(-uTexel.x, uTexel.y)).rg;
    vec2 se = texture(uState, vUv + vec2(uTexel.x, -uTexel.y)).rg;
    vec2 sw = texture(uState, vUv - uTexel).rg;

    vec2 lap = (n + s + e + w) * 0.20 + (ne + nw + se + sw) * 0.05 - c;

    float energy = smoothstep(0.035, 0.72, uLevel * (0.55 + uSensitivity * 1.25));
    float feed = mix(0.018, 0.044, uMatter) + uBass * 0.009 + uMutation * 0.006;
    float kill = mix(0.071, 0.052, uMatter) - uMid * 0.004 + uTreble * 0.003;
    float reaction = c.r * c.g * c.g;

    float dA = 1.0 * lap.r - reaction + feed * (1.0 - c.r);
    float dB = 0.48 * lap.g + reaction - (kill + feed) * c.g;

    float memoryStep = mix(0.55, 1.0, uMemory);
    vec2 nextState = c + vec2(dA, dB) * memoryStep;

    // Sound creates sparse colonies. Silence cannot create matter.
    vec2 grid = floor(vUv * mix(34.0, 110.0, uTreble));
    float pulse = step(0.988 - uPeak * 0.02, hash21(grid + floor(uTime * 10.0)));
    float movingSeed = circle(
        vUv,
        vec2(
            0.5 + 0.34 * sin(uTime * (0.7 + uBass)),
            0.5 + 0.34 * cos(uTime * (0.9 + uMid))
        ),
        0.018 + uBass * 0.055
    );
    float soundSeed = energy * max(pulse * (0.25 + uTreble), movingSeed * (0.10 + uPeak * 1.8));

    float existingMatter = smoothstep(0.01, 0.22, c.g);
    float touchSeed = circle(vUv, uTouch, 0.03 + uBass * 0.04) * uTouchActive * existingMatter;
    float injection = clamp(soundSeed + touchSeed, 0.0, 1.0);
    nextState.r = mix(nextState.r, 0.20, injection);
    nextState.g = mix(nextState.g, 0.96, injection);

    // In silence, the organism slowly returns to blank A=1, B=0.
    float silence = 1.0 - energy;
    float erosion = silence * mix(0.0004, 0.006, uDecay);
    nextState.r = mix(nextState.r, 1.0, erosion);
    nextState.g = mix(nextState.g, 0.0, erosion * 1.35);

    fragColor = vec4(clamp(nextState, 0.0, 1.0), 0.0, 1.0);
}
