package com.backcountry.selenium_framework;

import static com.backcountry.selenium_framework.CommonUtilities.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Adapted from http://mindmeat.blogspot.com/2008/07/java-image-comparison.html
 * 
 * Compare two images and mark differences with red boxes. 
 * 
 * @author dbogardus
 *
 */
@SuppressWarnings("restriction")
public class ImageCompare {
	
	protected BufferedImage goldImage = null;
	protected BufferedImage comparisonImage = null;
	protected BufferedImage imgc = null;
	protected int widthOfComparisonSections = 0;
	protected int heightOfComparisonSections = 0;
	protected int avg_rgb_diff_allowed = 0;
	protected int stabilizer = 10;
	protected boolean match = false;
	protected int debugMode = 0; // 1: textual indication of change, 2: difference of factors

	public static final Class thisClass = ImageCompare.class;
	
	/* create a runable demo thing. */
	public static void main(String[] args) {
		// Create a compare object specifying the 2 images for comparison.
		ImageCompare ic = new ImageCompare("c:\\test1.jpg", "c:\\test2.jpg");
		// Set the comparison parameters. 
		//   (num vertical regions, num horizontal regions, sensitivity, stabilizer)
		ic.setParameters(8, 6, 5, 10);
		// Display some indication of the differences in the image.
		ic.setDebugMode(2);
		// Compare.
		ic.compare();
		// Display if these images are considered a match according to our parameters.
		//System.out.println("Match: " + ic.match());
		// If its not a match then write a file to show changed regions.
		if (!ic.match()) {
			saveJPG(ic.getChangeIndicator(), "c:\\changes.jpg");
		}
	}
	
	// constructor 1. use filenames
	public ImageCompare(String file1, String file2) {
		this(loadJPG(file1), loadJPG(file2));
	}
 
	// constructor 2. use awt images.
	public ImageCompare(Image img1, Image img2) {
		this(imageToBufferedImage(img1), imageToBufferedImage(img2));
	}
 
	// constructor 3. use buffered images. all roads lead to the same place. this place.
	public ImageCompare(BufferedImage goldImage, BufferedImage comparisonImage) {
		this.goldImage = goldImage;
		this.comparisonImage = comparisonImage;
		setParametersPerImageDimension(goldImage.getHeight(), comparisonImage.getWidth());
	}
	
	// like this to perhaps be upgraded to something more heuristic in the future.
	protected void setParametersPerImageDimension(int height, int width) {
		final int goalWidthOfComparisonSections = 25;
		final int goalHeightOfComparisonSections = 75;
		
		if(height < goalHeightOfComparisonSections){
			heightOfComparisonSections = height;
		}else{
			heightOfComparisonSections = goalHeightOfComparisonSections;
		}
		
		if(width < goalWidthOfComparisonSections){
			widthOfComparisonSections = width;
		}else{
			widthOfComparisonSections = goalWidthOfComparisonSections;
		}
		
		if(this.debugMode > 0){
			outputln(thisClass, "Height of comparison slices [" + heightOfComparisonSections + "]");
			outputln(thisClass, "Width of comparison slices [" + widthOfComparisonSections + "]");
		}
		
		//Tuned this from 1 on 6/7/2013 as minor diffs were tripping problems after switch 
		//to RGB comparison
		avg_rgb_diff_allowed = 1000; 
		stabilizer = 1;
	}
	
	// set the parameters for use during change detection.
	public void setParameters(int x, int y, int sensitivity, int stabilizer) {
		this.widthOfComparisonSections = x;
		this.heightOfComparisonSections = y;
		this.avg_rgb_diff_allowed = sensitivity;
		this.stabilizer = stabilizer;
	}
	
	// want to see some stuff in the console as the comparison is happening?
	public void setDebugMode(int m) {
		this.debugMode = m;
	}
	
