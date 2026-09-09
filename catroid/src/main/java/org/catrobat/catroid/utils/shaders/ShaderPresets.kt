package org.catrobat.catroid.utils.shaders

object ShaderPresets {

    const val COMMON_VERTEX_SHADER = """
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        
        varying vec2 v_texCoords;
        varying vec4 v_color;
        
        uniform mat4 u_projTrans;
        
        void main() {
            v_color = a_color;
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
    """

    // Вспомогательный хелпер смешивания маски для сохранения Z-слоев
    private const val MASK_HEADER = """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec2 v_texCoords;
        varying vec4 v_color;
        uniform sampler2D u_texture;
        uniform sampler2D u_maskTexture; // Юнит 1 (Маска исключенных объектов)
        uniform int u_hasMask;           // 1 - включено, 0 - нет
    """

    private const val MASK_FOOTER = """
            if (u_hasMask == 1) {
                float isExcluded = texture2D(u_maskTexture, v_texCoords).r;
                vec4 origColor = texture2D(u_texture, v_texCoords);
                finalColor = mix(finalColor, origColor, isExcluded);
            }
            gl_FragColor = v_color * finalColor;
        }
    """

    // 1. SHOCKWAVE (Волна управляется через u_radius, без авто-сброса)
    val SHOCKWAVE_FRAGMENT = MASK_HEADER + """
        uniform vec2 u_center;        
        uniform float u_radius;       
        uniform float u_thickness;    
        uniform float u_strength;     
        uniform int u_invert;         
        uniform float u_aspectRatio;  

        void main() {
            vec2 uv = v_texCoords;
            vec2 aspectUV = vec2((uv.x - u_center.x) * u_aspectRatio + u_center.x, uv.y);
            
            float distance = distance(aspectUV, u_center);

            if (abs(distance - u_radius) <= u_thickness) {
                float diff = distance - u_radius;
                float powDiff = 1.0 - pow(abs(diff / max(u_thickness, 0.001)), 0.8);
                float diffTime = diff * powDiff;
                
                vec2 diffUV = normalize(aspectUV - u_center);
                if (u_invert == 1) {
                    diffUV = -diffUV;
                }
                uv += diffUV * (diffTime * u_strength);
            }
            uv = clamp(uv, 0.0, 1.0);
            vec4 finalColor = texture2D(u_texture, uv);
    """ + MASK_FOOTER

    // 2. GLITCH
    val GLITCH_FRAGMENT = MASK_HEADER + """
        uniform float u_time;
        uniform float u_speed;
        uniform float u_strength;
        uniform float u_sliceHeight;
        uniform float u_maxSliceXOff;
        uniform float u_rgbOff;

        float rand(vec2 co) {
            return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
        }

        void main() {
            vec2 uv = v_texCoords;
            float stepTime = floor(u_time * max(u_speed, 0.1) * 12.0);
            float slice = floor(uv.y / max(u_sliceHeight, 0.001));
            float noise = rand(vec2(slice, stepTime));

            if (noise < u_strength) {
                float xOffset = (rand(vec2(slice, stepTime + 1.0)) - 0.5) * u_maxSliceXOff;
                uv.x += xOffset;
            }

            uv = clamp(uv, 0.0, 1.0);
            float r = texture2D(u_texture, clamp(uv + vec2(u_rgbOff * u_strength, 0.0), 0.0, 1.0)).r;
            float g = texture2D(u_texture, uv).g;
            float b = texture2D(u_texture, clamp(uv - vec2(u_rgbOff * u_strength, 0.0), 0.0, 1.0)).b;
            float a = texture2D(u_texture, uv).a;

            vec4 finalColor = vec4(r, g, b, a);
    """ + MASK_FOOTER

    // 3. BULGE / LENS
    val BULGE_FRAGMENT = MASK_HEADER + """
        uniform vec2 u_center;
        uniform float u_radius;
        uniform float u_strength;     
        uniform float u_aspectRatio;

        void main() {
            vec2 uv = v_texCoords;
            vec2 aspectUV = vec2((uv.x - u_center.x) * u_aspectRatio + u_center.x, uv.y);
            
            float dist = distance(aspectUV, u_center);
            
            if (dist < u_radius) {
                float percent = dist / max(u_radius, 0.001);
                if (u_strength >= 0.0) {
                    percent = pow(percent, 1.0 + u_strength * 2.0);
                } else {
                    percent = pow(percent, 1.0 / (1.0 - u_strength * 2.0));
                }
                vec2 dir = normalize(aspectUV - u_center);
                vec2 newAspectUV = u_center + dir * percent * u_radius;
                uv = vec2((newAspectUV.x - u_center.x) / max(u_aspectRatio, 0.001) + u_center.x, newAspectUV.y);
            }
            
            uv = clamp(uv, 0.0, 1.0);
            vec4 finalColor = texture2D(u_texture, uv);
    """ + MASK_FOOTER

