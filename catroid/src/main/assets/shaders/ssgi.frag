#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform sampler2D u_depthTexture;

uniform mat4 u_projectionMatrix;
uniform mat4 u_invProjectionMatrix;
uniform mat4 u_viewMatrix;
uniform float u_farPlane;

uniform float u_radius;
uniform float u_intensity;
uniform float u_bias;
uniform vec3 u_baseAlbedo;

float getScreenEdgeFade(vec2 uv) {
vec2 margin = smoothstep(vec2(0.0), vec2(0.12), uv) * smoothstep(vec2(1.0), vec2(0.88), uv);
return margin.x * margin.y;
}

float getDepth(vec2 uv) {
vec2 data = texture2D(u_depthTexture, uv).rg;
return (data.x + data.y / 255.0) * u_farPlane;
}

vec3 getNormal(vec2 uv) {
vec2 p = texture2D(u_depthTexture, uv).ba * 2.0 - 1.0;
vec3 n = vec3(p.x, p.y, 1.0 - abs(p.x) - abs(p.y));
float t = clamp(-n.z, 0.0, 1.0);
n.x += (n.x >= 0.0) ? -t : t;
n.y += (n.y >= 0.0) ? -t : t;
return normalize(mat3(u_viewMatrix) * n);
}

vec3 getViewPos(vec2 uv) {
float z = getDepth(uv);
vec2 ndc = uv * 2.0 - 1.0;

float invProjX = 1.0 / u_projectionMatrix[0][0];
float invProjY = 1.0 / u_projectionMatrix[1][1];

return vec3(ndc.x * invProjX * z, ndc.y * invProjY * z, -z);
}

vec2 projectToUV(vec3 viewPos) {
vec2 ndc;
ndc.x = (viewPos.x * u_projectionMatrix[0][0]) / (-viewPos.z);
ndc.y = (viewPos.y * u_projectionMatrix[1][1]) / (-viewPos.z);
return ndc * 0.5 + 0.5;
}

void main() {
    vec4 baseColor = texture2D(u_texture0, v_texCoords);
    float originDepth = getDepth(v_texCoords);

    if (originDepth >= u_farPlane * 0.95) {
        gl_FragColor = baseColor;
        return;
    }

    vec3 vPos = getViewPos(v_texCoords);
    vec3 vNorm = getNormal(v_texCoords);

    vec3 helper = abs(vNorm.z) < 0.99 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(helper, vNorm));
    vec3 bitangent = cross(vNorm, tangent);
    mat3 TBN = mat3(tangent, bitangent, vNorm);

    vec3 rayDirs[8];
    rayDirs[0] = normalize(TBN * vec3( 0.0,  0.6, 0.8));
    rayDirs[1] = normalize(TBN * vec3( 0.6,  0.0, 0.8));
    rayDirs[2] = normalize(TBN * vec3( 0.0, -0.6, 0.8));
    rayDirs[3] = normalize(TBN * vec3(-0.6,  0.0, 0.8));
    rayDirs[4] = normalize(TBN * vec3( 0.4,  0.4, 0.82));
    rayDirs[5] = normalize(TBN * vec3(-0.4,  0.4, 0.82));
    rayDirs[6] = normalize(TBN * vec3(-0.4, -0.4, 0.82));
    rayDirs[7] = normalize(TBN * vec3( 0.4, -0.4, 0.82));

    vec3 indirectLight = vec3(0.0);
    float totalWeight = 0.001;

    const int STEPS = 6;
    float maxDist3D = max(2.0, u_radius);
    float stepSize = maxDist3D / float(STEPS);

    for (int r = 0; r < 8; r++) {
        vec3 rayDir3D = rayDirs[r];
        float NdotL = max(0.0, dot(vNorm, rayDir3D));

        for (int s = 1; s <= STEPS; s++) {
            float rayDist = float(s) * stepSize;
            vec3 rayViewPos = vPos + rayDir3D * rayDist;

            vec2 sampleUV = projectToUV(rayViewPos);

            float edgeFade = getScreenEdgeFade(sampleUV);
            if (edgeFade <= 0.001) break;

            float sampleDepth = getDepth(sampleUV);
            if (sampleDepth >= u_farPlane * 0.95) continue;

            float rayDepth = -rayViewPos.z;
            float depthDiff = rayDepth - sampleDepth;

            if (depthDiff > 0.02) {
                if (depthDiff < 0.8) {
                    vec3 sampleNormGI = getNormal(sampleUV);
                    vec3 actualDir3D = rayViewPos - vPos;
                    float actualDist = length(actualDir3D);
                    if (actualDist < 0.05) break;

                    vec3 L_norm = actualDir3D / actualDist;
                    float NsDotL = max(0.0, dot(sampleNormGI, -L_norm));

                    if (NsDotL > 0.01) {
                        float atten = smoothstep(maxDist3D, 0.0, actualDist);
                        float weight = NdotL * NsDotL * atten * edgeFade;

                        vec3 sampleColor = texture2D(u_texture0, sampleUV).rgb;

                        indirectLight += sampleColor * weight;
                        totalWeight += weight;
                    }

                    break;
                }
            }
        }
    }

    vec3 avgIndirect = indirectLight / totalWeight;
    float coverage = clamp(totalWeight * 1.5, 0.0, 1.0);

    float indirectLum = dot(avgIndirect, vec3(0.299, 0.587, 0.114));
    vec3 compressedIndirect = avgIndirect / (1.0 + indirectLum);

    vec3 finalBounce = baseColor.rgb * compressedIndirect * coverage * u_intensity * 2.0;

    vec3 finalColor = baseColor.rgb + finalBounce;

    gl_FragColor = vec4(finalColor, baseColor.a);
}
