/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.util;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class Helper {

    public static Identifier identifier(String name) {
        return Identifier.of(ConnectibleChains.MODID, name);
    }

    @Deprecated
    public static double drip(double x, double d) {
        double c = ConnectibleChains.runtimeConfig.getChainHangAmount();
        double b = -c / d;
        double a = c / (d * d);
        return (a * (x * x) + b * x);
    }

    /**
     * For geogebra:
     * a = 9
     * h = 0
     * d = 5
     * p1 = a * asinh( (h / (2*a)) * 1 / sinh(d / (2*a)) )
     * p2 = -a * cosh( (2*p1 - d) / (2*a) )
     * f(x) = p2 + a * cosh( (2*x + 2*p1 - d) / (2*a) )
     *
     * @param x from 0 to d
     * @param d length of the chain
     * @param h height at x=d
     * @return y
     */
    public static double drip2(double x, double d, double h) {
        double a = ConnectibleChains.runtimeConfig.getChainHangAmount();
        a = a + (d * 0.3);
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        double p2 = -a * Math.cosh((2D * p1 - d) / (2D * a));
        return p2 + a * Math.cosh((((2D * x) + (2D * p1)) - d) / (2D * a));
    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    /**
     * Derivative of drip2
     * For geogebra:
     * f'(x) = sinh( (2*x + 2*p1 - d) / (2*a) )
     *
     * @param x from 0 to d
     * @param d length of the chain
     * @param h height at x=d
     * @return gradient at x
     */
    public static double drip2prime(double x, double d, double h) {
        double a = ConnectibleChains.runtimeConfig.getChainHangAmount();
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        return Math.sinh((2 * x + 2 * p1 - d) / (2 * a));
    }

    public static Vec3d middleOf(Vec3d a, Vec3d b) {
        double x = (a.getX() - b.getX()) / 2d + b.getX();
        double y = (a.getY() - b.getY()) / 2d + b.getY();
        double z = (a.getZ() - b.getZ()) / 2d + b.getZ();
        return new Vec3d(x, y, z);
    }

    /**
     * Get the x/z offset from a chain to a fence
     *
     * @param start fence pos
     * @param end   fence pos
     * @return the x/z offset
     */
    public static Vec3d getChainOffset(Vec3d start, Vec3d end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.normalize(2 / 16f);
        return new Vec3d(offset);
    }
}
