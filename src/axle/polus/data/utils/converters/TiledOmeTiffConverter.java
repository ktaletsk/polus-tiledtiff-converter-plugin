package axle.polus.data.utils.converters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.codec.CompressionType;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;

/**
 * Based off of the example from https://docs.openmicroscopy.org/bio-formats/5.9.1/developers/tiling.html
 *
 * @author nick.schaub at nih.gov
 */
public class TiledOmeTiffConverter {

	private static final Logger LOG = Logger.getLogger(TiledOmeTiffConverter.class.getName());

	private ImageReader reader;
	private OMETiffWriter writer;
	private String inputFile;
	private String outputFile;
	private int tileSizeX;
	private int tileSizeY;

	public TiledOmeTiffConverter(String inputFile, String outputFile, int tileSizeX, int tileSizeY) {
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.tileSizeX = tileSizeX;
		this.tileSizeY = tileSizeY;
	}

	private void init() throws DependencyException, FormatException, IOException, ServiceException {
		DebugTools.setRootLevel("error");

		// construct the object that stores OME-XML metadata
		ServiceFactory factory = new ServiceFactory();
		OMEXMLService service = factory.getInstance(OMEXMLService.class);
		IMetadata omexml = service.createOMEXMLMetadata();

		// set up the reader and associate it with the input file
		reader = new ImageReader();
		reader.setOriginalMetadataPopulated(true);
		reader.setMetadataStore(omexml);
		reader.setId(inputFile);

		// important to delete because OME uses RandomAccessFile
		Path outputPath = Paths.get(outputFile);
		outputPath.toFile().delete();

		// set up the writer and associate it with the output file
		writer = new OMETiffWriter();
		writer.setMetadataRetrieve(omexml);
		writer.setInterleaved(reader.isInterleaved());
		
	    // set the tile size height and width for writing
	    this.tileSizeX = writer.setTileSizeX(tileSizeX);
	    this.tileSizeY = writer.setTileSizeY(tileSizeY);

		writer.setId(outputFile);

		writer.setCompression(CompressionType.LZW.getCompression());
	}

	public void readWriteTiles() throws FormatException, DependencyException, ServiceException, IOException {
		int bpp = FormatTools.getBytesPerPixel(reader.getPixelType());
		int tilePlaneSize = tileSizeX * tileSizeY * reader.getRGBChannelCount() * bpp;
		byte[] buf = new byte[tilePlaneSize];

		// convert each image in the current series
		for (int image=0; image<reader.getImageCount(); image++) {
			int width = reader.getSizeX();
			int height = reader.getSizeY();

			// Determined the number of tiles to read and write
			int nXTiles = width / tileSizeX;
			int nYTiles = height / tileSizeY;
			if (nXTiles * tileSizeX != width) nXTiles++;
			if (nYTiles * tileSizeY != height) nYTiles++;

			for (int y=0; y<nYTiles; y++) {
				for (int x=0; x<nXTiles; x++) {
					
					int tileX = x * tileSizeX;
					int tileY = y * tileSizeY;
					
					int effTileSizeX = (tileX + tileSizeX) < width ? tileSizeX : width - tileX;
					int effTileSizeY = (tileY + tileSizeY) < height ? tileSizeY : height - tileY;

					buf = reader.openBytes(image, tileX, tileY, effTileSizeX, effTileSizeY);
					writer.saveBytes(image, buf, tileX, tileY, effTileSizeX, effTileSizeY);
				}
			}
		}
	}

	private void cleanup() {
		try {
			reader.close();
		}
		catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to close reader.",e);
		}
		try {
			writer.close();
		}
		catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to close writer.",e);
		}
	}

	/* Command line function */
	public static void main(String[] args) throws FormatException, IOException, DependencyException, ServiceException {
		int tileSizeX = Integer.parseInt(args[2]);
		int tileSizeY = Integer.parseInt(args[3]);
		TiledOmeTiffConverter tiledReadWriter = new TiledOmeTiffConverter(args[0], args[1], tileSizeX, tileSizeY);
		// initialize the files
		tiledReadWriter.init();

		try {
			// read and write the image using tiles
			tiledReadWriter.readWriteTiles();
		}
		catch(Exception e) {
			System.err.println("Failed to read and write tiles.");
			e.printStackTrace();
			throw e;
		}
		finally {
			// close the files
			tiledReadWriter.cleanup();
		}
	}

}