	// compare the two images in this object.
	public void compare() {
		// setup change display image
		imgc = imageToBufferedImage(comparisonImage);
		Graphics2D gc = imgc.createGraphics();
		gc.setColor(Color.RED);
		// convert to gray images.
		//img1 = imageToBufferedImage(GrayFilter.createDisabledImage(img1));
		//img2 = imageToBufferedImage(GrayFilter.createDisabledImage(img2));
		// how big are each section
		
		int smallestWidth;
		
		if(goldImage.getWidth() <= comparisonImage.getWidth()){
			smallestWidth = goldImage.getWidth();
		}else{
			smallestWidth = comparisonImage.getWidth();
		}

		int smallestHeight;
		
		if(goldImage.getHeight() <= comparisonImage.getHeight()){
			smallestHeight = goldImage.getHeight();
		}else{
			smallestHeight = comparisonImage.getHeight();
		}
		
		int comparisonSectionCountByWidth = smallestWidth / widthOfComparisonSections;
		int comparisonSectionCountByHeight = smallestHeight / heightOfComparisonSections;
		
		if (debugMode > 0){
			outputln(thisClass, "comparisonSectionCountByWidth : " + comparisonSectionCountByWidth);
			outputln(thisClass, "comparisonSectionCountByHeight : " + comparisonSectionCountByHeight);
		}
		
		String img1_Dimensions = goldImage.getWidth() + "x" + goldImage.getHeight();
		String img2_Dimensions = comparisonImage.getWidth() + "x" + comparisonImage.getHeight();
		
		if(!img1_Dimensions.equals(img2_Dimensions)){
			outputln(ImageCompare.class, 
					"Image dimensions are different - "  + 
					"[" + img1_Dimensions + "] vs [" + img2_Dimensions + "], " +
					"comparing from upper left corner.");
		}
		
		// set to a match by default, if a change is found then flag non-match
		this.match = true;
		// loop through whole image and compare individual blocks of images
		for (int y = 0; y < comparisonSectionCountByHeight; y++) {
			if (debugMode > 0) System.out.print("|");
			for (int x = 0; x < comparisonSectionCountByWidth; x++) {
				BufferedImage image1Slice = 
						goldImage.getSubimage(x*widthOfComparisonSections, y*heightOfComparisonSections,
								widthOfComparisonSections - 1, heightOfComparisonSections - 1);
				
				BufferedImage image2Slice = 
						comparisonImage.getSubimage(
								x*widthOfComparisonSections, y*heightOfComparisonSections, 
								widthOfComparisonSections - 1, heightOfComparisonSections - 1);
				
				int goldSliceAvgRGB = getAverageRGBValue(image1Slice);
				int comparisonSliceAvgRGB = getAverageRGBValue(image2Slice);
				
				if (debugMode > 0){
					System.out.println(
							"Image1 Slice Dims "
							+ "[" + image1Slice.getWidth() + "x" + image1Slice.getHeight() + "]");
					
					System.out.println(
							"Image2 Slice Dims "
							+ "[" + image2Slice.getWidth() + "x" + image2Slice.getHeight() + "]");
					
					System.out.println("Avg RGB area1 [" + goldSliceAvgRGB + "]");
					System.out.println("Avg RBG area2 [" + comparisonSliceAvgRGB + "]");
				}
				
				int diff = Math.abs(goldSliceAvgRGB - comparisonSliceAvgRGB);
				
				if (diff > avg_rgb_diff_allowed) { 
					// the difference in a certain region has passed the 
					//threshold value for sensitivity, draw an indicator on the change image 
					//to show where change was detected.
					outputln(thisClass, "Image difference detected [" + diff + "] beyond threshold "
							+ "[" + avg_rgb_diff_allowed + "] in image slice.");
					
					gc.drawRect(x*widthOfComparisonSections, y*heightOfComparisonSections, widthOfComparisonSections - 1, heightOfComparisonSections - 1);
					this.match = false;
				}
				if (debugMode == 1) System.out.print((diff > avg_rgb_diff_allowed ? "X" : " "));
				if (debugMode == 2) System.out.print(diff + (x < widthOfComparisonSections - 1 ? "," : ""));
			}
			if (debugMode > 0) System.out.println("|");
		}
	}
	
