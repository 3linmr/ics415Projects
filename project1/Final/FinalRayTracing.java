import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class FinalRayTracing {

    // ------------------------------------------------------------------------------------
    //  Basic 3D Vector
    // ------------------------------------------------------------------------------------
    static class Vec3 {
        public double x, y, z;

        public Vec3() {
            this(0, 0, 0);
        }
        public Vec3(double x, double y, double z) {
            this.x = x; 
            this.y = y; 
            this.z = z;
        }

        // Add, subtract, multiply element-wise, scale
        public Vec3 add(Vec3 o) { 
            return new Vec3(x + o.x, y + o.y, z + o.z); 
        }
        public Vec3 sub(Vec3 o) { 
            return new Vec3(x - o.x, y - o.y, z - o.z); 
        }
        public Vec3 mul(Vec3 o) { 
            return new Vec3(x * o.x, y * o.y, z * o.z); 
        }
        public Vec3 scale(double t) { 
            return new Vec3(x * t, y * t, z * t); 
        }

        // Dot product, length, normalize
        public double dot(Vec3 o) { 
            return x * o.x + y * o.y + z * o.z; 
        }
        public double length() { 
            return Math.sqrt(dot(this)); 
        }
        public Vec3 normalize() { 
            return scale(1.0 / length()); 
        }

        // Cross product (required for camera setup)
        public Vec3 cross(Vec3 o) {
            return new Vec3(
                y * o.z - z * o.y,
                z * o.x - x * o.z,
                x * o.y - y * o.x
            );
        }
    }

    // ------------------------------------------------------------------------------------
    //  Ray
    // ------------------------------------------------------------------------------------
    static class Ray {
        public Vec3 origin, dir;

        public Ray(Vec3 origin, Vec3 dir) {
            this.origin = origin; 
            this.dir = dir;
        }

        public Vec3 at(double t) {
            return origin.add(dir.scale(t));
        }
    }

    // ------------------------------------------------------------------------------------
    //  Material interface + implementations
    // ------------------------------------------------------------------------------------
    interface Material {
        ScatterResult scatter(Ray rIn, HitRecord rec, Random rng);
    }

    static class ScatterResult {
        boolean scattered;
        Ray outRay;
        Vec3 attenuation;
        public ScatterResult(boolean scattered, Ray outRay, Vec3 attenuation) {
            this.scattered   = scattered;
            this.outRay      = outRay;
            this.attenuation = attenuation;
        }
    }

    static class Lambertian implements Material {
        private final Vec3 albedo;
        public Lambertian(Vec3 albedo) {
            this.albedo = albedo;
        }
        @Override
        public ScatterResult scatter(Ray rIn, HitRecord rec, Random rng) {
            // Cosine-weighted random direction in hemisphere
            Vec3 scatterDir = rec.normal.add(randomUnitVector(rng));
            // If it's near zero, fallback to normal
            if (nearZero(scatterDir)) {
                scatterDir = rec.normal;
            }
            return new ScatterResult(true, new Ray(rec.p, scatterDir), albedo);
        }
    }

    static class Metal implements Material {
        private final Vec3 albedo;
        private final double fuzz;
        public Metal(Vec3 albedo, double fuzz) {
            this.albedo = albedo;
            // limit fuzz in [0,1]
            this.fuzz = (fuzz < 1) ? fuzz : 1;
        }
        @Override
        public ScatterResult scatter(Ray rIn, HitRecord rec, Random rng) {
            Vec3 reflected = reflect(rIn.dir.normalize(), rec.normal);
            // add fuzz
            reflected = reflected.add(randomInUnitSphere(rng).scale(fuzz));
            Ray scattered = new Ray(rec.p, reflected);
            boolean scatteredOk = scattered.dir.dot(rec.normal) > 0;
            return new ScatterResult(scatteredOk, scattered, albedo);
        }
    }

    static class Dielectric implements Material {
        private final double ir; // Index of refraction
        public Dielectric(double indexOfRefraction) {
            this.ir = indexOfRefraction;
        }
        @Override
        public ScatterResult scatter(Ray rIn, HitRecord rec, Random rng) {
            double refRatio = rec.frontFace ? (1.0 / ir) : ir;
            Vec3 unitDir = rIn.dir.normalize();
            double cosTheta = Math.min(unitDir.scale(-1).dot(rec.normal), 1.0);
            double sinTheta = Math.sqrt(1.0 - cosTheta*cosTheta);

            boolean cannotRefract = refRatio * sinTheta > 1.0;
            Vec3 direction;
            if (cannotRefract || reflectance(cosTheta, refRatio) > rng.nextDouble()) {
                // reflection
                direction = reflect(unitDir, rec.normal);
            } else {
                // refraction
                direction = refract(unitDir, rec.normal, refRatio);
            }
            return new ScatterResult(true, new Ray(rec.p, direction), new Vec3(1.0, 1.0, 1.0));
        }
    }

    // ------------------------------------------------------------------------------------
    //  Sphere & HittableList
    // ------------------------------------------------------------------------------------
    interface Hittable {
        boolean hit(Ray r, double tMin, double tMax, HitRecord rec);
    }

    static class HitRecord {
        public Vec3 p;          // intersection point
        public Vec3 normal;     // normal at intersection
        public Material mat;
        public double t;        // ray parameter
        public boolean frontFace;

        public void setFaceNormal(Ray r, Vec3 outwardNormal) {
            frontFace = r.dir.dot(outwardNormal) < 0;
            normal = frontFace ? outwardNormal : outwardNormal.scale(-1);
        }
    }

    static class Sphere implements Hittable {
        public Vec3 center;
        public double radius;
        public Material mat;
        public Sphere(Vec3 c, double r, Material m) {
            center = c; 
            radius = r; 
            mat = m;
        }
        @Override
        public boolean hit(Ray r, double tMin, double tMax, HitRecord rec) {
            Vec3 oc = r.origin.sub(center);
            double a = r.dir.dot(r.dir);
            double halfB = oc.dot(r.dir);
            double c = oc.dot(oc) - radius*radius;
            double discriminant = halfB*halfB - a*c;
            if (discriminant < 0) return false;
            double sqrtd = Math.sqrt(discriminant);

            double root = (-halfB - sqrtd) / a;
            if (root < tMin || root > tMax) {
                root = (-halfB + sqrtd) / a;
                if (root < tMin || root > tMax) {
                    return false;
                }
            }

            rec.t = root;
            rec.p = r.at(rec.t);
            Vec3 outwardNormal = rec.p.sub(center).scale(1.0 / radius);
            rec.setFaceNormal(r, outwardNormal);
            rec.mat = mat;
            return true;
        }
    }

    static class HittableList implements Hittable {
        private final java.util.List<Hittable> objects = new java.util.ArrayList<>();
        public void add(Hittable obj) {
            objects.add(obj);
        }
        @Override
        public boolean hit(Ray r, double tMin, double tMax, HitRecord rec) {
            HitRecord tempRec = new HitRecord();
            boolean hitAnything = false;
            double closestSoFar = tMax;

            for (Hittable obj : objects) {
                if (obj.hit(r, tMin, closestSoFar, tempRec)) {
                    hitAnything = true;
                    closestSoFar = tempRec.t;
                    rec.p = tempRec.p;
                    rec.normal = tempRec.normal;
                    rec.mat = tempRec.mat;
                    rec.t = tempRec.t;
                    rec.frontFace = tempRec.frontFace;
                }
            }
            return hitAnything;
        }
    }

    // ------------------------------------------------------------------------------------
    //  Camera
    // ------------------------------------------------------------------------------------
    static class Camera {
        private Vec3 origin;
        private Vec3 horizontal;
        private Vec3 vertical;
        private Vec3 lowerLeftCorner;
        private Vec3 u, v, w;
        private double lensRadius;

        public Camera(Vec3 lookFrom, Vec3 lookAt, Vec3 vUp,
                      double vfov, double aspectRatio) {
            double theta = Math.toRadians(vfov);
            double h = Math.tan(theta / 2);
            double viewportHeight = 2.0 * h;
            double viewportWidth = aspectRatio * viewportHeight;

            w = lookFrom.sub(lookAt).normalize();
            u = vUp.cross(w).normalize();
            v = w.cross(u);

            origin = lookFrom;
            horizontal = u.scale(viewportWidth);
            vertical   = v.scale(viewportHeight);
            lowerLeftCorner = origin
                    .sub(horizontal.scale(0.5))
                    .sub(vertical.scale(0.5))
                    .sub(w);
            lensRadius = 0;
        }

        public Ray getRay(double s, double t) {
            Vec3 direction = lowerLeftCorner
                    .add(horizontal.scale(s))
                    .add(vertical.scale(t))
                    .sub(origin);
            return new Ray(origin, direction);
        }
    }

    // ------------------------------------------------------------------------------------
    //  Scene Setup: Large spheres + many small spheres
    // ------------------------------------------------------------------------------------
    private static HittableList randomScene(Random rng) {
        HittableList world = new HittableList();

        // Ground
        Material groundMat = new Lambertian(new Vec3(0.5, 0.5, 0.5));
        world.add(new Sphere(new Vec3(0, -1000, 0), 1000, groundMat));

        // Many small spheres
        for (int a = -11; a < 11; a++) {
            for (int b = -11; b < 11; b++) {
                double chooseMat = rng.nextDouble();
                Vec3 center = new Vec3(a + 0.9 * rng.nextDouble(), 0.2, b + 0.9 * rng.nextDouble());

                // Skip spheres that are too close to the "big" ones in center
                if (center.sub(new Vec3(0,1,0)).length() > 0.9) {
                    if (chooseMat < 0.6) {
                        // Diffuse
                        Vec3 albedo = randomColor(rng).mul(randomColor(rng));
                        world.add(new Sphere(center, 0.2, new Lambertian(albedo)));
                    } else if (chooseMat < 0.85) {
                        // Metal
                        Vec3 albedo = randomColor(rng).scale(0.5);
                        double fuzz = 0.5 * rng.nextDouble();
                        world.add(new Sphere(center, 0.2, new Metal(albedo, fuzz)));
                    } else {
                        // Glass
                        world.add(new Sphere(center, 0.2, new Dielectric(1.5)));
                    }
                }
            }
        }

        // Three large spheres
        world.add(new Sphere(new Vec3(0, 1, 0), 1.0, new Dielectric(1.5)));    // glass
        world.add(new Sphere(new Vec3(-4, 1, 0), 1.0, 
                       new Lambertian(new Vec3(0.4, 0.2, 0.1))));             // diffuse
        world.add(new Sphere(new Vec3(4, 1, 0), 1.0, 
                       new Metal(new Vec3(0.7, 0.6, 0.5), 0.0)));             // metal

        return world;
    }

    // ------------------------------------------------------------------------------------
    //  Ray Color (recursive)
    // ------------------------------------------------------------------------------------
    private static Vec3 rayColor(Ray r, Hittable world, int depth, Random rng) {
        if (depth <= 0) {
            // Exceeded recursion limit
            return new Vec3(0,0,0);
        }

        HitRecord rec = new HitRecord();
        if (world.hit(r, 0.001, Double.POSITIVE_INFINITY, rec)) {
            ScatterResult scatter = rec.mat.scatter(r, rec, rng);
            if (scatter.scattered) {
                Vec3 attenuation = scatter.attenuation;
                Vec3 recurseColor = rayColor(scatter.outRay, world, depth - 1, rng);
                return attenuation.mul(recurseColor);
            }
            return new Vec3(0,0,0); // absorbed
        }
        // Sky background
        Vec3 unitDir = r.dir.normalize();
        double t = 0.5 * (unitDir.y + 1.0);
        return new Vec3(1.0, 1.0, 1.0).scale(1.0 - t)
               .add(new Vec3(0.5, 0.7, 1.0).scale(t));
    }

    // ------------------------------------------------------------------------------------
    //  Main: Render
    // ------------------------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        // Image
        final int imageWidth = 800;
        final int imageHeight = (int)(imageWidth / (16.0 / 9.0));
        final int samplesPerPixel = 100;  // Anti‐aliasing
        final int maxDepth = 50;         // Max bounces
    
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    
        // World
        Random rng = new Random();
        HittableList world = randomScene(rng);
    
        // Camera
        Vec3 lookFrom = new Vec3(13, 2, 3);
        Vec3 lookAt   = new Vec3(0, 0, 0);
        Vec3 vUp      = new Vec3(0, 1, 0);
        double vfov   = 20.0;
        double aspectRatio = (double)imageWidth / imageHeight;
        Camera cam = new Camera(lookFrom, lookAt, vUp, vfov, aspectRatio);
    
        // -------------- Time Measurement Start --------------
        long startTime = System.currentTimeMillis();
        // Render
        for (int j = 0; j < imageHeight; j++) {
            int row = imageHeight - 1 - j;
            System.out.printf("Scanlines remaining: %d\n", imageHeight - j - 1);
    
            for (int i = 0; i < imageWidth; i++) {
                Vec3 pixelColor = new Vec3(0, 0, 0);
                for (int s = 0; s < samplesPerPixel; s++) {
                    double u = (i + rng.nextDouble()) / (imageWidth  - 1);
                    double v = (j + rng.nextDouble()) / (imageHeight - 1);
                    Ray r = cam.getRay(u, v);
                    pixelColor = pixelColor.add(rayColor(r, world, maxDepth, rng));
                }
                // Average & gamma-correct
                double scale = 1.0 / samplesPerPixel;
                pixelColor = pixelColor.scale(scale);
                pixelColor = new Vec3(
                    Math.sqrt(pixelColor.x),
                    Math.sqrt(pixelColor.y),
                    Math.sqrt(pixelColor.z)
                );
    
                // Convert to [0..255]
                int ir = (int)(256 * clamp(pixelColor.x, 0.0, 0.999));
                int ig = (int)(256 * clamp(pixelColor.y, 0.0, 0.999));
                int ib = (int)(256 * clamp(pixelColor.z, 0.0, 0.999));
    
                int rgb = (ir << 16) | (ig << 8) | (ib);
                image.setRGB(i, row, rgb);
            }
        }
        // -------------- Time Measurement End --------------
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Rendering time: " + elapsed + " ms (" + (elapsed / 1000.0) + " s)");
    
        // Write to file
        File outFile = new File("output.png");
        ImageIO.write(image, "png", outFile);
        System.out.println("Done! Image saved to " + outFile.getAbsolutePath());
    }
    

    // ------------------------------------------------------------------------------------
    //  Utility functions
    // ------------------------------------------------------------------------------------
    private static double clamp(double x, double min, double max) {
        if (x < min) return min;
        if (x > max) return max;
        return x;
    }

    private static Vec3 reflect(Vec3 v, Vec3 n) {
        // reflection = v - 2 * (v·n) * n
        return v.sub(n.scale(2.0 * v.dot(n)));
    }

    private static Vec3 refract(Vec3 uv, Vec3 n, double etaiOverEtat) {
        double cosTheta = Math.min(uv.scale(-1).dot(n), 1.0);
        Vec3 rOutPerp = uv.add(n.scale(cosTheta)).scale(etaiOverEtat);
        Vec3 rOutParallel = n.scale(-Math.sqrt(Math.abs(1.0 - rOutPerp.dot(rOutPerp))));
        return rOutPerp.add(rOutParallel);
    }

    // Schlick's approximation for reflectance
    private static double reflectance(double cosine, double refIdx) {
        double r0 = (1 - refIdx) / (1 + refIdx);
        r0 = r0 * r0;
        return r0 + (1 - r0) * Math.pow((1 - cosine), 5);
    }

    // Check if a vector is close to zero in all components
    private static boolean nearZero(Vec3 v) {
        double s = 1e-8;
        return (Math.abs(v.x) < s) && (Math.abs(v.y) < s) && (Math.abs(v.z) < s);
    }

    // Returns a random unit vector (in hemisphere)
    private static Vec3 randomUnitVector(Random rng) {
        double a = 2.0 * Math.PI * rng.nextDouble();
        double z = 2.0*rng.nextDouble() - 1.0;
        double r = Math.sqrt(1 - z*z);
        return new Vec3(r*Math.cos(a), r*Math.sin(a), z);
    }

    // Returns a random point in a unit sphere
    private static Vec3 randomInUnitSphere(Random rng) {
        while (true) {
            Vec3 p = new Vec3(
                2*rng.nextDouble() - 1,
                2*rng.nextDouble() - 1,
                2*rng.nextDouble() - 1
            );
            if (p.dot(p) >= 1) continue;
            return p;
        }
    }

    // Returns a random color in [0..1]
    private static Vec3 randomColor(Random rng) {
        return new Vec3(rng.nextDouble(), rng.nextDouble(), rng.nextDouble());
    }
}
