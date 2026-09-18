#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_colorTexture;
uniform sampler2D u_depthTexture;
uniform sampler2D u_materialTexture;

uniform mat4 u_projectionMatrix;
uniform mat4 u_invProjectionMatrix;
uniform mat4 u_viewMatrix;
uniform float u_farPlane;

// SSAO
uniform vec3 u_kernel[16];
uniform float u_aoRadius;
uniform float u_aoIntensity;
uniform float u_aoBias;

// SSGI
uniform float u_giIntensity;
uniform float u_giRadius;

// SSR
uniform float u_reflectivityMulti;
uniform float u_edgeFade;
uniform float u_thickness;
uniform float u_maxDistance;
uniform float u_stride;
uniform int u_maxSteps;

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

float getScreenEdgeFade(vec2 uv) {
vec2 margin = smoothstep(vec2(0.0), vec2(0.18), uv) * smoothstep(vec2(1.0), vec2(0.82), uv);
return margin.x * margin.y;
}

void main() {
    float originDepth = getDepth(v_texCoords);
    if (originDepth >= u_farPlane * 0.98 || originDepth <= 0.05) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 vPos = getViewPos(v_texCoords);
    vec3 vNorm = getNormal(v_texCoords);

    vec3 helper = abs(vNorm.z) < 0.95 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(helper, vNorm));
    vec3 bitangent = cross(vNorm, tangent);
    mat3 TBN = mat3(tangent, bitangent, vNorm);

    float occlusion = 0.0;
    float dynamicBias = max(u_aoBias, originDepth * 0.002);

    for (int i = 0; i < 16; i++) {
        vec3 sampleDir = normalize(TBN * u_kernel[i] + vNorm * 0.35);
        vec3 samplePos = vPos + sampleDir * u_aoRadius;
        vec2 sampleUV = projectToUV(samplePos);

        if (sampleUV.x >= 0.0 && sampleUV.x <= 1.0 && sampleUV.y >= 0.0 && sampleUV.y <= 1.0) {
            float sampleDepth = getDepth(sampleUV);
            float diff = -samplePos.z - sampleDepth;

            if (diff > dynamicBias && diff < u_aoRadius) {
                float rangeCheck = smoothstep(u_aoRadius, dynamicBias, diff);
                occlusion += rangeCheck;
            }
        }
    }
    float ao = clamp(1.0 - (occlusion / 16.0) * u_aoIntensity, 0.1, 1.0);

    vec3 indirectLight = vec3(0.0);
    float totalWeight = 0.001;

    vec3 giDirs[6];
    giDirs[0] = normalize(vNorm + vec3( 0.5,  0.5, 0.0));
    giDirs[1] = normalize(vNorm + vec3(-0.5,  0.5, 0.0));
    giDirs[2] = normalize(vNorm + vec3( 0.5, -0.5, 0.0));
    giDirs[3] = normalize(vNorm + vec3(-0.5, -0.5, 0.0));
    giDirs[4] = normalize(vNorm + vec3( 0.0,  0.7, 0.2));
    giDirs[5] = normalize(vNorm + vec3( 0.0, -0.7, 0.2));

    const int STEPS = 6;
    float stepSize = max(1.5, u_giRadius) / float(STEPS);

    for (int r = 0; r < 6; r++) {
        vec3 rayDir = giDirs[r];
        float NdotL = max(0.0, dot(vNorm, rayDir));

        for (int s = 1; s <= STEPS; s++) {
            vec3 rayP = vPos + rayDir * (float(s) * stepSize);
            vec2 sUV = projectToUV(rayP);

            float edgeFade = getScreenEdgeFade(sUV);
            if (edgeFade <= 0.001) break;

            float sDepth = getDepth(sUV);
            float diff = -rayP.z - sDepth;

            if (diff > 0.04 && diff < (stepSize * 1.6)) {
                vec3 hitNorm = getNormal(sUV);
                vec3 toSurface = normalize(vPos - rayP);
                float NsDotL = max(0.0, dot(hitNorm, toSurface));

                if (NsDotL > 0.1) {
                    float atten = smoothstep(u_giRadius, 0.0, float(s) * stepSize);
                    vec3 col = texture2D(u_colorTexture, sUV).rgb;
                    float w = NdotL * NsDotL * atten * edgeFade;
                    indirectLight += col * w;
                    totalWeight += w;
                }
                break;
            }
        }
    }

    vec3 avgIndirect = indirectLight / totalWeight;
    float coverage = clamp(totalWeight * 2.0, 0.0, 1.0);
    vec3 finalBounce = avgIndirect * coverage * u_giIntensity;

    float matReflectivity = texture2D(u_materialTexture, v_texCoords).r;
    vec3 ssrColor = vec3(0.0);

    vec3 vDir = normalize(vPos);
    vec3 reflDir = normalize(reflect(vDir, vNorm));

    if (matReflectivity > 0.05 && reflDir.z <= 0.0) {
        float currentStep = max(0.15, u_stride);
        vec3 rayPos = vPos + vNorm * 0.1 + reflDir * currentStep;

        for (int i = 0; i < 35; i++) {
            if (i >= u_maxSteps) break;

            rayPos += reflDir * currentStep;
            vec2 hitUV = projectToUV(rayPos);
            if (hitUV.x < 0.0 || hitUV.x > 1.0 || hitUV.y < 0.0 || hitUV.y > 1.0) break;

            float sceneZ = getDepth(hitUV);
            float rayZ = -rayPos.z;

            if (rayZ > sceneZ) {
                float diff = rayZ - sceneZ;

                if (diff < u_thickness) {
                    vec3 hitNorm = getNormal(hitUV);
                    if (dot(hitNorm, -reflDir) > 0.15) {
                        vec3 startP = rayPos - reflDir * currentStep;
                        vec3 endP = rayPos;
                        vec3 midP;

                        for (int j = 0; j < 4; j++) {
                            midP = mix(startP, endP, 0.5);
                            vec2 mUV = projectToUV(midP);
                            if (-midP.z > getDepth(mUV)) endP = midP; else startP = midP;
                        }

                        vec2 finalUV = projectToUV(midP);
                        vec3 hitCol = texture2D(u_colorTexture, finalUV).rgb;

                        float edge = getScreenEdgeFade(finalUV);
                        float distFade = smoothstep(u_maxDistance, u_maxDistance * 0.3, distance(vPos, midP));
                        float fresnel = pow(1.0 - max(dot(vNorm, -vDir), 0.0), 3.0);

                        ssrColor = hitCol * (edge * distFade * fresnel * matReflectivity * u_reflectivityMulti);
                        break;
                    }
                }
            }
        }
    }

    gl_FragColor = vec4(finalBounce + ssrColor, ao);
}
