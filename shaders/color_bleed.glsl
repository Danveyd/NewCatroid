// Example GLSL fragment shader: Color-Bleed
// This is a minimal example and must be adapted to the project's GL setup (varyings, UVs, sampler names).

precision mediump float;

uniform sampler2D u_texture;
uniform float u_intensity; // 0.0 - 1.0

varying vec2 v_texCoord;

void main() {
    vec4 col = texture2D(u_texture, v_texCoord);
    vec3 bleed = vec3(col.r * 0.2, col.g * 0.05, 0.0);
    gl_FragColor = vec4(mix(col.rgb, col.rgb + bleed, u_intensity), col.a);
}
