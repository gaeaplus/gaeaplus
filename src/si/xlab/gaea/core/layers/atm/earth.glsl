/**
 * Precomputed Atmospheric Scattering
 * Copyright (c) 2008 INRIA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Author: Eric Bruneton
 */

#include "common.glsl"

const float ISun = 100.0;

uniform vec3 c;
uniform vec3 s;
uniform mat4 projInverse;
uniform mat4 viewInverse;
uniform float exposure;

uniform sampler2D reflectanceSampler;//ground reflectance texture
uniform sampler2D irradianceSampler;//precomputed skylight irradiance (E table)
uniform sampler3D inscatterSampler;//precomputed inscattered light (S table)

varying vec2 coords;
varying vec3 ray;

#ifdef _VERTEX_

void main() {
    coords = gl_Vertex.xy * 0.5 + 0.5;
	vec4 vertex = gl_Vertex;
	vertex.x = -vertex.x;
	vertex.z = -vertex.z;
	vec4 eyeVertex = projInverse * vertex;
    ray = vec3(-(viewInverse * vec4(eyeVertex.xyz, 0.0)).xyz);
    gl_Position = gl_Vertex;
}

#else

//inscattered light along ray x+tv, when sun in direction s (=S[L])
vec3 inscatter(inout vec3 x, vec3 v, vec3 s, out float r, out float mu) {
    vec3 result;
    r = length(x);
    mu = dot(x, v) / r;
    float d = -r * mu - sqrt(r * r * (mu * mu - 1.0) + Rt * Rt);
    if (d > 0.0) { // if x in space and ray intersects atmosphere
        // move x to nearest intersection of ray with top atmosphere boundary
        x += d * v;
        mu = (r * mu + d) / Rt;
        r = Rt;
    }
    if (r <= Rt) { // if ray intersects atmosphere
        float nu = dot(v, s);
        float muS = dot(x, s) / r;
        float phaseR = phaseFunctionR(nu);
        float phaseM = phaseFunctionM(nu);
        vec4 inscatter = max(texture4D(inscatterSampler, r, mu, muS, nu), 0.0);
        result = max(inscatter.rgb * phaseR + getMie(inscatter) * phaseM, 0.0);
    } else { // x in space and ray looking in space
        result = vec3(0.0);
    }
    return result * ISun;
}

//ground radiance at end of ray x+tv, when sun in direction s
//attenuated bewteen ground and viewer (=R[L0]+R[L*])
vec3 groundColor(vec3 x, vec3 v, vec3 s, float r, float mu)
{
    vec3 result;
    float d = -r * mu - sqrt(r * r * (mu * mu - 1.0) + Rg * Rg);
    if (d > 0.0) { // if ray hits ground surface
        // ground reflectance at end of ray, x0
        vec3 x0 = x + d * v;
        vec3 n = normalize(x0);
        vec2 coords = vec2(atan(n.y, n.x), acos(n.z)) * vec2(0.5, 1.0) / M_PI + vec2(0.5, 0.0);
        //vec2 coords = vec2(atan(n.z, n.x), acos(n.y)) * vec2(0.5, 1.0) / M_PI + vec2(0.5, 0.0);
        vec4 reflectance = texture2D(reflectanceSampler, coords) * vec4(0.2, 0.2, 0.2, 1.0);

        // direct sun light (radiance) reaching x0
        float muS = dot(n, s);
        vec3 sunLight = transmittanceWithShadow(Rg, muS);

        // precomputed sky light (irradiance) (=E[L*]) at x0
        vec3 groundSkyLight = irradiance(irradianceSampler, Rg, muS);

        // light reflected at x0 (=(R[L0]+R[L*])/T(x,x0))
        vec3 groundColor = reflectance.rgb * (max(muS, 0.0) * sunLight + groundSkyLight) * ISun / M_PI;
		groundColor = vec3(0,0,0);

        // attenuation of light to the viewer, T(x,x0)
        vec3 attenuation = transmittance(r, mu, v, x0);

        // water specular color due to sunLight
        if (reflectance.w > 0.0) {
            vec3 h = normalize(s - v);
            float fresnel = 0.02 + 0.98 * pow(1.0 - dot(-v, h), 5.0);
            float waterBrdf = fresnel * pow(max(dot(h, n), 0.0), 150.0);
            groundColor += reflectance.w * max(waterBrdf, 0.0) * sunLight * ISun;
        }

        result = attenuation * groundColor; //=R[L0]+R[L*]
    } else { // ray looking at the sky
        result = vec3(0.0);
    }
    return result;
}

// direct sun light for ray x+tv, when sun in direction s (=L0)
vec3 sunColor(vec3 x, vec3 v, vec3 s, float r, float mu) {
    vec3 transmittance = r <= Rt ? transmittanceWithShadow(r, mu) : vec3(1.0); // T(x,xo)
    float isun = step(cos(M_PI / 180.0), dot(v, s)) * ISun; // Lsun
    return transmittance * isun; // Eq (9)
}

vec3 HDR(vec3 L) {
    L = L * exposure;
    L.r = L.r < 1.413 ? pow(L.r * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.r);
    L.g = L.g < 1.413 ? pow(L.g * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.g);
    L.b = L.b < 1.413 ? pow(L.b * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.b);
    return L;
}

void main() {
    vec3 x = c;
    vec3 v = normalize(ray);
    float r, mu;
	vec3 inscatterColor = inscatter(x, v, s, r, mu); //S[L]-T(x,xs)S[l]|xs = S[L] for spherical ground
    vec3 groundColor = groundColor(x, v, s, r, mu); //R[L0]+R[L*]
    vec3 sunColor = sunColor(x, v, s, r, mu); //L0
    //gl_FragColor = vec4(HDR(sunColor + groundColor + inscatterColor), 0.5); // Eq (16)
    gl_FragColor = vec4(HDR(groundColor + inscatterColor), 0.5); // Eq (16)

    //gl_FragColor = texture3D(inscatterSampler,vec3(coords,(s.x+1.0)/2.0));
    //gl_FragColor = vec4(texture2D(irradianceSampler,coords).rgb*5.0, 1.0);
    //gl_FragColor = texture2D(transmittanceSampler,coords);
}

#endif
