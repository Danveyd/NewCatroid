#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform float u_sampleScale;

// 9-Tap Bilinear Tent Filter
void main() {
    vec2 d = u_texelSize * u_sampleScale;
    vec2 uv = v_texCoords;

    vec3 color = texture2D(u_texture, uv + vec2(-d.x, -d.y)).rgb * 1.0;
    color += texture2D(u_texture, uv + vec2( 0.0, -d.y)).rgb * 2.0;
    color += texture2D(u_texture, uv + vec2( d.x, -d.y)).rgb * 1.0;

    color += texture2D(u_texture, uv + vec2(-d.x,  0.0)).rgb * 2.0;
    color += texture2D(u_texture, uv).rgb * 4.0;
    color += texture2D(u_texture, uv + vec2( d.x,  0.0)).rgb * 2.0;

    color += texture2D(u_texture, uv + vec2(-d.x,  d.y)).rgb * 1.0;
    color += texture2D(u_texture, uv + vec2( 0.0,  d.y)).rgb * 2.0;
    color += texture2D(u_texture, uv + vec2( d.x,  d.y)).rgb * 1.0;

    gl_FragColor = vec4(color * (1.0 / 16.0), 1.0);
}
