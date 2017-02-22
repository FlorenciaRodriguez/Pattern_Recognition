import bsh.Console;
import com.sun.java.swing.plaf.windows.WindowsDesktopManager;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.measure.Measurements;

import ij.WindowManager;

import java.awt.*;

import ij.plugin.*;
import ij.plugin.frame.*;

import javax.swing.*;
import java.io.File;

public class PDI_Plugin implements PlugIn {

    private ImagePlus convertTo8Bits ( ImagePlus img ) {
        ImageProcessor ip = img.getProcessor ( );
        if ( ip instanceof ColorProcessor ) {
            MedianCut mc = new MedianCut ( ( int[] ) ip.getPixels ( ), ip.getWidth ( ), ip.getHeight ( ) );
            img.setProcessor ( null, mc.convertToByte ( 256 ) );
        } else {
            ip = ip.convertToByte ( true );
            img.setProcessor ( null, ip );
        }
        return img;
    }

    public void run ( String arg ) {

        //Imagen original
        ImagePlus originalImage = WindowManager.getCurrentImage ( );

        //Repository to save images
        String title = originalImage.getTitle ( );
        File repository = new File ( title.substring ( 0, title.length ( ) - 4 ) );
        if ( !repository.exists ( ) )
            try {
                repository.mkdir ( );
                System.out.println ( "Directory created\n" + repository.getAbsolutePath ( ) + "\n" );
            } catch (SecurityException se) {
                se.printStackTrace ( );
            }
        IJ.saveAsTiff ( originalImage, repository.getAbsolutePath ( ) + "\\1-original.tif" );

        //Conversi�n a Imagen de 8 bits
        ImagePlus image8Bits = convertTo8Bits ( originalImage );
        image8Bits.setTitle ( "8 bits image" );
        image8Bits.show ( );
        IJ.saveAsTiff ( image8Bits, repository.getAbsolutePath ( ) + "\\2-8bits.tif" );

        //Smoothing
       ImagePlus smoothImage = image8Bits.duplicate ( );
        smoothImage.getProcessor ( ).smooth ( );
        smoothImage.setTitle ( "Smooth image" );
        smoothImage.show ( );
        IJ.saveAsTiff ( smoothImage, repository.getAbsolutePath ( ) + "\\3-Smooth.tif" );

        //Background Subtracter
        ImagePlus noBackgroundImage = smoothImage.duplicate ( );
        BackgroundSubtracter bs = new BackgroundSubtracter ( );
        bs.rollingBallBackground ( noBackgroundImage.getProcessor ( ), ( double ) 50, false, true, false, false, false );
        noBackgroundImage.getProcessor ( ).resetMinAndMax ( );
        noBackgroundImage.setTitle ( "Image without background" );
        noBackgroundImage.show ( );
        IJ.saveAsTiff ( noBackgroundImage, repository.getAbsolutePath ( ) + "\\4-BackgroundSubtracter.tif" );

        //Find Edges
        ImagePlus edgesImage = noBackgroundImage.duplicate ( );
        edgesImage.getProcessor ( ).findEdges ( );
        edgesImage.setTitle ( "Edges" );
        edgesImage.show ( );
        IJ.saveAsTiff ( edgesImage, repository.getAbsolutePath ( ) + "\\5-Edges.tif" );

        //Binary Image
        ImagePlus binaryImage = edgesImage.duplicate ( );
        BinaryProcessor binarize = new BinaryProcessor ( new
                ByteProcessor ( binaryImage.getImage ( ) ) );
        binarize.autoThreshold ( );
        binaryImage.setProcessor ( binarize );
        binaryImage.setTitle ( "Binary" );
        binaryImage.show ( );
        IJ.saveAsTiff ( binaryImage, repository.getAbsolutePath ( ) + "\\6-Binary.tif" );

        //Watershed Image
        ImagePlus watershedImage = binaryImage.duplicate ( );
        watershedImage.setTitle ( "WatershedImage" );
        watershedImage.show ( );
        IJ.run ( "Watershed", "input=WatershedImage,mask=none" );
        watershedImage.updateAndDraw ( );
        IJ.saveAsTiff ( watershedImage, repository.getAbsolutePath ( ) + "\\7-WatershedImage.tif" );

        //Analyze Particle
        ResultsTable rt = new ResultsTable ( );
        ParticleAnalyzer pa = new ParticleAnalyzer ( ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES + ParticleAnalyzer.ELLIPSE + ParticleAnalyzer.SHOW_RESULTS + ParticleAnalyzer.SHOW_SUMMARY, Measurements.CIRCULARITY + Measurements.AREA + Measurements.CENTROID, rt, 256, 2048, 0.30, 1.00 );
        pa.analyze ( watershedImage.duplicate ( ) );
        rt.save ( repository.getAbsolutePath ( ) + "\\Resume.dat" );
    }

}


