#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_threshold;

// Jimenez 13-Tap Pattern
void main() {
    vec2 uv = v_texCoords;
    vec2 d = u_texelSize;

    vec3 a = texture2D(u_texture, uv + vec2(-2.0*d.x, -2.0*d.y)).rgb;
    vec3 b = texture2D(u_texture, uv + vec2( 0.0,     -2.0*d.y)).rgb;
    vec3 c = texture2D(u_texture, uv + vec2( 2.0*d.x, -2.0*d.y)).rgb;

    vec3 d_col = texture2D(u_texture, uv + vec2(-d.x, -d.y)).rgb;
    vec3 e = texture2D(u_texture, uv + vec2( d.x, -d.y)).rgb;

    vec3 f = texture2D(u_texture, uv + vec2(-2.0*d.x, 0.0)).rgb;
    vec3 g = texture2D(u_texture, uv).rgb;
    vec3 h = texture2D(u_texture, uv + vec2( 2.0*d.x, 0.0)).rgb;

    vec3 i = texture2D(u_texture, uv + vec2(-d.x, d.y)).rgb;
    vec3 j = texture2D(u_texture, uv + vec2( d.x, d.y)).rgb;

    vec3 k = texture2D(u_texture, uv + vec2(-2.0*d.x, 2.0*d.y)).rgb;
    vec3 l = texture2D(u_texture, uv + vec2( 0.0,     2.0*d.y)).rgb;
    vec3 m = texture2D(u_texture, uv + vec2( 2.0*d.x, 2.0*d.y)).rgb;

    vec3 color = e * 0.125 + d_col * 0.125 + i * 0.125 + j * 0.125;
    color += (a + c + k + m) * 0.03125;
    color += (b + f + h + l) * 0.0625;
    color += g * 0.125;

    float luma = max(color.r, max(color.g, color.b));
    float knee = u_threshold * 0.5;
    float soft = clamp(luma - u_threshold + knee, 0.0, 2.0 * knee);
    soft = (soft * soft) / (4.0 * knee + 0.0001);
    float contribution = max(soft, luma - u_threshold) / max(luma, 0.0001);

    color = clamp(color, vec3(0.0), vec3(8.0));
    gl_FragColor = vec4(color * max(0.0, contribution), 1.0);

}
