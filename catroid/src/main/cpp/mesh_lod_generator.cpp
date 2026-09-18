#include <jni.h>
#include <vector>
#include <cmath>
#include <algorithm>
#include <cstdint>

struct Vec3 {
    float x, y, z;
    Vec3() : x(0), y(0), z(0) {}
    Vec3(float _x, float _y, float _z) : x(_x), y(_y), z(_z) {}
    float dot(const Vec3& b) const { return x * b.x + y * b.y + z * b.z; }
};

extern "C" JNIEXPORT jshortArray JNICALL
Java_org_catrobat_catroid_raptor_lod_NativeLODGenerator_nativeSimplifyMesh(
        JNIEnv* env, jclass clazz,
        jfloatArray vertices_j, jint vertexStrideFloats, jint normalOffsetFloats,
        jshortArray indices_j, jfloat targetRatio) {

    if (!vertices_j || !indices_j) return nullptr;

    jsize vertexFloatCount = env->GetArrayLength(vertices_j);
    jsize indexCount = env->GetArrayLength(indices_j);

    if (vertexStrideFloats <= 0 || vertexFloatCount == 0 || indexCount == 0 || targetRatio >= 0.98f) {
        return indices_j;
    }

    int vertexCount = vertexFloatCount / vertexStrideFloats;
    jfloat* vData = env->GetFloatArrayElements(vertices_j, nullptr);
    jshort* iData = env->GetShortArrayElements(indices_j, nullptr);

    Vec3 minB(1e9f, 1e9f, 1e9f), maxB(-1e9f, -1e9f, -1e9f);
    for (int i = 0; i < vertexCount; ++i) {
        float x = vData[i * vertexStrideFloats];
        float y = vData[i * vertexStrideFloats + 1];
        float z = vData[i * vertexStrideFloats + 2];
        minB.x = std::min(minB.x, x); maxB.x = std::max(maxB.x, x);
        minB.y = std::min(minB.y, y); maxB.y = std::max(maxB.y, y);
        minB.z = std::min(minB.z, z); maxB.z = std::max(maxB.z, z);
    }

    Vec3 size(std::max(0.001f, maxB.x - minB.x),
              std::max(0.001f, maxB.y - minB.y),
              std::max(0.001f, maxB.z - minB.z));

    int gridRes = 64;

    std::vector<int> remap(vertexCount);
    std::vector<int> grid(gridRes * gridRes * gridRes, -1);

    for (int i = 0; i < vertexCount; ++i) {
        float x = (vData[i * vertexStrideFloats] - minB.x) / size.x;
        float y = (vData[i * vertexStrideFloats + 1] - minB.y) / size.y;
        float z = (vData[i * vertexStrideFloats + 2] - minB.z) / size.z;

        int gx = std::min(gridRes - 1, std::max(0, static_cast<int>(x * gridRes)));
        int gy = std::min(gridRes - 1, std::max(0, static_cast<int>(y * gridRes)));
        int gz = std::min(gridRes - 1, std::max(0, static_cast<int>(z * gridRes)));
        int cell = gx + gy * gridRes + gz * gridRes * gridRes;

        int rep = grid[cell];

        if (rep == -1) {
            grid[cell] = i;
            remap[i] = i;
        } else {
            bool canMerge = true;
            if (normalOffsetFloats >= 0) {
                Vec3 normRep(vData[rep * vertexStrideFloats + normalOffsetFloats],
                             vData[rep * vertexStrideFloats + normalOffsetFloats + 1],
                             vData[rep * vertexStrideFloats + normalOffsetFloats + 2]);

                Vec3 normCur(vData[i * vertexStrideFloats + normalOffsetFloats],
                             vData[i * vertexStrideFloats + normalOffsetFloats + 1],
                             vData[i * vertexStrideFloats + normalOffsetFloats + 2]);

                if (normRep.dot(normCur) < 0.70f) {
                    canMerge = false;
                }
            }

            if (canMerge) {
                remap[i] = rep;
            } else {
                remap[i] = i;
            }
        }
    }

    std::vector<jshort> newIndices;
    newIndices.reserve(indexCount);

    for (int i = 0; i < indexCount; i += 3) {
        int i0 = remap[static_cast<uint16_t>(iData[i])];
        int i1 = remap[static_cast<uint16_t>(iData[i + 1])];
        int i2 = remap[static_cast<uint16_t>(iData[i + 2])];

        if (i0 != i1 && i1 != i2 && i0 != i2) {
            newIndices.push_back(static_cast<jshort>(i0));
            newIndices.push_back(static_cast<jshort>(i1));
            newIndices.push_back(static_cast<jshort>(i2));
        }
    }

    env->ReleaseFloatArrayElements(vertices_j, vData, JNI_ABORT);
    env->ReleaseShortArrayElements(indices_j, iData, JNI_ABORT);

    if (newIndices.size() < 12) {
        return indices_j;
    }

    jshortArray result = env->NewShortArray(newIndices.size());
    env->SetShortArrayRegion(result, 0, newIndices.size(), newIndices.data());
    return result;
}
