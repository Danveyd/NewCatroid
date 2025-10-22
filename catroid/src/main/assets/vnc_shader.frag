#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    vec2 flipped_coords = vec2(v_texCoords.x, 1.0 - v_texCoords.y);

    gl_FragColor = texture2D(u_texture, flipped_coords).bgra;
}