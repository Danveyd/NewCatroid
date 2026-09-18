#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_sceneTexture;
uniform sampler2D u_bloomTexture;
uniform sampler2D u_indirectTexture;

uniform float u_bloomIntensity;
uniform float u_exposure;

vec3 ACESFilm(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 sceneRaw = texture2D(u_sceneTexture, v_texCoords);
    vec4 indirect = texture2D(u_indirectTexture, v_texCoords);
    vec3 bloomColor = texture2D(u_bloomTexture, v_texCoords).rgb;

    vec3 sceneLinear = pow(sceneRaw.rgb, vec3(2.2));

    float ao = indirect.a;
    vec3 bounceAndSSR = indirect.rgb;

    vec3 color = sceneLinear * ao + bounceAndSSR;
    color += bloomColor * u_bloomIntensity;

    color *= u_exposure;
    color = ACESFilm(color);

    color = pow(color, vec3(1.0 / 2.2));

    gl_FragColor = vec4(color, sceneRaw.a);
}
