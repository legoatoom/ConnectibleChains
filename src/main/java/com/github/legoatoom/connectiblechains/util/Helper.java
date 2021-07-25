/*
 * Copyright (C) 2021 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.util;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Helper {

    public static Identifier identifier(String name) {
        return new Identifier(ConnectibleChains.MODID, name);
    }

    @Deprecated
    public static double drip(double x, double d) {
        double c = ConnectibleChains.config.getChainHangAmount();
        double b = -c / d;
        double a = c / (d * d);
        return (a * (x * x) + b * x);
    }

    public static double drip2(double x, double d, double h) {
        double a = ConnectibleChains.config.getChainHangAmount();
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        double p2 = -a * Math.cosh((2D * p1 - d) / (2D * a));
        return p2 + a * Math.cosh((((2D * x) + (2D * p1)) - d) / (2D * a));
    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    public static Vec3d middleOf(Vec3d a, Vec3d b) {
        double x = (a.getX() - b.getX()) / 2d + b.getX();
        double y = (a.getY() - b.getY()) / 2d + b.getY();
        double z = (a.getZ() - b.getZ()) / 2d + b.getZ();
        return new Vec3d(x, y, z);
    }
}
