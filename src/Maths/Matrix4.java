package Maths;

import Data.Vertex;

public class Matrix4 {
    public double[][] m = new double[4][4];

    public Matrix4() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                m[i][j] = 0;
            }
        }
    }

    public static Matrix4 getIdentity() {
        Matrix4 mat = new Matrix4();
        mat.m[0][0] = 1;
        mat.m[1][1] = 1;
        mat.m[2][2] = 1;
        mat.m[3][3] = 1;
        return mat;
    }

    public Matrix4 multiply(Matrix4 other) {
        Matrix4 res = new Matrix4();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                res.m[i][j] = this.m[i][0] * other.m[0][j] +
                              this.m[i][1] * other.m[1][j] +
                              this.m[i][2] * other.m[2][j] +
                              this.m[i][3] * other.m[3][j];
            }
        }
        return res;
    }

    public Vertex multiply(Vertex v) {
        double x = v.x * m[0][0] + v.y * m[0][1] + v.z * m[0][2] + v.w * m[0][3];
        double y = v.x * m[1][0] + v.y * m[1][1] + v.z * m[1][2] + v.w * m[1][3];
        double z = v.x * m[2][0] + v.y * m[2][1] + v.z * m[2][2] + v.w * m[2][3];
        double w = v.x * m[3][0] + v.y * m[3][1] + v.z * m[3][2] + v.w * m[3][3];
        return new Vertex(x, y, z, w);
    }

    public static Matrix4 getTranslation(double x, double y, double z) {
        Matrix4 mat = getIdentity();
        mat.m[0][3] = x;
        mat.m[1][3] = y;
        mat.m[2][3] = z;
        return mat;
    }

    public static Matrix4 getRotationX(double angle) {
        Matrix4 mat = getIdentity();
        double c = java.lang.Math.cos(angle);
        double s = java.lang.Math.sin(angle);
        mat.m[1][1] = c;
        mat.m[1][2] = -s;
        mat.m[2][1] = s;
        mat.m[2][2] = c;
        return mat;
    }

    public static Matrix4 getRotationY(double angle) {
        Matrix4 mat = getIdentity();
        double c = java.lang.Math.cos(angle);
        double s = java.lang.Math.sin(angle);
        mat.m[0][0] = c;
        mat.m[0][2] = s;
        mat.m[2][0] = -s;
        mat.m[2][2] = c;
        return mat;
    }

    public static Matrix4 getRotationZ(double angle) {
        Matrix4 mat = getIdentity();
        double c = java.lang.Math.cos(angle);
        double s = java.lang.Math.sin(angle);
        mat.m[0][0] = c;
        mat.m[0][1] = -s;
        mat.m[1][0] = s;
        mat.m[1][1] = c;
        return mat;
    }

    public static Matrix4 getScale(double x, double y, double z) {
        Matrix4 mat = getIdentity();
        mat.m[0][0] = x;
        mat.m[1][1] = y;
        mat.m[2][2] = z;
        return mat;
    }

    public static Matrix4 getProjection(double fov, double aspectRatio, double near, double far) {
        Matrix4 mat = new Matrix4();
        double fovRad = 1.0 / java.lang.Math.tan(fov * 0.5 / 180.0 * java.lang.Math.PI);
        mat.m[0][0] = aspectRatio * fovRad;
        mat.m[1][1] = fovRad;
        mat.m[2][2] = far / (far - near);
        mat.m[2][3] = (-far * near) / (far - near);
        mat.m[3][2] = 1.0;
        mat.m[3][3] = 0.0;
        return mat;
    }
}
