package com.nokia.miaf.gallery;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Google Motion Photo 创建示例
 * 
 * 演示如何使用 MotionPhotoComposer 和 HEIFXmpInjector
 * 创建符合 Google Motion Photo 规范的 HEIC 动态照片
 */
public class MotionPhotoExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Google Motion Photo Creator");
        System.out.println("=".repeat(60));
        
        // 示例 1: 从文件创建 Motion Photo
        example1_CreateFromFiles();
        
        // 示例 2: 从字节数组创建
        example2_CreateFromBytes();
        
        // 示例 3: 验证和提取 XMP
        example3_VerifyAndExtractXmp();
    }
    
    /**
     * 示例 1: 从文件创建 Motion Photo
     */
    private static void example1_CreateFromFiles() {
        System.out.println("\n[Example 1] Creating Motion Photo from files");
        System.out.println("-".repeat(60));
        
        String imagePath = "/path/to/image.heic";  // 替换为实际路径
        String videoPath = "/path/to/video.mp4";   // 替换为实际路径
        String outputPath = "/path/to/motion_photo.heic";
        
        MotionPhotoComposer composer = new MotionPhotoComposer();
        
        boolean success = composer.composeMotionPhotoToFile(
            imagePath,
            videoPath,
            outputPath
        );
        
        if (success) {
            System.out.println("✓ Motion Photo created successfully!");
            System.out.println("  Output: " + outputPath);
        } else {
            System.out.println("✗ Failed to create Motion Photo");
        }
    }
    
    /**
     * 示例 2: 从字节数组创建
     */
    private static void example2_CreateFromBytes() {
        System.out.println("\n[Example 2] Creating Motion Photo from byte arrays");
        System.out.println("-".repeat(60));
        
        try {
            // 读取图像和视频数据
            byte[] imageData = Files.readAllBytes(Paths.get("/path/to/image.heic"));
            byte[] videoData = Files.readAllBytes(Paths.get("/path/to/video.mp4"));
            
            // 创建 Motion Photo
            MotionPhotoComposer composer = new MotionPhotoComposer();
            byte[] motionPhoto = composer.composeMotionPhoto(imageData, videoData);
            
            if (motionPhoto != null) {
                // 保存结果
                Files.write(Paths.get("/path/to/motion_photo.heic"), motionPhoto);
                
                System.out.println("✓ Motion Photo created!");
                System.out.println("  Size: " + motionPhoto.length + " bytes");
                System.out.println("  = Image: " + imageData.length + " bytes");
                System.out.println("  + Video: " + videoData.length + " bytes");
                System.out.println("  + Overhead: " + (motionPhoto.length - imageData.length - videoData.length) + " bytes");
            } else {
                System.out.println("✗ Failed to create Motion Photo");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 示例 3: 验证和提取 XMP
     */
    private static void example3_VerifyAndExtractXmp() {
        System.out.println("\n[Example 3] Verifying and extracting XMP metadata");
        System.out.println("-".repeat(60));
        
        try {
            // 读取 Motion Photo 文件
            byte[] motionPhoto = Files.readAllBytes(Paths.get("/path/to/motion_photo.heic"));
            
            // 验证文件结构
            System.out.println("Analyzing file structure...");
            analyzeFileStructure(motionPhoto);
            
            // 提取和显示 XMP
            HEIFXmpInjector xmpInjector = new HEIFXmpInjector();
            String xmp = xmpInjector.extractXmp(motionPhoto);
            
            if (xmp != null) {
                System.out.println("\n✓ XMP metadata found:");
                System.out.println("-".repeat(60));
                System.out.println(xmp);
                System.out.println("-".repeat(60));
                
                // 验证关键属性
                verifyXmpContent(xmp);
            } else {
                System.out.println("✗ No XMP metadata found");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 分析文件结构
     */
    private static void analyzeFileStructure(byte[] data) {
        var boxes = ISOBMFFParser.parseTopLevelBoxes(data);
        
        System.out.println("File structure:");
        long totalSize = 0;
        for (Box box : boxes) {
            System.out.printf("  [%s] offset=0x%08X size=%d bytes\n", 
                box.type, box.offset, box.size);
            totalSize += box.size;
        }
        System.out.printf("Total: %d bytes\n", totalSize);
        
        // 检查是否有 mpvd box
        boolean hasMpvd = false;
        boolean hasXml = false;
        for (Box box : boxes) {
            if ("mpvd".equals(box.type)) {
                hasMpvd = true;
                System.out.println("✓ Found mpvd box (Motion Photo video data)");
            }
            if ("xml ".equals(box.type)) {
                hasXml = true;
                System.out.println("✓ Found xml box (XMP metadata)");
            }
        }
        
        if (!hasMpvd) {
            System.out.println("✗ Missing mpvd box");
        }
        if (!hasXml) {
            System.out.println("⚠ Missing xml box (XMP metadata)");
        }
    }
    
    /**
     * 验证 XMP 内容
     */
    private static void verifyXmpContent(String xmp) {
        System.out.println("\nVerifying XMP content:");
        
        // 检查必需的属性
        checkAttribute(xmp, "GCamera:MotionPhoto", "1");
        checkAttribute(xmp, "GCamera:MotionPhotoVersion", "1");
        checkAttribute(xmp, "Item:Semantic=\"Primary\"", null);
        checkAttribute(xmp, "Item:Semantic=\"MotionPhoto\"", null);
        checkAttribute(xmp, "Item:Padding=\"8\"", null);
        checkAttribute(xmp, "Item:Mime=\"video/mp4\"", null);
    }
    
    private static void checkAttribute(String xmp, String attribute, String expectedValue) {
        if (xmp.contains(attribute)) {
            if (expectedValue != null) {
                String pattern = attribute + "=\"" + expectedValue + "\"";
                if (xmp.contains(pattern)) {
                    System.out.println("  ✓ " + pattern);
                } else {
                    System.out.println("  ⚠ " + attribute + " found but value may be incorrect");
                }
            } else {
                System.out.println("  ✓ " + attribute);
            }
        } else {
            System.out.println("  ✗ Missing: " + attribute);
        }
    }
    
    /**
     * 高级示例：批量处理
     */
    @SuppressWarnings("unused")
    private static void example4_BatchProcessing() {
        System.out.println("\n[Example 4] Batch processing");
        System.out.println("-".repeat(60));
        
        String[] imagePaths = {
            "/path/to/image1.heic",
            "/path/to/image2.heic",
            "/path/to/image3.heic"
        };
        
        String[] videoPaths = {
            "/path/to/video1.mp4",
            "/path/to/video2.mp4",
            "/path/to/video3.mp4"
        };
        
        MotionPhotoComposer composer = new MotionPhotoComposer();
        
        for (int i = 0; i < imagePaths.length; i++) {
            String outputPath = "/path/to/motion_photo_" + (i + 1) + ".heic";
            
            System.out.println("Processing " + (i + 1) + "/" + imagePaths.length + "...");
            
            boolean success = composer.composeMotionPhotoToFile(
                imagePaths[i],
                videoPaths[i],
                outputPath
            );
            
            if (success) {
                System.out.println("  ✓ " + outputPath);
            } else {
                System.out.println("  ✗ Failed");
            }
        }
    }
}
