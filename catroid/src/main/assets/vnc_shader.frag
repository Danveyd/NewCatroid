#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    // Создаем новые координаты, инвертируя Y
    // v_texCoords.y идет от 0 (низ) до 1 (верх).
    // 1.0 - v_texCoords.y сделает так, что 0 станет 1, а 1 станет 0.
    vec2 flipped_coords = vec2(v_texCoords.x, 1.0 - v_texCoords.y);

    // Используем перевернутые координаты для чтения текстуры
    gl_FragColor = texture2D(u_texture, flipped_coords).bgra;
}