	// return the image that indicates the regions where changes where detected.
	public BufferedImage getChangeIndicator() {
		return imgc;
	}
	
	// returns a value specifying some kind of average brightness in the image.
	protected int getAverageRGBValue(BufferedImage img) {
		int totalRGB = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int rgb_for_pixel = img.getRGB(img.getMinX() + x, img.getMinY() + y);
				totalRGB += Math.abs(rgb_for_pixel);
			}
		}
		
		int area = (img.getWidth()/stabilizer)*(img.getHeight()/stabilizer);
		
		if (debugMode > 0){
			System.out.println("Buff Height [" + img.getHeight() + "]");
			System.out.println("Buff Width [" + img.getWidth() + "]");
			
			System.out.println("Area of comparison slice [" + area + "]");
			System.out.println("Total RGB for area [" + totalRGB + "]");
		}
		
		//To avoid divide by zero 
		if(area == 0){
			area = 1;
		}
		
		return totalRGB / area;
		
	}
	
	// returns true if image pair is considered a match
	public boolean match() {
		return this.match;
	}

	// buffered images are just better.
	protected static BufferedImage imageToBufferedImage(Image img) {
		BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(img, null, null);
		return bi;
	}
	
	// write a buffered image to a jpeg file.
	public static void saveJPG(Image img, String filename) {
		BufferedImage bi = imageToBufferedImage(img);
		
		saveJPG(bi, filename);
	}
	
	
	// write a buffered image to a jpeg file.
	@SuppressWarnings("all")
	public static void saveJPG(BufferedImage bi, String filename) {
		
		File outputFile = new File(filename);
		
		if(!outputFile.exists()){
			try {
				outputFile.createNewFile();
				System.out.println("File Path:" + outputFile.getAbsolutePath());
			} catch (IOException e) {
				System.out.println("Could not create file:" + filename); 
				e.printStackTrace();
			}
		}
		
		System.out.println("File Path:" + outputFile.getAbsolutePath());
		
		FileOutputStream out = null;
		try { 
			out = new FileOutputStream(filename);
		} catch (java.io.FileNotFoundException io) { 
			System.out.println("File Not Found"); 
		}
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
		param.setQuality(0.8f,false);
		encoder.setJPEGEncodeParam(param);
		try { 
			encoder.encode(bi); 
			out.close(); 
		} catch (java.io.IOException io) {
			System.out.println("IOException"); 
		}
	}
	
	
	// read a jpeg file into a buffered image
	@SuppressWarnings("all")
	protected static Image loadJPG(String filename) {
		FileInputStream in = null;
		try { 
			in = new FileInputStream(filename);
		} catch (java.io.FileNotFoundException io) { 
			System.out.println("File Not Found"); 
		}
		JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
		BufferedImage bi = null;
		try { 
			bi = decoder.decodeAsBufferedImage(); 
			in.close(); 
		} catch (java.io.IOException io) {
			System.out.println("IOException");
		}
		return bi;
	}
	
	
	public static BufferedImage horizontalflip(BufferedImage img) {
	        int w = img.getWidth();
	        int h = img.getHeight();
	        BufferedImage dimg = new BufferedImage(w, h, img.getType());
	        Graphics2D g = dimg.createGraphics();
	        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
	        g.dispose();
	        return dimg;
	}
	
	
	public static BufferedImage getImageDifferences(BufferedImage goldImage, 
			BufferedImage comparisonImage) {
		
		// Create a compare object specifying the 2 images for comparison.
		ImageCompare ic = new ImageCompare(goldImage, comparisonImage);
		
		ic.compare();
		
		// If its not a match then write a file to show changed regions.
		if (!ic.match()) {
			return ic.getChangeIndicator();
		}
		return null;
	}
	 
	
}