    // 4. BLOOM
    val BLOOM_FRAGMENT = MASK_HEADER + """
        uniform float u_threshold; 
        uniform float u_intensity; 

        void main() {
            vec4 color = texture2D(u_texture, v_texCoords);
            vec3 bloomSum = vec3(0.0);
            float spread = 0.003 * u_intensity;

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    vec2 off = vec2(float(x), float(y)) * spread;
                    vec4 smp = texture2D(u_texture, clamp(v_texCoords + off, 0.0, 1.0));
                    float bright = dot(smp.rgb, vec3(0.2126, 0.7152, 0.0722));
                    if (bright > u_threshold) {
                        bloomSum += (smp.rgb - vec3(u_threshold)) * (bright - u_threshold);
                    }
                }
            }

            vec3 finalBloom = (bloomSum / 9.0) * u_intensity * 3.0;
            vec4 finalColor = vec4(color.rgb + finalBloom, color.a);
    """ + MASK_FOOTER

    // 5. 2D FOG
    val VOLUMETRIC_FOG_2D_FRAGMENT = MASK_HEADER + """
        uniform float u_time;
        uniform float u_speed;
        uniform float u_density;
        uniform vec4 u_fogColor;
        uniform float u_scale;

        float hash(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                       mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
        }

        void main() {
            vec4 sceneColor = texture2D(u_texture, v_texCoords);
            
            vec2 fogUV = v_texCoords * u_scale + vec2(u_time * u_speed, u_time * u_speed * 0.5);
            float n = noise(fogUV) * 0.6 + noise(fogUV * 2.0) * 0.4;
            
            float fogAlpha = clamp(n * u_density, 0.0, 1.0);
            vec3 mixedRGB = mix(sceneColor.rgb, u_fogColor.rgb, fogAlpha * u_fogColor.a);
            vec4 finalColor = vec4(mixedRGB, sceneColor.a);
    """ + MASK_FOOTER

    // 6. PIXELATE
    val PIXELATE_FRAGMENT = MASK_HEADER + """
        uniform vec2 u_pixelSize; 

        void main() {
            vec2 px = max(u_pixelSize, vec2(0.0001));
            vec2 uv = floor(v_texCoords / px) * px;
            uv = clamp(uv, 0.0, 1.0);
            vec4 finalColor = texture2D(u_texture, uv);
    """ + MASK_FOOTER

    // 7. COLOR EDIT
    val COLOR_EDIT_FRAGMENT = MASK_HEADER + """
        uniform float u_grayscale; 
        uniform float u_sepia;     
        uniform float u_hue;       
        uniform vec3 u_colorTint;  

        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        void main() {
            vec4 color = texture2D(u_texture, v_texCoords);
            vec3 rgb = color.rgb;

            if (u_grayscale > 0.0) {
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(rgb, vec3(gray), u_grayscale);
            }

            if (u_sepia > 0.0) {
                vec3 sepiaColor = vec3(
                    dot(rgb, vec3(0.393, 0.769, 0.189)),
                    dot(rgb, vec3(0.349, 0.686, 0.168)),
                    dot(rgb, vec3(0.272, 0.534, 0.131))
                );
                rgb = mix(rgb, sepiaColor, u_sepia);
            }

            if (u_hue != 0.0) {
                vec3 hsv = rgb2hsv(rgb);
                hsv.x = fract(hsv.x + u_hue / 360.0);
                rgb = hsv2rgb(hsv);
            }

            rgb *= u_colorTint;
            vec4 finalColor = vec4(rgb, color.a);
    """ + MASK_FOOTER

    // 8. VIGNETTE
    val VIGNETTE_FRAGMENT = MASK_HEADER + """
        uniform float u_radius;
        uniform float u_softness;

        void main() {
            vec4 color = texture2D(u_texture, v_texCoords);
            vec2 position = v_texCoords - vec2(0.5);
            float len = length(position);
            float vignette = smoothstep(u_radius, u_radius - u_softness, len);
            vec4 finalColor = vec4(color.rgb * vignette, color.a);
    """ + MASK_FOOTER

    // 9. CRT SCANLINES
    val CRT_FRAGMENT = MASK_HEADER + """
        uniform float u_time;
        uniform float u_linesCount;

        void main() {
            vec2 uv = v_texCoords;
            vec4 color = texture2D(u_texture, uv);
            float scanline = sin((uv.y + u_time * 0.05) * max(u_linesCount, 100.0)) * 0.15;
            color.rgb -= scanline;
            vec4 finalColor = color;
    """ + MASK_FOOTER

    // 10. RADIAL BLUR
    val RADIAL_BLUR_FRAGMENT = MASK_HEADER + """
        uniform vec2 u_center;
        uniform float u_strength;

        void main() {
            vec2 uv = v_texCoords;
            vec2 dir = u_center - uv;
            vec4 sum = texture2D(u_texture, uv);
            
            float factor = u_strength * 0.02;
            sum += texture2D(u_texture, uv + dir * factor * 1.0);
            sum += texture2D(u_texture, uv + dir * factor * 2.0);
            sum += texture2D(u_texture, uv + dir * factor * 3.0);
            sum += texture2D(u_texture, uv + dir * factor * 4.0);
            
            vec4 finalColor = (sum / 5.0);
    """ + MASK_FOOTER
}
