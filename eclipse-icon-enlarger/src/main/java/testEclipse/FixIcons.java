package testEclipse;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Small utility to double the size of eclipse icons for QHD monitors.
 * 
 * @author David Levy
 * @since 2014/04/10
 *
 */
public class FixIcons {

	private static final Logger logger = Logger.getLogger(FixIcons.class);

	public static final void main(String[] args) {
		try {

			Options options = new Options();

			options.addOption(new Option("b", "baseDir", true,
					"This is the base directory where we'll parse jars/zips"));
			options.addOption(new Option("o", "outputDir", true,
					"This is the base directory where we'll place output"));
			for (Option o : new ArrayList<Option>(options.getOptions())) {
				o.setRequired(true);
			}

			GnuParser parser = new GnuParser();
			CommandLine commandLine = parser.parse(options, args);
			String baseDirArg = commandLine.getOptionValue("b");
			logger.info("Base directory: " + baseDirArg);

			String outputDirArg = commandLine.getOptionValue("o");
			logger.info("Output directory: " + outputDirArg);

			File base = new File(baseDirArg);
			if (!base.exists() || !base.canRead() || !base.isDirectory()) {
				logger.error("Unable to read from base directory");
				return;
			}

			File output = new File(outputDirArg);
			if (!output.exists() || !output.canRead() || !output.canWrite()
					|| !output.isDirectory()) {
				logger.error("Unable to write to output director");
				return;
			}

			if (base.list() == null || base.list().length == 0) {
				logger.error("The base directory is empty");
				return;
			}

			if (output.list() != null && output.list().length != 0) {
				logger.error("The output directory is not empty");
				return;
			}

			processDirectory(base, output);

		} catch (ParseException e) {
			logger.error("Unable to parse arguments: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error: " + e.getMessage(), e);
		}

	}

	public static void processDirectory(File directory, File outputDirectory) throws Exception {
		logger.info("Processing directory [" + directory.getAbsolutePath() + "]");
		
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				File targetDir = new File(outputDirectory.getAbsolutePath()
						+ File.separator + file.getName());
				logger.info("Creating directory: "
						+ targetDir.getAbsolutePath());
				targetDir.mkdir();
				processDirectory(file, targetDir);
			} else {
				File targetFile = new File(outputDirectory.getAbsolutePath()
						+ File.separator + file.getName());
				
				
				if (file.getName().toLowerCase().endsWith(".zip")
						|| file.getName().toLowerCase().endsWith(".jar")) {
					logger.info("Processing archive file: " + file.getAbsolutePath());
					
					ZipFile zipSrc = new ZipFile(file);
					Enumeration<ZipEntry> srcEntries = (Enumeration<ZipEntry>) zipSrc.entries();
					
					ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(targetFile));
					
					while (srcEntries.hasMoreElements()) {
						ZipEntry entry = (ZipEntry) srcEntries.nextElement();
			            logger.info("Processing zip entry ["+ entry.getName() + "]");
						
						ZipEntry newEntry = new ZipEntry(entry.getName());
			            outStream.putNextEntry(newEntry);

			            BufferedInputStream bis = new BufferedInputStream(zipSrc
			                            .getInputStream(entry));

			            if (ImageType.findType(entry.getName()) != null) {
			            	processImage(entry.getName(), bis, outStream);
			            } else {
			            	IOUtils.copy(bis, outStream);
			            }
			            
			            
			            outStream.closeEntry();
			            bis.close();						
					}
					
					zipSrc.close();
					outStream.close();
				}
				else if (ImageType.findType(file.getName()) != null) {
					logger.info("Processing image: "+ file.getAbsolutePath());
					
					FileInputStream inStream = null;
					FileOutputStream outStream = null;
					
					try {
						inStream = new FileInputStream(file);
						outStream = new FileOutputStream(targetFile);
						processImage(file.getName(), inStream, outStream);
					} finally {	
						IOUtils.closeQuietly(inStream);
						IOUtils.closeQuietly(outStream);
					}
				}
				else {
					logger.info("Processing : "+ file.getAbsolutePath());

					FileInputStream inStream = null;
					FileOutputStream outStream = null;
					
					try {
						inStream = new FileInputStream(file);
						outStream = new FileOutputStream(targetFile);
						IOUtils.copy(inStream, outStream);
					} finally {	
						IOUtils.closeQuietly(inStream);
						IOUtils.closeQuietly(outStream);
					}
					
				}

			}

		}

	}

	public static void processImage(String fileName, InputStream input, OutputStream output) throws IOException {
		
		logger.info("Scaling image: " + fileName);
		
		boolean imageWriteStarted = false;
		try {
			BufferedImage out = ImageIO.read(input);

			int outWidth = out.getWidth() * 2;
			int outHeight = out.getHeight() * 2;
			/*
			ResampleOp resampleOp = new ResampleOp(outWidth,
					outHeight);
			BufferedImage rescaledOut = resampleOp.filter(out, null);
			*/
			
		    AffineTransform scaleTransform = AffineTransform.getScaleInstance(2, 2);
		    AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);
		    BufferedImage rescaledOut= bilinearScaleOp.filter(
		            out,
		            new BufferedImage(outWidth, outHeight, out.getType()));
			
			imageWriteStarted = true;
			
			
			ImageIO.write(rescaledOut, ImageType.findType(fileName).name(), output);
			
		} catch (Exception e) {
			if (imageWriteStarted) {
				throw new RuntimeException("Failed to scale image [" + fileName + "]: " + e.getMessage(), e);
			}
			else
			{
				logger.error("Unable to scale ["+ fileName+ "]: " + e.getMessage(), e);
				IOUtils.copy(input, output);
			}
		}
	}

}
